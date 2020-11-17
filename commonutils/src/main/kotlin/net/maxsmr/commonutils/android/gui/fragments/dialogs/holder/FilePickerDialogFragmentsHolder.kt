package net.maxsmr.commonutils.android.gui.fragments.dialogs.holder

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.android.disableFileUriStrictMode
import net.maxsmr.commonutils.android.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.android.media.getPath
import net.maxsmr.commonutils.data.createFile
import net.maxsmr.commonutils.data.deleteFile
import net.maxsmr.commonutils.data.isFileExists
import java.io.File

const val TAG_PICK_FILE_CHOICE = "pick_file_choice"

open class FilePickerDialogFragmentsHolder(tags: Collection<String>) : DialogFragmentsHolder(mergeTags(listOf(TAG_PICK_FILE_CHOICE), tags)) {

    var filePickerConfigurator: IFilePickerConfigurator? = null

    var activity: Activity? = null

    var fragment: Fragment? = null

    var cameraPictureFile: File? = null
        private set

    override fun onSetOtherEventListener(forFragment: DialogFragment, owner: LifecycleOwner) {
        if (forFragment is TypedDialogFragment<*>) {
            buttonClickLiveEvents()
            buttonClickLiveEvents(forFragment.tag).subscribe(owner)  {
                onDialogButtonClick(forFragment, it.value)
            }
        }
    }

    fun onResult(context: Context, resultCode: Int, requestCode: Int, data: Intent?): String? {
        var result: String? = null
        if (Activity.RESULT_OK == resultCode) {
            val filePickerConfigurator = filePickerConfigurator
            checkNotNull(filePickerConfigurator) { "IFilePickerConfigurator is not specified" }
            if (requestCode == filePickerConfigurator.pickFromCameraRequestCode) {
                if (cameraPictureFile != null) {
                    result = cameraPictureFile!!.absolutePath
                }
            } else if (requestCode == filePickerConfigurator.pickFromGalleryRequestCode || requestCode == filePickerConfigurator.pickFileRequestCode) {
                result = if (data != null && data.data != null) getPath(context, data.data) else null
            }
        }
        return result
    }

    fun onResultUri(context: Context, resultCode: Int, requestCode: Int, data: Intent?): Uri? {
        val result = onResult(context, resultCode, requestCode, data)
        return if (!TextUtils.isEmpty(result)) Uri.fromFile(File(result)) else null
    }

    fun getContext(): Context {
        with(activity) {
            if (this != null) {
                return this
            } else {
                fragment?.let {
                    return it.requireContext()
                }
            }
        }
        throw IllegalStateException("No activity or fragment specified")
    }

    @JvmOverloads
    fun showPictureModeDialog(reshow: Boolean = true) {
        val context = getContext()
        val filePickerConfigurator = filePickerConfigurator
        checkNotNull(filePickerConfigurator) { "IFilePickerConfigurator is not specified" }
        val positiveButton = filePickerConfigurator.getPickFromGalleryAlertButtonName(context)
        val neutralButton = filePickerConfigurator.getPickFromFileAlertButtonName(context)
        val negativeButton = filePickerConfigurator.getPickFromCameraAlertButtonName(context)
        show(TAG_PICK_FILE_CHOICE, TypedDialogFragment.DefaultTypedDialogBuilder(getContext())
                .setIconResId(filePickerConfigurator.alertIconResId)
                .setMessage(filePickerConfigurator.getAlertMessage(context))
                .setCancelable(filePickerConfigurator.isAlertCancelable)
                .setButtons(if (!TextUtils.isEmpty(positiveButton)) positiveButton else null,
                        if (!TextUtils.isEmpty(neutralButton)) neutralButton else null,
                        if (!TextUtils.isEmpty(negativeButton)) negativeButton else null)
                .build(), reshow)
    }

    fun deleteCameraPictureFile() {
        cameraPictureFile?.let {
            deleteFile(it)
            cameraPictureFile = null
        }
    }

    protected open fun onDialogButtonClick(fragment: TypedDialogFragment<*>, which: Int) {
        if (fragment.tag == TAG_PICK_FILE_CHOICE) {
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> pickFromGallery()
                DialogInterface.BUTTON_NEUTRAL -> pickFile()
                DialogInterface.BUTTON_NEGATIVE -> pickFromCamera()
            }
        }
    }

    private fun pickFromGallery() {
        val filePickerConfigurator = filePickerConfigurator
        checkNotNull(filePickerConfigurator) { "IFilePickerConfigurator is not specified" }
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        if (!filePickerConfigurator.onBeforePickPictureFromGallery(intent)) {
            return
        }
        var handled = false
        val activity = activity
        val fragment = fragment
        if (fragment == null) {
            if (activity != null) {
                activity.startActivityForResult(intent, filePickerConfigurator.pickFromGalleryRequestCode)
                handled = true
            }
        } else {
            fragment.startActivityForResult(intent, filePickerConfigurator.pickFromGalleryRequestCode)
            handled = true
        }
        if (!handled) {
            throw RuntimeException("Activity or Fragment is not specified")
        }
    }

    private fun pickFromCamera() {
        val filePickerConfigurator = filePickerConfigurator
        checkNotNull(filePickerConfigurator) { "IFilePickerConfigurator is not specified" }
        val shouldRecreate = filePickerConfigurator.shouldDeletePreviousFile()
        with(cameraPictureFile) {
            val exists = isFileExists(this)
            if (exists) {
                if (shouldRecreate) {
                    deleteCameraPictureFile()
                } else {
                    logger.e("File \"$this\" exists and should not be recreated")
                    return
                }
            }
        }
        with(filePickerConfigurator.newCameraPictureFile()) {
            val fileUriProviderAuthority = filePickerConfigurator.fileProviderAuthorityPostfix
            val fileUri: Uri
            fileUri = if (!TextUtils.isEmpty(fileUriProviderAuthority)) {
                FileProvider.getUriForFile(getContext(), "${getContext().packageName}.$fileUriProviderAuthority", this)
            } else { // strict mode must be disabled in that case!
                disableFileUriStrictMode()
                Uri.fromFile(this)
            }
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            if (!filePickerConfigurator.onBeforePickPictureFromCamera(intent)) {
                return
            }
            val cameraPictureFile = createFile(name, parent, true)
            if (cameraPictureFile == null) {
                logger.e("Cannot create file \"$cameraPictureFile\"")
                return
            }
            this@FilePickerDialogFragmentsHolder.cameraPictureFile = cameraPictureFile
            val activity = activity
            val fragment = fragment
            var handled = false
            if (fragment == null) {
                if (activity != null) {
                    activity.startActivityForResult(intent, filePickerConfigurator.pickFromCameraRequestCode)
                    handled = true
                }
            } else {
                fragment.startActivityForResult(intent, filePickerConfigurator.pickFromCameraRequestCode)
                handled = true
            }
            if (!handled) {
                throw RuntimeException("Activity or fragment is not specified")
            }
        }
    }

    private fun pickFile() {
        val filePickerConfigurator = filePickerConfigurator
        checkNotNull(filePickerConfigurator) { "IFilePickerConfigurator is not specified" }
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        if (!filePickerConfigurator.onBeforePickFile(intent)) {
            return
        }
        val activity = activity
        val fragment = fragment
        var handled = false
        if (fragment == null) {
            if (activity != null) {
                activity.startActivityForResult(intent, filePickerConfigurator.pickFileRequestCode)
                handled = true
            }
        } else {
            fragment.startActivityForResult(intent, filePickerConfigurator.pickFileRequestCode)
            handled = true
        }
        if (!handled) {
            throw RuntimeException("Activity or Fragment is not specified")
        }
    }

    interface IFilePickerConfigurator {

        val pickFromGalleryRequestCode: Int
        val pickFromCameraRequestCode: Int
        val pickFileRequestCode: Int
        val isAlertCancelable: Boolean
        @get:DrawableRes
        val alertIconResId: Int
        val fileProviderAuthorityPostfix: String

        fun newCameraPictureFile(): File
        fun shouldDeletePreviousFile(): Boolean

        fun getAlertMessage(context: Context): String?

        fun getPickFromGalleryAlertButtonName(context: Context): String?
        fun getPickFromFileAlertButtonName(context: Context): String?
        fun getPickFromCameraAlertButtonName(context: Context): String?

        fun onBeforePickPictureFromGallery(intent: Intent): Boolean
        fun onBeforePickPictureFromCamera(intent: Intent): Boolean
        fun onBeforePickFile(intent: Intent): Boolean
    }
}