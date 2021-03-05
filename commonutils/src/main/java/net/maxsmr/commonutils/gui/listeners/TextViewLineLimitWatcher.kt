package net.maxsmr.commonutils.gui.listeners

import android.animation.ObjectAnimator
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
        expandedText: CharSequence = EMPTY_STRING,
        collapsedText: CharSequence = EMPTY_STRING
) {

    private val textChangeListener: DefaultTextWatcher

    var expandListener: ((Boolean) -> Unit)? = null

    var useAnimation: Boolean = false

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

    var expandedText: CharSequence = EMPTY_STRING
        set(value) {
            if (field != value) {
                field = value
                if (triggerRefresh) {
                    refreshState()
                }
            }
        }

    var collapsedText: CharSequence = EMPTY_STRING
        set(value) {
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

        textChangeListener = object : DefaultTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                observeLayoutChanges()
            }
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
        this.expandedText = expandedText
        this.collapsedText = collapsedText
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
        if (useAnimation) {
            // FIXME аниматор не работает
            val animation = ObjectAnimator.ofInt(observableTextView, "maxLines", count)
            animation.setDuration(100).start()
        } else {
            observableTextView.maxLines = count
        }
    }

    protected open fun refreshExpanded() {
        if (isExpanded) {
            moreButton.text = expandedText
            setMaxLines(Integer.MAX_VALUE)
        } else {
            moreButton.text = collapsedText
            setMaxLines(lineLimit)
        }
    }

    protected open fun setMoreButtonVisibility(isVisible: Boolean) {
        moreButton.isVisible = isVisible
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
    }

    private fun refreshState() {
        if (lineLimit in 1 until observableTextView.lineCount) {
            refreshExpanded()
            setMoreButtonVisibility(true)
        } else {
            setMoreButtonVisibility(false)
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