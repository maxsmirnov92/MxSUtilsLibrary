package net.maxsmr.commonutils.media

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.*
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.graphic.createBitmapFromFile
import net.maxsmr.commonutils.graphic.createBitmapFromUri
import net.maxsmr.commonutils.graphic.rotateBitmap
import java.io.File
import kotlin.Pair

private const val MIME_TYPE_IMAGE = "image/*"

class ContentPicker(
        private val owner: LifecycleOwner,
        private val pickerConfigurator: BasePickerConfigurator
) {

    /**
     * @param newCameraPictureFileFunc создание пустого файла для взятия с камеры для версий < N
     */
    constructor(
            context: Context,
            owner: LifecycleOwner,
            fileProviderAuthorityPostfix: String,
            pickParams: PickParams,
            newCameraPictureFileFunc: (() -> File)? = null
    ) : this(owner,
            if (isAtLeastNougat()) {
                MediaStorePickerConfigurator(
                        context,
                        fileProviderAuthorityPostfix,
                        pickParams,
                        newCameraPictureFileFunc
                )
            } else {
                FilePickerConfigurator(
                        context,
                        fileProviderAuthorityPostfix,
                        pickParams,
                        newCameraPictureFileFunc
                )
            })

    val activity: Activity
        get() {
            with(owner) {
                return when (this) {
                    is Activity -> this
                    is Fragment -> requireActivity()
                    else -> throw IllegalStateException("Owner is not fragment or activity")
                }
            }
        }

    private var cameraContentUri: Uri? = null

    fun pickFromGallery(): Boolean {
        with(pickerConfigurator) {
            val pickFromGalleryRequestCode = pickParams.pickFromGalleryRequestCode ?: return false
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MIME_TYPE_IMAGE)
            if (!onBeforePickPictureFromGallery(intent)) {
                return false
            }
            val activity = activity
            val fragment = if (owner is Fragment) owner else null
            return startActivityForResultSafe(activity,
                    fragment,
                    wrapIntent(intent, pickParams.pickFromGalleryChooserTitle),
                    pickFromGalleryRequestCode)
        }
    }

    fun pickFromCamera(isFromMediaStore: Boolean, mimeType: String = MIME_TYPE_IMAGE): Boolean {
        with(pickerConfigurator) {
            val pickFromCameraRequestCode = pickParams.pickFromCameraRequestCode ?: return false
            val intentInfo = if (isFromMediaStore) {
                createCameraPictureIntentFromMediaStore(mimeType)
            } else {
                createCameraPictureIntentFromFile()
            }
            if (!onBeforePickPictureFromCamera(intentInfo.first)) {
                return false
            }
            cameraContentUri = intentInfo.second
            val activity = activity
            val fragment = if (owner is Fragment) owner else null
            return startActivityForResultSafe(activity,
                    fragment,
                    wrapIntent(intentInfo.first, pickParams.pickFromCameraChooserTitle),
                    pickFromCameraRequestCode)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun pickContent(
            type: String?,
            mimeTypes: List<String>?,
            openOrGet: Boolean = true
    ): Boolean {
        with(pickerConfigurator) {
            val pickFileRequestCode = pickParams.pickFileRequestCode ?: return false
            val intent = if (openOrGet) {
                getOpenDocumentIntent(type, mimeTypes)
            } else {
                getContentIntent(type, mimeTypes)
            }
            if (!onBeforePickContent(intent)) {
                return false
            }
            val activity = activity
            val fragment = if (owner is Fragment) owner else null
            return startActivityForResultSafe(activity,
                    fragment,
                    wrapIntent(intent, pickParams.pickFileChooserTitle),
                    pickFileRequestCode)
        }
    }

    fun onResult(resultCode: Int, requestCode: Int, data: Intent?): Pair<ContentSource, Uri>? {
        var result: Pair<ContentSource, Uri>? = null
        with(pickerConfigurator) {
            if (Activity.RESULT_OK == resultCode) {
                if (requestCode == pickParams.pickFromCameraRequestCode) {
                    retrieveUriAfterPickFromCamera(cameraContentUri)?.let {
                        result = Pair(ContentSource.CAMERA, it)
                    }
                    cameraContentUri = null
                } else if (requestCode in arrayOf(pickParams.pickFromGalleryRequestCode, pickParams.pickFileRequestCode)) {
                    retrieveUriAfterPickContent(data)?.let {
                        result = Pair(if (requestCode == pickParams.pickFromGalleryRequestCode) ContentSource.GALLERY else ContentSource.OTHER, it)
                    }
                }
            }
        }
        return result
    }

    abstract class BasePickerConfigurator(
            val context: Context,
            val fileProviderAuthorityPostfix: String,
            val pickParams: PickParams,
            private val newCameraPictureFileFunc: (() -> File)? = null
    ) {

        protected val contentResolver = context.contentResolver

        fun createCameraPictureIntentFromMediaStore(mimeType: String): Pair<Intent, Uri?> {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val values = ContentValues(1)
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val outputUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            return Pair(captureIntent, outputUri)
        }

        fun createCameraPictureIntentFromFile(): Pair<Intent, Uri> {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val localFileUri: Uri
            val cameraFile = newCameraPictureFileFunc?.invoke()
                    ?: throw IllegalStateException("Camera picture file must be created for PickerConfigurator")
            val outputFileUri = with(cameraFile) {
                localFileUri = this.toFileUri()
                if (!TextUtils.isEmpty(fileProviderAuthorityPostfix)) {
                    // в манифесте аппа объявлен FileProvider
                    FileProvider.getUriForFile(context, "${context.packageName}.$fileProviderAuthorityPostfix", this)
                } else {
                    // попытка выключить "strict mode" для пользования обычным файлом
                    disableFileUriStrictMode()
                    localFileUri
                }
            }
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            // для своих целей сохраняем файловую урлу,
            // чтобы в дальнейшем работать с File
            return Pair(captureIntent, localFileUri)
        }

        abstract fun retrieveUriAfterPickContent(data: Intent?): Uri?
        abstract fun retrieveUriAfterPickFromCamera(outputFileUri: Uri?): Uri?

        fun onBeforePickPictureFromGallery(intent: Intent): Boolean = true
        fun onBeforePickPictureFromCamera(intent: Intent): Boolean = true
        fun onBeforePickContent(intent: Intent): Boolean = true
    }

    /**
     * [IPickerConfigurator] для получения контента
     * 1. Из стороннего аппа с разными scheme:// - извлекается файловый path
     * 2. Камера - в предоставленный внешний файл по ссылке;
     * Применяется в версиях < N
     */
    open class FilePickerConfigurator(
            context: Context,
            fileProviderAuthorityPostfix: String,
            pickParams: PickParams,
            newCameraPictureFileFunc: (() -> File)? = null
    ) : BasePickerConfigurator(
            context,
            fileProviderAuthorityPostfix,
            pickParams,
            newCameraPictureFileFunc
    ) {

        override fun retrieveUriAfterPickContent(data: Intent?): Uri? {
            // getPath в данном случае это путь к файлу
            val path = if (data != null && data.data != null) data.data.getPath(context) else EMPTY_STRING
            return if (path.isNotEmpty()) {
                File(path).toFileUri()
            } else {
                null
            }
        }

        override fun retrieveUriAfterPickFromCamera(outputFileUri: Uri?): Uri? = outputFileUri

        private fun createBitmap(fileUri: Uri?): Bitmap? {
            return if (fileUri != null) {
                createBitmapFromFile(File(fileUri.path ?: EMPTY_STRING))
            } else {
                null
            }
        }
    }

    /**
     * [IPickerConfigurator] для получения контента
     * 1. Из стороннего аппа с разными scheme:// - возвращается в неизменном виде
     * 2. Камера - в предоставленный внешний [EXTERNAL_CONTENT_URI] через провайдер;
     * Применяется в версиях >= N
     */
    open class MediaStorePickerConfigurator(
            context: Context,
            fileProviderAuthorityPostfix: String,
            pickParams: PickParams,
            newCameraPictureFileFunc: (() -> File)? = null
    ) : BasePickerConfigurator(context,
            fileProviderAuthorityPostfix,
            pickParams,
            newCameraPictureFileFunc
    ) {

        override fun retrieveUriAfterPickContent(data: Intent?): Uri? {
            // в кач-ве урлы полученный результат data (может быть, например, "content://")
            return data?.data
        }

        override fun retrieveUriAfterPickFromCamera(outputFileUri: Uri?): Uri? {
            outputFileUri?.let {
                contentResolver.notifyChange(it, null)
                return it
            }
            return null
        }


    }

    enum class ContentSource {
        GALLERY, CAMERA, OTHER
    }

    data class PickParams(
            val pickFromGalleryRequestCode: Int?,
            val pickFromCameraRequestCode: Int?,
            val pickFileRequestCode: Int?,
            val pickFromGalleryChooserTitle: String = EMPTY_STRING,
            val pickFromCameraChooserTitle: String = EMPTY_STRING,
            val pickFileChooserTitle: String = EMPTY_STRING
    )

    companion object {

        @JvmOverloads
        fun retrieveBitmapFromAnyOrFileUri(
                contentResolver: ContentResolver,
                uri: Uri?,
                rotate: Boolean,
                config: Config = Config.ARGB_8888,
                withSampleSize: Boolean = false
        ): Pair<Bitmap, Int>? = if (uri.isFileScheme()) {
            retrieveBitmapFromFileUri(uri, rotate, config, withSampleSize)
        } else {
            retrieveBitmapFromAnyUri(contentResolver, uri, rotate, config, withSampleSize)
        }

        /**
         * Декодирование стрима из [ContentResolver]
         * @param uri готовая контентная урла при
         * @param rotate требуется ли поворот в соот-ии с ориентацией из MediaStore
         * @return декодированный из [uri] битмап + угол из MediaStore
         */
        @JvmOverloads
        fun retrieveBitmapFromAnyUri(
                contentResolver: ContentResolver,
                uri: Uri?,
                rotate: Boolean,
                config: Config = Config.ARGB_8888,
                withSampleSize: Boolean = false
        ): Pair<Bitmap, Int>? {
            // api >= Q -> можем юзать получение угла из MediaStore (не ExifInterface)
            // и создание bitmap из ContentResolver
            if (uri == null) {
                return null
            }
            // на >= Q с BitmapFactory.Options не работает

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                ImageDecoder.createSource(contentResolver, it).decodeBitmap { info, decoder -> }
//            } else {
////                    MediaStore.Images.Media.getBitmap(contentResolver, it)
//                GraphicUtils.createBitmapFromUri(contentResolver, it)
//            }
            val bitmap = createBitmapFromUri(uri, contentResolver, config = config, withSampleSize = withSampleSize)
            val angle = getRotationAngleFromExif(contentResolver, uri)

            if (bitmap != null) {
                if (rotate && angle != null && angle > 0) {
                    val resultBitmap = rotateBitmap(bitmap, angle)
                    if (resultBitmap != null) {
                        return Pair(resultBitmap, angle)
                    }
                }
                return Pair(bitmap, angle ?: 0)
            }
            return null
        }

        /**
         * Декодирование файла из урлы с file://
         * @param uri готовая файловая урла
         * @param rotate требуется ли поворот в соот-ии с ориентацией из Exif
         * @return декодированный из [uri] битмап + угол из Exif
         */
        @JvmOverloads
        fun retrieveBitmapFromFileUri(
                uri: Uri?,
                rotate: Boolean,
                config: Config = Config.ARGB_8888,
                withSampleSize: Boolean = false
        ): Pair<Bitmap, Int>? {
            // несколько кейсов: pick контента, фотки, заранее известного cameraFile:
            // урла здесь всегда должна быть файловая!
            if (uri == null || !uri.isFileScheme()) {
                return null
            }
            val path = uri.path ?: EMPTY_STRING
            if (path.isEmpty()) {
                return null
            }

            val file = File(path)

            val bitmap = createBitmapFromFile(file, config = config, withSampleSize = withSampleSize)
            val angle = getRotationAngleFromExif(file)

            if (bitmap != null) {
                if (rotate && angle > 0) {
                    val resultBitmap = rotateBitmap(bitmap, angle)
                    if (resultBitmap != null) {
                        return Pair(resultBitmap, angle)
                    }
                }
                return Pair(bitmap, angle)
            }
            return null
        }
    }
}
