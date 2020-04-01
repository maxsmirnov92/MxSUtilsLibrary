package net.maxsmr.networkutils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.util.Log

private const val LOG_TAG = "DownloadHelper"

/**
 * Для отслеживания начатых и завершённых, старта новых загрузок через [DownloadManager]
 * @param Request опциональный объект, содержащий оригинальный URL, для отслеживания
 */
class DownloadHelper<Request>(val context: Context) {

    /**
     * Мапа текущих файлов для скачивания. Key - id загрузки, Value - урла файла на сервере.
     */
    val currentDownloads = mutableMapOf<Long, Uri>()

    /**
     * Key - id начатой загрузки; Value - исходный запрос, по которому был ответ с этим url загрузки
     */
    val currentDownloadsRequestMap = mutableMapOf<Long, Request>()

    /**
     * Мапа загруженных файлов. Key - id загрузки, Value - [DownloadedInfo]
     */
    val downloadedMap = mutableMapOf<Long, DownloadedInfo>()

    val downloadSuccessIds: List<Long>
        get() = downloadedMap.filter {
            it.value.isSuccess
        }.map {
            it.key
        }

    val downloadFailedIds: List<Long>
        get() = downloadedMap.filter {
            !it.value.isSuccess
        }.map {
            it.key
        }

    /**
     * Все загрузки завершены успешно, ни одна не выполняется
     */
    val isAllDownloadsSuccess: Boolean
        get() = currentDownloads.isEmpty() && downloadSuccessIds.size == downloadedMap.size

    /**
     * Все загрузки зафейлены, ни одна не выполняется
     */
    val isAllDownloadsFailed: Boolean
        get() = currentDownloads.isEmpty() && downloadFailedIds.size == downloadedMap.size

    val isAnyDownloadFailed: Boolean
        get() = downloadFailedIds.isNotEmpty()

    /**
     * Зарегистрированные [DownloadBroadcastReceiver] по начатым download id
     */
    private val registeredReceiversSet = mutableSetOf<DownloadBroadcastReceiver<Request>>()

    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    /**
     * Глобальный листенер для еррора на старте загрузки с указанным [Uri]
     */
    var startDownloadErrorListener: ((Uri, Throwable) -> Unit)? = null

    /**
     * @param broadcastRegisterer возвращает свой [DownloadBroadcastReceiver] или null, если дополнительная логика в onReceive не требуется
     * @return id начатой загрузки, null - в случае неуспеха
     */
    fun startDownload(
            uri: Uri,
            mimeType: String,
            requestConfigurator: ((DownloadManager.Request) -> Unit),
            broadcastRegisterer: ((Long) -> DownloadBroadcastReceiver<Request>)? = null,
            request: Request? = null,
            errorListener: ((Throwable) -> Unit)? = null
    ): Long? {
        try {
            val downloadRequest = DownloadManager.Request(uri).setMimeType(mimeType)
            requestConfigurator.invoke(downloadRequest)
            val downloadId = downloadManager.enqueue(downloadRequest)
            var receiver = broadcastRegisterer?.invoke(downloadId)
            if (receiver == null) {
                receiver = DownloadBroadcastReceiver(this, downloadId)
            } else {
                check(receiver.downloadId == downloadId) {
                    "Download ids: original ($downloadId) and DownloadBroadcastReceiver (${receiver.downloadId}) not match!"
                }
            }
            registerReceiver(receiver)
            currentDownloads[downloadId] = uri
            request?.let {
                currentDownloadsRequestMap[downloadId] = request
            }
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            val movedToFirst = cursor.moveToFirst()
            var isSuccess: Boolean? = null
            if (!movedToFirst || isDownloadFailed(cursor)) {
                isSuccess = false
            } else if (movedToFirst && isDownloadSuccess(cursor)) {
                isSuccess = true
            }
            if (isSuccess != null) {
                // успех/неуспех может произойти сразу, не вернувшись в BroadcastReceiver
                try {
                    receiver.onDownloadComplete(uri, isSuccess, getDownloadReason(cursor))
                } finally {
                    unregisterReceiver(downloadId)
                }
            }
            return downloadId
        } catch (e: Throwable) {
            errorListener?.invoke(e)
            startDownloadErrorListener?.invoke(uri, e)
        }
        return null
    }

    fun dispose() {
        currentDownloads.keys.let {
            if (it.isNotEmpty()) {
                downloadManager.remove(*it.toLongArray())
            }
        }
        currentDownloads.clear()
        currentDownloadsRequestMap.clear()
        downloadedMap.clear()
        unregisterAllReceivers()
    }

    private fun registerReceiver(receiver: DownloadBroadcastReceiver<Request>) {
        unregisterReceiver(receiver.downloadId)
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        registeredReceiversSet.add(receiver)
    }

    private fun unregisterReceiver(downloadId: Long) {
        registeredReceiversSet.find { it.downloadId == downloadId }?.let {
            context.unregisterReceiver(it)
            val iterator = registeredReceiversSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().downloadId == downloadId) {
                    iterator.remove()
                }
            }
        }
    }

    private fun unregisterAllReceivers() {
        registeredReceiversSet.map { it.downloadId }.forEach {
            unregisterReceiver(it)
        }
    }

    /**
     * @param downloadId id загрузки из enqueue, для который был зарегистрирован данный [DownloadBroadcastReceiver]
     */
    open class DownloadBroadcastReceiver<Request>(
            val downloadHelper: DownloadHelper<Request>,
            val downloadId: Long
    ) : BroadcastReceiver() {

        init {
            require(downloadId >= 0) { "downloadId cannot be less than zero: $downloadId" }
        }

        final override fun onReceive(context: Context, intent: Intent) {
            try {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id < 0) {
                    Log.e(LOG_TAG, "Incorrect received downloadId: $id")
                }
                if (downloadId != id) {
                    Log.e(LOG_TAG, "Registered download id $downloadId and received $id not match!")
                    return
                }
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE != intent.action) return
                val downloadUri = downloadHelper.currentDownloads[id] ?: return
                val cursor = downloadHelper.downloadManager.query(DownloadManager.Query().setFilterById(id))
                if (!cursor.moveToFirst()) return
                val isSuccess = isDownloadSuccess(cursor)
                val reason = getDownloadReason(cursor)
                onDownloadComplete(downloadUri, isSuccess, reason)
            } finally {
                downloadHelper.unregisterReceiver(downloadId)
            }
        }

        fun onDownloadComplete(downloadUri: Uri, isSuccess: Boolean, reason: Int) {
            val fileUri: Uri? = downloadHelper.downloadManager.getUriForDownloadedFile(downloadId)
            downloadHelper.downloadedMap[downloadId] = DownloadedInfo(isSuccess, downloadUri, fileUri)
            val request = downloadHelper.currentDownloadsRequestMap[downloadId]
            downloadHelper.currentDownloads.remove(downloadId)
            downloadHelper.currentDownloadsRequestMap.remove(downloadId)
            onDownloadRequestComplete(request, downloadUri, fileUri, isSuccess, reason)
        }

        protected open fun onDownloadRequestComplete(request: Request?, downloadUri: Uri, fileUri: Uri?, isSuccess: Boolean, reason: Int) {
            // override if needed
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