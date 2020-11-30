package net.maxsmr.commonutils.android.gui.fragments.dialogs.holder

import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.android.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.android.media.ContentPicker

const val TAG_PICK_CONTENT_CHOICE = "pick_content_choice"

abstract class ContentPickerDialogFragmentsHolder(
    private val dialogConfigurator: IDialogConfigurator,
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
            val builder = TypedDialogFragment.DefaultTypedDialogBuilder(context)
            setupDialogBuilder(builder)
            show(
                TAG_PICK_CONTENT_CHOICE, builder.setButtons(
                    if (!TextUtils.isEmpty(positiveButton)) positiveButton else null,
                    if (!TextUtils.isEmpty(neutralButton)) neutralButton else null,
                    if (!TextUtils.isEmpty(negativeButton)) negativeButton else null
                ).build(), reshow
            )
        }
    }

    protected open fun onDialogButtonClick(fragment: TypedDialogFragment<*>, which: Int) {
        if (fragment.tag == TAG_PICK_CONTENT_CHOICE) {
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> contentPicker?.pickFromGallery()
                DialogInterface.BUTTON_NEUTRAL -> contentPicker?.pickContent()
                DialogInterface.BUTTON_NEGATIVE -> contentPicker?.pickFromCamera()
            }
        }
    }

    interface IDialogConfigurator {

        fun getPickFromGalleryAlertButtonName(context: Context): String
        fun getPickFromFileAlertButtonName(context: Context): String
        fun getPickFromCameraAlertButtonName(context: Context): String

        fun setupDialogBuilder(builder: TypedDialogFragment.Builder<TypedDialogFragment<AlertDialog>>)
    }
}