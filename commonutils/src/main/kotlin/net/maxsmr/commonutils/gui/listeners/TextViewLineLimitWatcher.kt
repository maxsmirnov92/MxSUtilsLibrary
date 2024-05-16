package net.maxsmr.commonutils.gui.listeners

import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.view.isVisible
import net.maxsmr.commonutils.text.EMPTY_STRING

/**
 * Ограничивает максимальное число линий в [observableTextView] в зав-ти от указанного
 * @param observableTextView view, в которой обозревается занимаемое кол-во линий
 * @param moreButton скрываемая кнопка, по которой происходит показ/скрытие полного текста в соот-ии с [lineLimit]
 */
open class TextViewLineLimitWatcher @JvmOverloads constructor(
        protected val observableTextView: TextView,
        protected val moreButton: TextView,
        lineLimit: Int = 0,
        private val expandedTextFunc: (() -> CharSequence)? = null,
        private val collapsedTextFunc: (() -> CharSequence)? = null
) {

    private val textChangeListener: TextChangeListener

    var expandListener: ((Boolean) -> Unit)? = null

    var triggerChangeByClick: Boolean = true

    var lineLimit: Int = 0
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("lineLimit cannot be < 0")
            }
            if (field != value) {
                field = value
                if (triggerRefresh) {
                    refreshState()
                }
            }
        }

    var isExpanded: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (triggerRefresh) {
                    refreshState()
                }
                expandListener?.invoke(value)
            }
        }

    private var triggerRefresh: Boolean = true

    init {
        observeLayoutChanges()

        textChangeListener = TextChangeListener { _, _, _, _ ->
            observeLayoutChanges()
        }

        // при изначальном тексте не сработает
        observableTextView.addTextChangedListener(textChangeListener)

        moreButton.setOnClickListener {
            if (triggerChangeByClick) {
                toggleExpanded()
            } else {
                expandListener?.invoke(!isExpanded)
            }
        }

        triggerRefresh = false
        this.lineLimit = lineLimit
        triggerRefresh = true

        observableTextView.post {
            refreshState()
        }
    }

    fun dispose() {
        observableTextView.removeTextChangedListener(textChangeListener)
        moreButton.setOnClickListener(null)
    }

    protected open fun setMaxLines(count: Int) {
        observableTextView.maxLines = count
    }

    /**
     * Контент "показать/скрыть" отображается, рефреш всего остального в зав-ти от isExpanded
     */
    protected open fun refreshExpanded() {
        if (isExpanded) {
            moreButton.text = expandedTextFunc?.invoke() ?: EMPTY_STRING
            setMaxLines(Integer.MAX_VALUE)
        } else {
            moreButton.text = collapsedTextFunc?.invoke() ?: EMPTY_STRING
            setMaxLines(lineLimit)
        }
    }

    protected open fun setMoreContentVisibility(isVisible: Boolean) {
        moreButton.isVisible = isVisible
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
    }

    private fun refreshState() {
        if (lineLimit in 1 until observableTextView.lineCount) {
            refreshExpanded()
            setMoreContentVisibility(true)
        } else {
            setMoreContentVisibility(false)
        }
    }

    private fun observeLayoutChanges() {
        observableTextView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                observableTextView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                // фактическое число lineCount будет посчитано здесь
                // и оно может быть > чем отображаемое в данный момент (maxLines)
                refreshState()
            }
        })
    }
}