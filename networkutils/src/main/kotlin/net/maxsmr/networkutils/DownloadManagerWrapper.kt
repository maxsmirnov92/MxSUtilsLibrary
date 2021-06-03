package net.maxsmr.networkutils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(DownloadManagerWrapper::class.java)

/**
 * Класс для отслеживания начатых и завершённых, старта новых загрузок через [DownloadManager]
 */
class DownloadManagerWrapper(private val context: Context) {

    /**
     * Мапа текущих файлов для скачивания. Key - id загрузки, Value - урла файла на сервере.
     */
    val currentDownloads = mutableMapOf<Long, Uri>()
        @Synchronized
        get() = field

    /**
     * Key - id начатой загрузки; Value - исходный запрос, по которому был ответ с этим url загрузки
     */
    val currentDownloadsRequestMap: MutableMap<Long, Any> = mutableMapOf()
        @Synchronized
        get() = field

    /**
     * Мапа загруженных файлов. Key - id загрузки, Value - [DownloadedInfo]
     */
    val downloadedMap = mutableMapOf<Long, DownloadedInfo>()
        @Synchronized
        get() = field

    val downloadSuccessIds: List<Long>
        @Synchronized
        get() = downloadedMap.filter {
            it.value.isSuccess
        }.map {
            it.key
        }

    val downloadFailedIds: List<Long>
        @Synchronized
        get() = downloadedMap.filter {
            !it.value.isSuccess
        }.map {
            it.key
        }

    val isAnyDownloadRunning: Boolean
        get() = currentDownloads.isNotEmpty()

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Зарегистрированные [DownloadListener] с начатами downloadId
     */
    private val registeredListeners = mutableSetOf<DownloadListener<*>>()

    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
                ?: throw RuntimeException("DownloadManager is null")
    }

    /**
     * Глобальный листенер для еррора на старте загрузки с указанным [Uri]
     */
    var startDownloadErrorListener: ((Uri, Throwable) -> Unit)? = null

    private var currentReceiver: DownloadBroadcastReceiver? = null

    @Synchronized
    @JvmOverloads
    fun isAllDownloadsSuccess(checkAllCompleted: Boolean = true): Boolean = downloadSuccessIds.size == downloadedMap.size && (!checkAllCompleted || !isAnyDownloadRunning)

    @Synchronized
    @JvmOverloads
    fun isAllDownloadsFailed(checkAllCompleted: Boolean = true): Boolean = downloadFailedIds.size == downloadedMap.size && (!checkAllCompleted || !isAnyDownloadRunning)

    @Synchronized
    @JvmOverloads
    fun isAnyDownloadSuccess(checkAllCompleted: Boolean = true): Boolean = downloadSuccessIds.isNotEmpty() && (!checkAllCompleted || !isAnyDownloadRunning)

    @Synchronized
    @JvmOverloads
    fun isAnyDownloadFailed(checkAllCompleted: Boolean = true): Boolean = downloadFailedIds.isNotEmpty() && (!checkAllCompleted || !isAnyDownloadRunning)

    /**
     * @param requestConfigurator функция для настройки [DownloadManager.Request]
     * @param listenerCreator возвращает свой [DownloadListener] или null, если дополнительная логика по завершении загрузки не требуется
     * @return id начатой загрузки, null - в случае неуспеха
     */
    @Synchronized
    @JvmOverloads
    fun <Request> startDownload(
            uri: Uri,
            mimeType: String,
            requestConfigurator: ((DownloadManager.Request) -> Unit),
            listenerCreator: ((Long) -> DownloadListener<Request>)? = null,
            request: Request? = null,
            requestClass: Class<Request>? = null,
            errorListener: ((Throwable) -> Unit)? = null
    ): Long? {
        logger.i("startDownload, uri=$uri, mimeType=$mimeType, request=$request, requestClass=$requestClass")
        try {
            val downloadRequest = DownloadManager.Request(uri).setMimeType(mimeType)
            requestConfigurator.invoke(downloadRequest)
            val downloadId = downloadManager.enqueue(downloadRequest)

            var listener = listenerCreator?.invoke(downloadId)
            if (listener == null) {
                listener = DownloadListener()
            }
            listener.downloadManagerWrapper = this
            listener.downloadId = downloadId
            listener.completeHandler = mainHandler
            listener.requestClass = requestClass
            addListener(listener)

            val receiver = currentReceiver
                    ?: throw IllegalStateException("Common DownloadBroadcastReceiver still not registered")
            receiver.registeredListeners = registeredListeners

            currentDownloads[downloadId] = uri
            request?.let {
                currentDownloadsRequestMap[downloadId] = request as Any
            }
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            val movedToFirst = cursor.moveToFirst()
            var isSuccess: Boolean? = null
            if (!movedToFirst || isDownloadFailed(cursor)) {
                isSuccess = false
            } else if (movedToFirst && isDownloadSuccess(cursor)) {
                isSuccess = true
            }
            logger.d("Download with id $downloadId queried, current status: " + getDownloadStatus(cursor))
            if (isSuccess != null) {
                // успех/неуспех может произойти сразу, не вернувшись в BroadcastReceiver
                try {
                    listener.onDownloadComplete(uri, isSuccess, getDownloadReason(cursor))
                } finally {
                    removeListenerWithUnregister(downloadId)
                }
            }
            return downloadId
        } catch (e: Throwable) {
            errorListener?.invoke(e)
            startDownloadErrorListener?.invoke(uri, e)
        }
        return null
    }

    @Synchronized
    fun dispose() {
        logger.d("dispose")
        currentDownloads.keys.let {
            if (it.isNotEmpty()) {
                downloadManager.remove(*it.toLongArray())
            }
        }
        currentDownloads.clear()
        currentDownloadsRequestMap.clear()
        downloadedMap.clear()
        removeAllListenersWithUnregister()
    }

    private fun addListener(listener: DownloadListener<*>) {
        removeListener(listener.downloadId)
        logger.d("Adding listener with download id ${listener.downloadId}...")
        registeredListeners.add(listener)
        currentReceiver.let {
            if (it == null) {
                // регистрируем основной, только если не был зарегистрирован ранее
                registerReceiver()
            }
        }
    }

    private fun removeAllListeners() {
        registeredListeners.map { it.downloadId }.forEach {
            removeListener(it)
        }
    }

    private fun removeListener(downloadId: Long) {
        val iterator = registeredListeners.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().downloadId == downloadId) {
                logger.d("Removing listener with download id $downloadId...")
                iterator.remove()
            }
        }
    }

    private fun removeAllListenersWithUnregister() {
        removeAllListeners()
        unregisterSafe()
    }

    /**
     * Убирает [DownloadListener] и проверяет, можно ли отрегистрировать основной [DownloadBroadcastReceiver]
     */
    private fun removeListenerWithUnregister(downloadId: Long) {
        removeListener(downloadId)
        unregisterSafe()
    }

    private fun registerReceiver() {
        unregisterReceiver()
        with(DownloadBroadcastReceiver()) {
            downloadManagerWrapper = this@DownloadManagerWrapper
            registeredListeners = this@DownloadManagerWrapper.registeredListeners
            logger.d("Registering common receiver...")
            context.registerReceiver(this, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            // для DownloadManager'а теперь активен ТОЛЬКО этот receiver
            currentReceiver = this
        }
    }

    private fun unregisterReceiver() {
        currentReceiver?.let {
            logger.d("Unregistering common receiver...")
            context.unregisterReceiver(it)
            currentReceiver = null
        }
    }

    private fun unregisterSafe() {
        if (!isAnyDownloadRunning) {
            unregisterReceiver()
        }
    }

    /**
     * @param Request опциональный объект, содержащий оригинальный URL, для отслеживания
     */
    open class DownloadListener<Request> {

        internal lateinit var downloadManagerWrapper: DownloadManagerWrapper

        /**
         * id загрузки из enqueue
         */
        internal var downloadId: Long = -1

        internal var completeHandler: Handler? = null

        internal var requestClass: Class<Request>? = null

        /**
         * Колбек с результатом загрузки, может быть вызван отдельно
         */
        @Suppress("UNCHECKED_CAST")
        internal fun onDownloadComplete(downloadUri: Uri, isSuccess: Boolean, reason: Int) {
            logger.i("onDownloadComplete, downloadUri=$downloadUri, isSuccess=$isSuccess, reason=$reason")
            downloadId.let {
                require(it >= 0) { "Receiver's downloadId cannot be less than zero: $it" }
            }
            synchronized(downloadManagerWrapper) {
                val fileUri: Uri? = downloadManagerWrapper.downloadManager.getUriForDownloadedFile(downloadId)
                val info = DownloadedInfo(isSuccess, downloadUri, fileUri)
                downloadManagerWrapper.downloadedMap[downloadId] = info
                val request = downloadManagerWrapper.currentDownloadsRequestMap[downloadId]
                downloadManagerWrapper.currentDownloads.remove(downloadId)
                downloadManagerWrapper.currentDownloadsRequestMap.remove(downloadId)
                with(completeHandler) {
                    requestClass.let {
                        val isAssignable = request != null && it != null && it.isAssignableFrom(request.javaClass)
                        if (request == null || it == null || isAssignable) {
                            this?.post { onDownloadRequestComplete(downloadId, if (isAssignable) request as Request else null, info, reason) }
                                    ?: onDownloadRequestComplete(downloadId, if (isAssignable) request as Request else null, info, reason)
                        }
                    }
                }
            }
        }

        @CallSuper
        protected open fun onDownloadRequestComplete(downloadId: Long, request: Request?, downloadInfo: DownloadedInfo, reason: Int) {
            logger.d("onDownloadRequestComplete, downloadId=$downloadId request=$request, downloadInfo=$downloadInfo, reason=$reason")
        }
    }

    /**
     * Основной [BroadcastReceiver] для [DownloadManagerWrapper], если выполняется хотя бы одна загрузка
     */
    private class DownloadBroadcastReceiver : BroadcastReceiver() {

        internal lateinit var downloadManagerWrapper: DownloadManagerWrapper

        internal lateinit var registeredListeners: Set<DownloadListener<*>>

        @MainThread
        override fun onReceive(context: Context, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
            logger.d("onReceive, intent=$intent, id=$id")

            synchronized(downloadManagerWrapper) {
                try {
                    if (id < 0) {
                        logger.e("Incorrect received downloadId: $id")
                        return
                    }
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE != intent?.action) {
                        logger.e("Action {$intent?.action} is not " + DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                        return
                    }
                    val downloadUri = downloadManagerWrapper.currentDownloads[id]
                    if (downloadUri == null) {
                        logger.e("Download id $id was not found in currentDownloads")
                        return
                    }
                    val cursor = downloadManagerWrapper.downloadManager.query(DownloadManager.Query().setFilterById(id))
                    if (!cursor.moveToFirst()) {
                        logger.e("Cannot move cursor to first")
                        return
                    }
                    val isSuccess = isDownloadSuccess(cursor)
                    val reason = getDownloadReason(cursor)

                    registeredListeners.forEach {
                        if (it.downloadId == id) {
                            it.onDownloadComplete(downloadUri, isSuccess, reason)
                        }
                    }
                } finally {
                    downloadManagerWrapper.removeListenerWithUnregister(id)
                }
            }
        }
    }

    /**
     * Информация о загруженном файле
     */
    data class DownloadedInfo(
            val isSuccess: Boolean,
            val downloadUri: Uri,
            val fileUri: Uri?
    )

    companion object {

        fun getDownloadStatus(cursor: Cursor) = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

        fun isDownloadSuccess(cursor: Cursor): Boolean = DownloadManager.STATUS_SUCCESSFUL ==
                getDownloadStatus(cursor)

        fun isDownloadFailed(cursor: Cursor): Boolean = DownloadManager.STATUS_FAILED ==
                getDownloadStatus(cursor)

        fun getDownloadReason(cursor: Cursor): Int = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))

        fun getDownloadedUri(cursor: Cursor): Uri? =
                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))?.let {
                    Uri.parse(it)
                }
    }
}