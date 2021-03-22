package net.maxsmr.commonutils.gui.fragments.dialogs.holder

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage
import net.maxsmr.commonutils.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.isAtLeastKitkat
import net.maxsmr.commonutils.media.ContentPicker

const val TAG_PICK_CONTENT_CHOICE = "pick_content_choice"

abstract class ContentPickerDialogFragmentsHolder(
        private val dialogConfigurator: IDialogConfigurator,
        private val contentPickerOpts: ContentPickerOpts,
        tags: Collection<String>
) : DialogFragmentsHolder(mergeTags(listOf(TAG_PICK_CONTENT_CHOICE), tags)) {

    private var contentPicker: ContentPicker? = null

    abstract fun createPicker(owner: LifecycleOwner): ContentPicker

    override fun attachOwner(owner: LifecycleOwner, fragmentManager: FragmentManager) {
        super.attachOwner(owner, fragmentManager)
        contentPicker = createPicker(owner)
    }

    override fun onSetOtherEventListener(forFragment: DialogFragment, owner: LifecycleOwner) {
        if (forFragment is TypedDialogFragment<*>) {
            buttonClickLiveEvents(forFragment.tag).subscribe(owner) {
                onDialogButtonClick(forFragment, it.value)
            }
        }
    }

    @JvmOverloads
    fun showPictureModeDialog(context: Context, reshow: Boolean = true) {
        with(dialogConfigurator) {
            val positiveButton = this.getPickFromGalleryAlertButtonName(context)
            val neutralButton = this.getPickFromFileAlertButtonName(context)
            val negativeButton = this.getPickFromCameraAlertButtonName(context)
            val builder = TypedDialogFragment.DefaultTypedDialogBuilder()
            setupDialogBuilder(builder)
            show(TAG_PICK_CONTENT_CHOICE,
                    builder.setButtons(
                            positiveButton,
                            neutralButton,
                            negativeButton
                    ).build(context), reshow)
        }
    }

    protected open fun onDialogButtonClick(fragment: TypedDialogFragment<*>, which: Int) {
        if (fragment.tag == TAG_PICK_CONTENT_CHOICE) {
            contentPicker?.let {
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> it.pickFromGallery()
                    DialogInterface.BUTTON_NEUTRAL ->
                        if (isAtLeastKitkat()) {
                            with(contentPickerOpts) {
                                it.pickContent(type, mimeTypes, openOrGet)
                            }
                        } else {
                            // do noting
                        }
                    else -> it.pickFromCamera(true)
                }
            }
        }
    }

    interface IDialogConfigurator {

        fun getPickFromGalleryAlertButtonName(context: Context): TextMessage
        fun getPickFromFileAlertButtonName(context: Context): TextMessage
        fun getPickFromCameraAlertButtonName(context: Context): TextMessage

        fun setupDialogBuilder(builder: TypedDialogFragment.Builder<AlertDialog, TypedDialogFragment<AlertDialog>>)
    }

    data class ContentPickerOpts(
            val type: String?,
            val mimeTypes: List<String>?,
            val openOrGet: Boolean = true
    )
}