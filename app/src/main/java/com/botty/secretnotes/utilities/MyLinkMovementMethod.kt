package com.botty.secretnotes.utilities

import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView

private class MyLinkMovementMethod(val onLinkClick: (url: String, urlType: UrlType) -> Unit): ArrowKeyMovementMethod() {

    override fun onTouchEvent(widget: TextView?, buffer: Spannable?, event: MotionEvent?): Boolean {
        if(buffer?.isEmpty() != false) {
            return super.onTouchEvent(widget, buffer, event)
        }

        // If action has finished
        if (event?.action == MotionEvent.ACTION_UP) {
            // Locate the area that was pressed
            var x = event.x
            var y = event.y
            widget?.run {
                x -= totalPaddingLeft
                y -= totalPaddingTop
                x += scrollX
                y += scrollY
                // Locate the URL text
                val line = layout.getLineForVertical(y.toInt())
                val off = layout.getOffsetForHorizontal(line, x)

                // Find the URL that was pressed
                val link = buffer.getSpans(off, off, URLSpan::class.java)

                // If we've found a URL
                if (link?.isNotEmpty() == true) {
                    // Find the URL
                    val url = link[0].url
                    // If it's a valid URL
                    val urlType = when {
                        url.startsWith("https") or
                            url.startsWith("http") or
                            url.startsWith("www") -> UrlType.WEB

                        url.startsWith("tel") -> UrlType.PHONE

                        url.startsWith("mailto") -> UrlType.EMAIL

                        else -> null
                    }

                    urlType?.run { onLinkClick(url, this) }
                    return true
                }
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }
}

enum class UrlType {WEB, EMAIL, PHONE}

fun EditText.onLinkClicked(onClick: (url: String, urlType: UrlType) -> Unit) {
    movementMethod = MyLinkMovementMethod(onClick)
}