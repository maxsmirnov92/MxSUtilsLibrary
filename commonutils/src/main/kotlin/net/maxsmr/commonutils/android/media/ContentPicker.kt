package net.maxsmr.commonutils.android.media

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
import net.maxsmr.commonutils.android.*
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.graphic.createBitmapFromFile
import net.maxsmr.commonutils.graphic.createBitmapFromUri
import net.maxsmr.commonutils.graphic.rotateBitmap
import java.io.File

private const val MIME_TYPE_IMAGE = "image/*"

class ContentPicker(
        private val owner: LifecycleOwner,
        private val pickerConfigurator: IPickerConfigurator
) {

    /**
     * @param newCameraPictureFileFunc создание пустого файла для взятия с камеры для версий < N
     */
    constructor(
            context: Context,
            owner: LifecycleOwner,
            fileProviderAuthorityPostfix: String,
            pickFromGalleryRequestCode: Int? = null,
            pickFromCameraRequestCode: Int? = null,
            pickFileRequestCode: Int? = null,
            newCameraPictureFileFunc: (() -> File)? = null
    ) : this(owner,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                MediaStorePickerConfigurator(context, pickFromGalleryRequestCode, pickFromCameraRequestCode, pickFileRequestCode)
            } else {
                object : ContentPicker.BaseFilePickerConfigurator(context, fileProviderAuthorityPostfix, pickFromGalleryRequestCode, pickFromCameraRequestCode, pickFileRequestCode) {
                    override fun newCameraPictureFile(): File = newCameraPictureFileFunc?.invoke()
                            ?: throw IllegalStateException("Camera picture file must be created for FilePickerConfigurator")
                }
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
            val pickFromGalleryRequestCode = pickFromGalleryRequestCode ?: return false
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MIME_TYPE_IMAGE)
            if (!onBeforePickPictureFromGallery(intent)) {
                return false
            }
            val activity = activity
            val fragment = if (owner is Fragment) owner else null
            return startActivityForResultSafe(activity,
                    fragment,
                    intent,
                    pickFromGalleryRequestCode)
        }
    }

    fun pickFromCamera(mimeType: String = MIME_TYPE_IMAGE): Boolean {
        with(pickerConfigurator) {
            val pickFromCameraRequestCode = pickFromCameraRequestCode ?: return false
            val intentInfo = createCameraPictureIntent(mimeType)
            if (!onBeforePickPictureFromCamera(intentInfo.first)) {
                return false
            }
            cameraContentUri = intentInfo.second
            val activity = activity
            val fragment = if (owner is Fragment) owner else null
            return startActivityForResultSafe(activity,
                    fragment,
                    intentInfo.first,
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
            val pickFileRequestCode = pickFileRequestCode ?: return false
            val intent = if (openOrGet) {
                getOpenDocumentIntent(type, mimeTypes)
            } else {
                getContentIntent(type, mimeTypes)
            }
            val activity = activity
            val fragment = if (owner is Fragment) owner else null
            return startActivityForResultSafe(activity,
                    fragment,
                    intent,
                    pickFileRequestCode)
        }
    }

    fun onResult(resultCode: Int, requestCode: Int, data: Intent?): Pair<ContentSource, Uri>? {
        var result: Pair<ContentSource, Uri>? = null
        with(pickerConfigurator) {
            if (Activity.RESULT_OK == resultCode) {
                if (requestCode == pickFromCameraRequestCode) {
                    retrieveUriAfterPickFromCamera(cameraContentUri)?.let {
                        result = Pair(ContentSource.CAMERA, it)
                    }
                    cameraContentUri = null
                } else if (requestCode in arrayOf(pickFromGalleryRequestCode, pickFileRequestCode)) {
                    retrieveUriAfterPickContent(data)?.let {
                        result = Pair(if (requestCode == pickFromGalleryRequestCode) ContentSource.GALLERY else ContentSource.OTHER, it)
                    }
                }
            }
        }
        return result
    }

    interface IPickerConfigurator {

        val pickFromGalleryRequestCode: Int?
        val pickFromCameraRequestCode: Int?
        val pickFileRequestCode: Int?

        fun createCameraPictureIntent(mimeType: String): Pair<Intent, Uri?>

        fun onBeforePickPictureFromGallery(intent: Intent): Boolean = true
        fun onBeforePickPictureFromCamera(intent: Intent): Boolean = true
        fun onBeforePickContent(intent: Intent): Boolean = true

        fun retrieveUriAfterPickContent(data: Intent?): Uri?
        fun retrieveUriAfterPickFromCamera(outputFileUri: Uri?): Uri?
    }

    abstract class BasePickerConfigurator(
            override val pickFromGalleryRequestCode: Int?,
            override val pickFromCameraRequestCode: Int?,
            override val pickFileRequestCode: Int?
    ) : IPickerConfigurator

    /**
     * [IPickerConfigurator] для получения контента
     * 1. Из стороннего аппа с разными scheme:// - извлекается файловый path
     * 2. Камера - в предоставленный внешний файл по ссылке;
     * Применяется в версиях < N
     */
    abstract class BaseFilePickerConfigurator(
            private val context: Context,
            private val fileProviderAuthorityPostfix: String,
            pickFromGalleryRequestCode: Int? = null,
            pickFromCameraRequestCode: Int? = null,
            pickFileRequestCode: Int? = null
    ) : BasePickerConfigurator(pickFromGalleryRequestCode, pickFromCameraRequestCode, pickFileRequestCode) {

        abstract fun newCameraPictureFile(): File

        override fun createCameraPictureIntent(mimeType: String): Pair<Intent, Uri?> {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val localFileUri: Uri
            val outputFileUri = with(newCameraPictureFile()) {
                localFileUri = Uri.fromFile(this)
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
            // чтобы в дальнейшем извлечь File, из него ExifInterface и т.д.
            return Pair(captureIntent, localFileUri)
        }

        override fun retrieveUriAfterPickContent(data: Intent?): Uri? {
            // getPath в данном случае это путь к файлу
            val path = if (data != null && data.data != null) getPath(context, data.data) else EMPTY_STRING
            return if (path.isNotEmpty()) {
                Uri.fromFile(File(path))
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
            pickFromGalleryRequestCode: Int? = null,
            pickFromCameraRequestCode: Int? = null,
            pickFileRequestCode: Int? = null
    ) : BasePickerConfigurator(pickFromGalleryRequestCode, pickFromCameraRequestCode, pickFileRequestCode) {

        private val contentResolver = context.contentResolver

        override fun createCameraPictureIntent(mimeType: String): Pair<Intent, Uri?> {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val values = ContentValues(1)
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val outputUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            return Pair(captureIntent, outputUri)
        }

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

    companion object {

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
            val angle = getOrientationFromMediaStore(contentResolver, uri)

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
