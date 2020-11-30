package net.maxsmr.commonutils.android.media

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.graphics.decodeBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.android.disableFileUriStrictMode
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.graphic.GraphicUtils
import java.io.File

class ContentPicker(
    private val owner: LifecycleOwner,
    private val pickerConfigurator: IPickerConfigurator
) {

    constructor(
        context: Context,
        owner: LifecycleOwner,
        fileProviderAuthorityPostfix: String,
        pickFromGalleryRequestCode: Int? = null,
        pickFromCameraRequestCode: Int? = null,
        pickFileRequestCode: Int? = null,
        newCameraPictureFileFunc: () -> File
    ) : this(owner,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MediaStorePickerConfigurator(context, pickFromGalleryRequestCode, pickFromCameraRequestCode, pickFileRequestCode)
        } else {
            object : ContentPicker.BaseFilePickerConfigurator(context, fileProviderAuthorityPostfix, pickFromGalleryRequestCode, pickFromCameraRequestCode, pickFileRequestCode) {
                override fun newCameraPictureFile(): File = newCameraPictureFileFunc()
            }
        })

    private val activity: Activity
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
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            if (!onBeforePickPictureFromGallery(intent)) {
                return false
            }
            val activity = activity
            val fragment = if (owner is Fragment) owner else null
            if (fragment == null) {
                activity.startActivityForResult(intent, pickFromGalleryRequestCode)
            } else {
                fragment.startActivityForResult(intent, pickFromGalleryRequestCode)
            }
        }
        return true
    }

    fun pickFromCamera(): Boolean {
        with(pickerConfigurator) {
            val pickFromCameraRequestCode = pickFromCameraRequestCode ?: return false
            val intent = createCameraPictureIntent()
            if (!onBeforePickPictureFromCamera(intent.first)) {
                return false
            }
            cameraContentUri = intent.second
            val activity = activity
            val fragment = if (owner is Fragment) owner else null
            if (fragment == null) {
                activity.startActivityForResult(intent.first, pickFromCameraRequestCode)
            } else {
                fragment.startActivityForResult(intent.first, pickFromCameraRequestCode)
            }
        }
        return true
    }

    fun pickContent(): Boolean {
        with(pickerConfigurator) {
            val pickFileRequestCode = pickFileRequestCode ?: return false
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = mimeType
            if (!onBeforePickContent(intent)) {
                return false
            }
            val activity = activity
            val fragment = if (owner is Fragment) owner else null
            if (fragment == null) {
                activity.startActivityForResult(intent, pickFileRequestCode)
            } else {
                fragment.startActivityForResult(intent, pickFileRequestCode)
            }
        }
        return true
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

        /**
         * MimeType для случая [Intent.ACTION_GET_CONTENT]
         * или в базовой реализации [MediaStorePickerConfigurator]
         */
        val mimeType: String get() = "image/*"

        val pickFromGalleryRequestCode: Int?
        val pickFromCameraRequestCode: Int?
        val pickFileRequestCode: Int?

        fun createCameraPictureIntent(): Pair<Intent, Uri?>

        fun onBeforePickPictureFromGallery(intent: Intent): Boolean = true
        fun onBeforePickPictureFromCamera(intent: Intent): Boolean = true
        fun onBeforePickContent(intent: Intent): Boolean = true

        fun retrieveUriAfterPickContent(data: Intent?): Uri?
        fun retrieveUriAfterPickFromCamera(outputFileUri: Uri?): Uri?

        fun retrieveBitmapAfterPickContent(data: Intent?): Bitmap?
        fun retrieveBitmapAfterPickFromCamera(outputFileUri: Uri?): Bitmap?
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

        override fun createCameraPictureIntent(): Pair<Intent, Uri?> {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val outputFileUri = with(newCameraPictureFile()) {
                if (!TextUtils.isEmpty(fileProviderAuthorityPostfix)) {
                    // в манифесте аппа объявлен FileProvider
                    FileProvider.getUriForFile(context, "${context.packageName}.$fileProviderAuthorityPostfix", this)
                } else {
                    // попытка выключить "strict mode" для пользования обычным файлом
                    disableFileUriStrictMode()
                    Uri.fromFile(this)
                }
            }
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            return Pair(captureIntent, outputFileUri)
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

        override fun retrieveBitmapAfterPickContent(data: Intent?): Bitmap? {
            return createBitmap(retrieveUriAfterPickContent(data))
        }

        override fun retrieveBitmapAfterPickFromCamera(outputFileUri: Uri?): Bitmap? {
            return createBitmap(retrieveUriAfterPickFromCamera(outputFileUri))
        }

        private fun createBitmap(fileUri: Uri?): Bitmap? {
            return if (fileUri != null) {
                GraphicUtils.createBitmapFromFile(File(fileUri.path ?: EMPTY_STRING))
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

        override fun createCameraPictureIntent(): Pair<Intent, Uri?> {
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

        override fun retrieveBitmapAfterPickContent(data: Intent?): Bitmap? {
            return createBitmap(retrieveUriAfterPickContent(data))
        }

        @Suppress("DEPRECATION")
        override fun retrieveBitmapAfterPickFromCamera(outputFileUri: Uri?): Bitmap? {
            return createBitmap(retrieveUriAfterPickFromCamera(outputFileUri))
        }

        private fun createBitmap(contentUri: Uri?): Bitmap? {
            contentUri?.let {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.createSource(contentResolver, it).decodeBitmap { info, decoder -> }
                } else {
//                    MediaStore.Images.Media.getBitmap(contentResolver, it)
                    GraphicUtils.createBitmapFromUri(contentResolver, it)
                }
            }
            return null
        }
    }

    enum class ContentSource {
        GALLERY, CAMERA, OTHER
    }
}