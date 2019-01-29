package com.botty.secretnotes.utilities

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

private abstract class AfterTextChangedListener: TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}

fun EditText.afterTextChanged(onTextChanged: ((s: Editable?) -> Unit)) {
    addTextChangedListener(object: AfterTextChangedListener() {
        override fun afterTextChanged(s: Editable?) {
            onTextChanged.invoke(s)
        }
    })
}