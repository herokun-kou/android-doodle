package com.herok.doodle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class Manuscript(context: Context, attrs: AttributeSet?): View(context, attrs) {

    companion object {
        const val ORIENTATION_HORIZONTAL = 0
        const val ORIENTATION_VERTICAL_TO_LEFT = 1
        const val ORIENTATION_VERTICAL_TO_RIGHT = -1
    }

    var text = ""
        set(value) {
            field = value
            requestLayout()
        }

    var textSize = 0
        set(value) {
            field = value
            requestLayout()
        }

    var color = 0
        set(value) {
            field = value
            invalidate()
        }

    var textColor = 0
        set(value) {
            field = value
            invalidate()
        }

    var minimumColumnCount = -1
        set(value) {
            field = value
            requestLayout()
        }

    var maximumColumnCount = -1
        set(value) {
            field = value
            requestLayout()
        }

    var orientation = 0
        set(value) {
            field = value
            requestLayout()
        }

    var removeHorizontalOutline = false
        set(value) {
            field = value
            invalidate()
        }

    var removeLineNumber = false
        set(value) {
            field = value
            invalidate()
        }

    private var sentences: MutableList<String> = ArrayList()

    private var textRectSize = 0
    private var marginBetweenSentences = 0
    private var maxColumnCountInScreen = 0
    private var textPadding = 0

    private var textPaint = Paint()
    private var lineNumberPaint = Paint()

    private var linePaint = Paint()
    private var lineStrokeWidth = 0

    private var maxMeasuredSize2 = 0
    private var fullSize2 = 0

    constructor(context: Context): this(context, null)

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.Manuscript,
            0,
            0
        ).apply {
            try {
                text = getString(R.styleable.Manuscript_manuscriptText) ?: ""

                textSize = getDimensionPixelSize(
                    R.styleable.Manuscript_manuscriptTextSize,
                    context.resources.getDimensionPixelSize(R.dimen.default_manuscript_text_size)
                )

                minimumColumnCount = getInt(R.styleable.Manuscript_minimumColumnCount, -1).coerceAtLeast(-1)

                maximumColumnCount = getInt(R.styleable.Manuscript_maximumColumnCount, -1).coerceAtLeast(-1)

                orientation = getInt(R.styleable.Manuscript_manuscriptOrientation, 0)

                color = getColor(R.styleable.Manuscript_manuscriptColor, Color.parseColor("#60FFFFFF"))

                textColor = getColor(R.styleable.Manuscript_manuscriptTextColor, Color.parseColor("#FFFFFF"))

                removeHorizontalOutline = getBoolean(R.styleable.Manuscript_removeHorizontalOutline, false)

                removeLineNumber = getBoolean(R.styleable.Manuscript_removeLineNumber, false)
            }finally {
                recycle()
            }
        }
        textPadding = context.resources.getDimensionPixelSize(R.dimen.manuscript_text_padding)
        lineStrokeWidth = context.resources.getDimensionPixelSize(R.dimen.manuscript_stroke_width)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        textRectSize = textSize + textPadding
        marginBetweenSentences = (textSize / 2.5f).toInt()

        if((minimumColumnCount != -1 && maximumColumnCount != -1) && (minimumColumnCount > maximumColumnCount)){
            throw Exception("minimumColumnCount is bigger than maximumColumnCount!!")
        }

        sentences.clear()
        sentences = separateText(maximumColumnCount) as MutableList<String>
        var maxLength = 0
        sentences.forEach{
            if(it.length > maxLength) maxLength = it.length
        }

        val size1 = maxLength.coerceAtLeast(minimumColumnCount) * textRectSize + paddingLeft + paddingRight
        val size2: Int

        val measuredWidth: Int
        val measuredHeight: Int

        if(orientation == ORIENTATION_HORIZONTAL){
            measuredWidth = when(widthMode){
                MeasureSpec.UNSPECIFIED, MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
                MeasureSpec.AT_MOST -> size1.coerceAtMost(MeasureSpec.getSize(widthMeasureSpec))
                else -> size1
            }

            maxColumnCountInScreen = getMaxColumnCountInScreen(size1, measuredWidth)

            sentences.clear()
            sentences = separateText(maxColumnCountInScreen.coerceAtLeast(minimumColumnCount)) as MutableList<String>

            size2 = sentences.size * (textRectSize + marginBetweenSentences) + marginBetweenSentences + paddingTop + paddingBottom

            maxMeasuredSize2 = MeasureSpec.getSize(heightMeasureSpec)
            fullSize2 = size2

            measuredHeight = when(heightMode){
                MeasureSpec.UNSPECIFIED, MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
                MeasureSpec.AT_MOST -> size2
                else -> size2
            }
        } else {
            measuredHeight = when(heightMode){
                MeasureSpec.UNSPECIFIED, MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
                MeasureSpec.AT_MOST -> size1.coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
                else -> size1
            }

            maxColumnCountInScreen = getMaxColumnCountInScreen(size1, measuredHeight)

            sentences.clear()
            sentences = separateText(maxColumnCountInScreen.coerceAtLeast(minimumColumnCount)) as MutableList<String>

            size2 = sentences.size * (textRectSize + marginBetweenSentences) + marginBetweenSentences + paddingTop + paddingBottom

            maxMeasuredSize2 = MeasureSpec.getSize(widthMeasureSpec)
            fullSize2 = size2

            measuredWidth = when(widthMode){
                MeasureSpec.UNSPECIFIED, MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
                MeasureSpec.AT_MOST -> size2
                else -> size2
            }
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = textSize.toFloat()

        linePaint.color = color
        linePaint.strokeWidth = lineStrokeWidth.toFloat()
        linePaint.style = Paint.Style.FILL

        lineNumberPaint.color = color
        lineNumberPaint.textAlign = Paint.Align.LEFT
        lineNumberPaint.textSize = marginBetweenSentences.coerceAtMost(context.resources.getDimensionPixelSize(R.dimen.max_line_number_text_size)).toFloat() - 5

        val maxTextLengthInRow = maxColumnCountInScreen.coerceAtLeast(minimumColumnCount)

        canvas?.save()
        if(orientation == ORIENTATION_VERTICAL_TO_LEFT && fullSize2 - maxMeasuredSize2 > 0){
            canvas?.translate(-(fullSize2 - maxMeasuredSize2).toFloat(), 0f)
        }

        for(sentenceIndex in 0 until sentences.size){
            val sentence = sentences[sentenceIndex]
            for(letterIndex in 0 until maxTextLengthInRow){
                if(letterIndex < sentence.length) {
                    drawText(canvas, sentence, letterIndex, sentenceIndex)
                }
                if(letterIndex != 0)
                    drawLetterLine(canvas, letterIndex, sentenceIndex)
            }
            if(!removeLineNumber)
                drawLineNumber(canvas, sentenceIndex, maxTextLengthInRow)
            drawSentenceLine(canvas, sentenceIndex, maxTextLengthInRow)
        }
        drawOuterLines(canvas, maxTextLengthInRow)

        canvas?.restore()
    }

    private fun getMaxColumnCountInScreen(size1: Int, measuredSize: Int): Int{
        var newSize1 = size1
        while(newSize1 > measuredSize){
            newSize1 -= textRectSize
        }
        val padding = paddingTop + paddingBottom
        return (newSize1 - padding) / textRectSize
    }

    private fun separateText(maxLength: Int): List<String>{
        val newLinedSentences = text.split("\n")
        if(maxLength > 0){
            newLinedSentences.forEach {
                var eachLine = ""
                for (index in it.indices) {
                    eachLine += it[index]
                    if((index+1) % maxLength == 0 || index == it.length - 1){
                        sentences.add(eachLine)
                        eachLine = ""
                    }
                }
            }
        }else{
            sentences.addAll(newLinedSentences)
        }
        return sentences
    }

    private fun drawText(canvas: Canvas?, sentence: String, letterIndex: Int, sentenceIndex: Int){
        val textHeight = abs(textPaint.ascent()) + textPaint.descent()
        val diff = abs(textPaint.ascent()) - textHeight/2f

        var x = 0f; var y = 0f

        when(orientation){
            ORIENTATION_HORIZONTAL -> {
                x = paddingLeft +
                        (textRectSize * letterIndex + textRectSize / 2f)
                y = paddingTop +
                        ((marginBetweenSentences + textRectSize) * (sentenceIndex + 1) + diff - textHeight/2f - (textRectSize  - textHeight)/2f)
            }
            ORIENTATION_VERTICAL_TO_LEFT -> {
                x = (canvas?.width ?: measuredWidth) - (paddingRight + textRectSize/2f + (marginBetweenSentences) * (sentenceIndex + 1) + sentenceIndex * textRectSize)
                y = paddingTop + ((letterIndex + 1) * textRectSize) + diff - textHeight/2f - (textRectSize  - textHeight)/2f
            }
            ORIENTATION_VERTICAL_TO_RIGHT -> {
                x = paddingLeft + textRectSize/2f + (marginBetweenSentences) * (sentenceIndex + 1) + textRectSize * sentenceIndex
                y = paddingTop + ((letterIndex + 1) * textRectSize) + diff - textHeight/2f - (textRectSize  - textHeight)/2f
            }
        }
        if(sentence[letterIndex] == '.' || sentence[letterIndex] == ',') {
            val offset = if(orientation == 0 || orientation == -1) -textRectSize/2f + 30 else textRectSize/2f - 30
            canvas?.drawText(sentence[letterIndex].toString(), x + offset, y + 15, textPaint)
        }else{
            canvas?.drawText(sentence[letterIndex].toString(), x, y, textPaint)
        }
    }

    private fun drawLetterLine(canvas: Canvas?, letterIndex: Int, sentenceIndex: Int){
        when(orientation) {
            ORIENTATION_HORIZONTAL -> {
                val x = paddingLeft + (letterIndex * textRectSize).toFloat()
                val yStart =
                    paddingTop + ((sentenceIndex + 1) * marginBetweenSentences + sentenceIndex * textRectSize).toFloat()
                val yStop =
                    paddingTop + ((sentenceIndex + 1) * marginBetweenSentences + (sentenceIndex + 1) * textRectSize).toFloat()

                canvas?.drawLine(x, yStart, x, yStop, linePaint)
            }
            ORIENTATION_VERTICAL_TO_LEFT -> {
                val xStart = (canvas?.width ?: measuredWidth) -
                        (paddingRight + ((sentenceIndex + 1) * marginBetweenSentences + sentenceIndex * textRectSize)).toFloat()
                val xStop = (canvas?.width ?: measuredWidth) -
                        (paddingRight + ((sentenceIndex + 1) * marginBetweenSentences + (sentenceIndex + 1) * textRectSize)).toFloat()
                val y = paddingTop + (letterIndex * textRectSize).toFloat()

                canvas?.drawLine(xStart, y, xStop, y, linePaint)
            }
            ORIENTATION_VERTICAL_TO_RIGHT -> {
                val xStart =
                    paddingLeft + ((sentenceIndex + 1) * marginBetweenSentences + sentenceIndex * textRectSize).toFloat()
                val xStop =
                    paddingLeft + ((sentenceIndex + 1) * marginBetweenSentences + (sentenceIndex + 1) * textRectSize).toFloat()
                val y = paddingTop + (letterIndex * textRectSize).toFloat()

                canvas?.drawLine(xStart, y, xStop, y, linePaint)
            }
        }
    }

    private fun drawLineNumber(canvas: Canvas?, sentenceIndex: Int, maxTextLengthInRow: Int){
        var x = 0f; var y = 0f
        when(orientation){
            ORIENTATION_HORIZONTAL -> {
                x = paddingLeft.toFloat() + 7.5f
                y = paddingTop + ((sentenceIndex + 1) * marginBetweenSentences + sentenceIndex * textRectSize).toFloat() - lineNumberPaint.descent() -1.2f
            }
            ORIENTATION_VERTICAL_TO_LEFT -> {
                x = (canvas?.width ?: width) -
                        (paddingRight + ((sentenceIndex + 1) * marginBetweenSentences + sentenceIndex * textRectSize).toFloat()) + lineNumberPaint.descent()
                y = paddingTop.toFloat() + 7.5f
            }
            ORIENTATION_VERTICAL_TO_RIGHT -> {
                lineNumberPaint.textAlign = Paint.Align.RIGHT
                x = (paddingLeft + ((sentenceIndex + 1) * marginBetweenSentences + sentenceIndex * textRectSize).toFloat()) - lineNumberPaint.descent()
                y = paddingTop.toFloat() + 7.5f
            }
        }
        canvas?.save()
        if(orientation == 1)
            canvas?.rotate(90f, x, y)
        else if(orientation == -1)
            canvas?.rotate(-90f, x, y)
        canvas?.drawText("${maxTextLengthInRow * sentenceIndex + 1}", x, y, lineNumberPaint)
        canvas?.restore()
        lineNumberPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawSentenceLine(canvas: Canvas?, sentenceIndex: Int, maxTextLengthInRow: Int){
        when(orientation){
            ORIENTATION_HORIZONTAL -> {
                val startX = paddingLeft.toFloat()
                val stopX = paddingLeft + (maxTextLengthInRow * textRectSize).toFloat()
                val topStartStopY = paddingTop + ((sentenceIndex + 1) * marginBetweenSentences + sentenceIndex * textRectSize).toFloat()
                val bottomStartStopY = paddingTop + ((sentenceIndex + 1) * marginBetweenSentences + (sentenceIndex + 1) * textRectSize).toFloat()

                canvas?.drawLine(startX, topStartStopY, stopX, topStartStopY, linePaint)
                canvas?.drawLine(startX, bottomStartStopY, stopX, bottomStartStopY, linePaint)
            }
            ORIENTATION_VERTICAL_TO_LEFT -> {
                val rightStartStopX = (canvas?.width ?: measuredWidth) - paddingRight - ((sentenceIndex + 1) * marginBetweenSentences + sentenceIndex * textRectSize).toFloat()
                val leftStartStopX = (canvas?.width ?: measuredWidth) - paddingRight - ((sentenceIndex + 1) * marginBetweenSentences + (sentenceIndex + 1) * textRectSize).toFloat()
                val startY = paddingTop.toFloat()
                val stopY = paddingTop + (maxTextLengthInRow * textRectSize).toFloat()

                canvas?.drawLine(rightStartStopX, startY, rightStartStopX, stopY, linePaint)
                canvas?.drawLine(leftStartStopX, startY, leftStartStopX, stopY, linePaint)
            }
            ORIENTATION_VERTICAL_TO_RIGHT -> {
                val leftStartStopX = paddingLeft + ((sentenceIndex + 1) * marginBetweenSentences + sentenceIndex * textRectSize).toFloat()
                val rightStartStopX = paddingLeft + ((sentenceIndex + 1) * marginBetweenSentences + (sentenceIndex + 1) * textRectSize).toFloat()
                val startY = paddingTop.toFloat()
                val stopY = paddingTop + (maxTextLengthInRow * textRectSize).toFloat()

                canvas?.drawLine(rightStartStopX, startY, rightStartStopX, stopY, linePaint)
                canvas?.drawLine(leftStartStopX, startY, leftStartStopX, stopY, linePaint)
            }
        }
    }

    private fun drawOuterLines(canvas: Canvas?, maxTextLengthInRow: Int){
        var x1 = 0f; var y1 = 0f; var x2 = 0f; var y2 = 0f
        when(orientation){
            ORIENTATION_HORIZONTAL -> {
                x1 = paddingLeft.toFloat()
                y1 = paddingTop.toFloat()
                x2 = paddingLeft + (textRectSize * maxTextLengthInRow).toFloat()
                y2 = paddingTop + ((sentences.size + 1) * marginBetweenSentences + sentences.size * textRectSize).toFloat()
            }
            ORIENTATION_VERTICAL_TO_LEFT, ORIENTATION_VERTICAL_TO_RIGHT -> {
                x1 = paddingLeft.toFloat()
                y1 = paddingTop.toFloat()
                x2 = paddingLeft + ((sentences.size + 1) * marginBetweenSentences + sentences.size * textRectSize).toFloat()
                y2 = paddingTop + (textRectSize * maxTextLengthInRow).toFloat()
            }
        }

        if(!removeHorizontalOutline) {
            canvas?.drawLine(x1, y1, x1, y2, linePaint)
            canvas?.drawLine(x2, y1, x2, y2, linePaint)
            canvas?.drawLine(x1, y1, x2, y1, linePaint)
            canvas?.drawLine(x1, y2, x2, y2, linePaint)
        }else if(orientation == 0){
            canvas?.drawLine(x1, y1, x2, y1, linePaint)
            canvas?.drawLine(x1, y2, x2, y2, linePaint)
        }else{
            canvas?.drawLine(x1, y1, x1, y2, linePaint)
            canvas?.drawLine(x2, y1, x2, y2, linePaint)
        }
    }

}