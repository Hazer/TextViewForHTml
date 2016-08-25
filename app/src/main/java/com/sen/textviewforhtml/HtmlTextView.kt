package com.sen.textviewforhtml

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView

/**
 * Created by shouwang on 16/7/20.
 */
class HtmlTextView : TextView {
    constructor(context: Context) : super(context) {
        movementMethod = LinkMovementMethod.getInstance()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        movementMethod = LinkMovementMethod.getInstance()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        movementMethod = LinkMovementMethod.getInstance()
    }

    override fun setText(text: CharSequence, type: TextView.BufferType) {
        super.setText(text, type)
    }

    fun setHtmlText(text: CharSequence) {
        val htmlTagFormatter = HtmlTagFormatter()
        try {
            val spanned = htmlTagFormatter.handlerHtmlContent(context, text.toString())
//            Log.d("HtmlTextView", spanned.toString())
            setText(spanned)
        } catch (ex: Exception) {
            Log.d("HtmlTextView", "Failed to parse HTML", ex)
            setText(Html.fromHtml(text.toString()))
        }
    }
}
