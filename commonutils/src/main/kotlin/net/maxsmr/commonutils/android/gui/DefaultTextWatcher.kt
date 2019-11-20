package net.maxsmr.commonutils.android.gui

import android.text.Editable
import android.text.TextWatcher

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