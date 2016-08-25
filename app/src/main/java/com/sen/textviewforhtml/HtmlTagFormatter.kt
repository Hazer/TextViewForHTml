package com.sen.textviewforhtml

import java.util.HashMap
import java.util.Vector
import java.util.regex.Pattern

import org.xml.sax.Attributes

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.Layout
import android.text.Spanned
import android.text.style.*
import android.util.Log

class HtmlTagFormatter {
    private val mListParents = Vector<String>()//用来标记列表(有序和无序列表)
    private var mListItemCount = 0//用来标记列表(有序和无序列表)

    internal var mTagStyle = HashMap<String, String>()
    internal var mTagStartIndex = HashMap<String, Int>()//用来保存标签开始标记

    private var hrefValue: String? = null

    @Throws(NumberFormatException::class)
    fun handlerHtmlContent(context: Context, htmlContent: String): Spanned {
        return HtmlParser.buildSpannedText(htmlContent) { opening, tag, output, attributes ->
            when (tag) {
                TAG_HANDLE_SPAN, TAG_HANDLE_P -> if (opening) { //<style>标签的处理方式
                    mTagStartIndex.put(tag, output.length)

                    var styleContent = HtmlParser.getValue(attributes, TAG_HANDLE_STYLE)
                    styleContent = handleAlignAttribute(attributes, styleContent)

                    mTagStyle.put(tag, styleContent)
                } else {
                    handleStyleTag(output, tag, context)
                    mTagStyle.put(tag, "")
                }
                TAG_HANDLE_A -> if (opening) {
                    mTagStartIndex.put(tag, output.length)

                    var styleContent = HtmlParser.getValue(attributes, TAG_HANDLE_STYLE)
                    styleContent = handleAlignAttribute(attributes, styleContent)

                    hrefValue = HtmlParser.getValue(attributes, TAG_HANDLE_HREF)

                    mTagStyle.put(tag, styleContent)
                } else {
                    handleHref(output, tag, hrefValue!!)
                    handleStyleTag(output, tag, context)
                    mTagStyle.put(tag, "")
                }
                TAG_HANDLE_UL, TAG_HANDLE_OL, TAG_HANDLE_DD -> {
                    if (opening) {
                        mListParents.add(tag)
                    } else
                        mListParents.remove(tag)

                    mListItemCount = 0
                }
                TAG_HANDLE_LI -> if (!opening) {
                    handleListTag(output)
                }
            }
            false
        }
    }

    private fun handleHref(output: Editable, tag: String, link: String) {
        val startIndex = mTagStartIndex[tag]!!
        val stopIndex = output.length

//        output.removeSpan(output.getSpans(startIndex, stopIndex, URLSpan::class.java).first())
        output.setSpan(URLSpan(link), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun handleAlignAttribute(attributes: Attributes?, styleContent: String?): String? {
        return HtmlParser.getValue(attributes, TAG_HANDLE_ALIGN)?.let { alignContent ->
            if (styleContent.isNullOrBlank())
                return@let TAG_TEXT_ALIGN + ":" + alignContent

            var newStyle = styleContent!!

            if (!newStyle.endsWith(";")) newStyle += ";"

            newStyle += TAG_TEXT_ALIGN + ":" + alignContent

            return@let newStyle
        } ?: styleContent
    }

    //处理列表标签
    private fun handleListTag(output: Editable) {
        if (mListParents.lastElement() == "ul") {
            output.append("\n")
            val split = output.toString().split("\n".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()

            val lastIndex = split.size - 1
            val start = output.length - split[lastIndex].length - 1
            output.setSpan(BulletSpan(15), start, output.length, 0)
        } else if (mListParents.lastElement() == "ol") {
            mListItemCount++
            output.append("\n")
            val split = output.toString().split("\n".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()

            val lastIndex = split.size - 1
            val start = output.length - split[lastIndex].length - 1
            output.insert(start, "$mListItemCount. ")
            output.setSpan(LeadingMarginSpan.Standard(15 * mListParents.size), start, output.length, 0)
        }
    }

    //处理<text-align>
    private fun handleAlignTag(output: Editable, parentTag: String, alignTag: String) {
        val startIndex = mTagStartIndex[parentTag]
        val stopIndex = output.length

        val alinspan: AlignmentSpan = when (alignTag) {
            "center" -> AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER)
            "right" -> AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE)
            "left" -> AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL)
            else -> AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL)
        }
        // 参考:https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/text/SpannableStringBuilder.java
        // throw new RuntimeException("PARAGRAPH span must start at paragraph boundary");
        // AlignmentSpan继承ParagraphStyle;会检查前后是不是有换行符\n;没有的话抛出以上异常
        //        if(!"\n".equals(""+output.charAt(stopIndex-1))) {
        //            output.append("\n");
        //            stopIndex++;
        //
        //        }
        //        if(!"\n".equals(""+output.charAt(0))){
        //            output.insert(0,"\n");
        //            stopIndex++;
        //            startIndex--;
        //        }
        output.setSpan(alinspan, startIndex!!, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun handleStyleTag(output: Editable, tag: String, context: Context) {
        val styleContent = mTagStyle[tag]
        val startIndex = mTagStartIndex[tag]!!
        val stopIndex = output.length

        if (!styleContent.isNullOrBlank()) {
            val styleValues = styleContent!!.split(';')
            for (styleValue in styleValues) {
                val tmpValues = styleValue.split(':')
                if (tmpValues.size > 0) { //�?要标�?+数据才可食用(font-size=14px)
                    when (tmpValues[0]) {
                        TAG_FONT_SIZE -> {
                            val size = Integer.valueOf(getAllNumbers(tmpValues[1]))!!
                            Log.i("tag", "$size")
                            output.setSpan(AbsoluteSizeSpan(sp2px(context, size.toFloat())), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        TAG_BACKGROUND_COLOR -> {
                            output.setSpan(BackgroundColorSpan(Color.parseColor(tmpValues[1])), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        Tag_FONT_COLOR -> {
                            val str = output.toString()
                            output.setSpan(ForegroundColorSpan(Color.parseColor(tmpValues[1])), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        TAG_TEXT_ALIGN -> {
                            handleAlignTag(output, tag, tmpValues[1])
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val TAG_HANDLE_A = "a"
        private val TAG_HANDLE_P = "p"
        private val TAG_HANDLE_UL = "ul"
        private val TAG_HANDLE_OL = "ol"
        private val TAG_HANDLE_LI = "li"
        private val TAG_HANDLE_DD = "DD"
        private val TAG_HANDLE_HREF = "href"
        private val TAG_HANDLE_SPAN = "span"
        private val TAG_HANDLE_STYLE = "style"
        private val TAG_HANDLE_ALIGN = "align"
        private val TAG_FONT_SIZE = "font-size"
        private val TAG_BACKGROUND_COLOR = "background-color"
        private val Tag_FONT_COLOR = "color"
        private val TAG_TEXT_ALIGN = "text-align"

        //正则获取字体
        private fun getAllNumbers(body: String): String {
            val pattern = Pattern.compile("\\d+")
            val matcher = pattern.matcher(body)
            while (matcher.find()) {
                return matcher.group(0)
            }
            return ""
        }

        fun sp2px(context: Context, spValue: Float): Int {
            val fontScale = context.resources.displayMetrics.scaledDensity
            return (spValue * fontScale + 0.5f).toInt()
        }
    }
}
