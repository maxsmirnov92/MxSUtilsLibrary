package net.maxsmr.commonutils.gui.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.ListPopupWindow
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.ReflectionUtils.getFieldValue

class CustomAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    @AttrRes defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr), AdapterView.OnItemClickListener {

    private val ignoreAutoCompleteSelection: Boolean

    init {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.CustomAutoCompleteTextView, defStyleAttr, defStyleRes)
        try {
            ignoreAutoCompleteSelection =
                typedArray.getBoolean(R.styleable.CustomAutoCompleteTextView_ignoreAutoCompleteSelection, false)
        } finally {
            typedArray.recycle()
        }
        if (ignoreAutoCompleteSelection) {
            // убираем логику с автоматическим выставлением выбранной строки из списка в EditText
            val popup: ListPopupWindow? = getFieldValue(AutoCompleteTextView::class.java, this, "mPopup")
            popup?.setOnItemClickListener(this)
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (isPopupShowing) {
            onItemClickListener?.onItemClick(parent, view, position, id)
        }
    }
}