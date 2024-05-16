package net.maxsmr.commonutils.gui.listeners

import android.text.Editable
import android.text.TextWatcher

/**
 * [TextWatcher] с возможностью пропуска [onTextChanged] [skipCount] раз
 */
abstract class TextChangeWithSkipListener(private val skipCount: Int): TextChangeListener {

    init {
        require(skipCount >= 0) { "Incorrect skipCount: $skipCount" }
    }

    private var count: Int = 0

    private var shouldCheckSkip = true

    abstract fun onTextChangedHandled(s: CharSequence?, start: Int, before: Int, count: Int)

    final override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        var handled = true
        if (shouldCheckSkip) {
            this.count++
            handled = this.count > skipCount
            if (handled) {
                // чтобы не инкрементировать счётчик оставшиеся разы
                shouldCheckSkip = false
            }
        }
        if (handled) {
            onTextChangedHandled(s, start, before, count)
        }
    }
}

fun interface BeforeTextChangeListener : TextWatcher {

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable) {}
}

fun interface TextChangeListener : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {}
}

fun interface AfterTextChangeListener : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}
