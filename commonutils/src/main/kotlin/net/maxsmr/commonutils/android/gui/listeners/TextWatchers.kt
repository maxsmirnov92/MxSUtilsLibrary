package net.maxsmr.commonutils.android.gui.listeners

import android.text.Editable
import android.text.TextWatcher

/**
 * [TextWatcher] с возможностью пропуска [onTextChanged] [skipCount] раз
 */
abstract class TextChangeWithSkipListener(private val skipCount: Int): TextWatcher {

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

    override fun afterTextChanged(s: Editable?) {
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }
}

open class DefaultTextWatcher : TextWatcher {

    override fun afterTextChanged(editable: Editable?) {
        //do nothing
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        //do nothing
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        //do nothing
    }
}

class AfterTextWatcher(val listener: (Editable) -> (Unit)) : TextWatcher {

    override fun afterTextChanged(s: Editable) {
        listener(s)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        //do nothing
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        //do nothing
    }
}

class BeforeTextWatcher(val listener: (s: CharSequence?, start: Int, before: Int, count: Int) -> Unit) : TextWatcher {

    override fun afterTextChanged(s: Editable) {
        //do nothing
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        listener(s, start, before, count)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        //do nothing
    }
}

class OnTextWatcher(val listener: (s: CharSequence?, start: Int, before: Int, count: Int) -> (Unit)) : TextWatcher {

    override fun afterTextChanged(s: Editable) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        listener(s, start, before, count)
    }
}
