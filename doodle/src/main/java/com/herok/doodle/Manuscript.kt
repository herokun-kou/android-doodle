package com.herok.doodle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.abs

/**
 * @author herok
 *
 * A Manuscript class extends View.
 * Creates manuscript view in android layout.
 * Draw texts with regular size and draw lines like manuscript.
 *
 * @property[text] Text which displays in this view.
 * @property[textSize] Text size of [text].
 * @property[color] Color of manuscript lines.
 * @property[textColor] Text color of [text].
 * @property[minimumCellsPerLine] Minimum count of cells per line set by user.
 * @property[maximumCellsPerLine] Maximum count of cells per line set by user.
 * @property[orientation] Orientation of this view. Must be one of [ORIENTATION_HORIZONTAL], [ORIENTATION_VERTICAL_TO_LEFT], [ORIENTATION_VERTICAL_TO_RIGHT].
 * @property[removeBeginningAndEndOutline] Removes beginning outline and end outline of this view.
 * @property[removeLineNumber] Removes line number of this view.
 * @property[linesInParagraph] Holds sentences which split by newline character and max cells per line.
 * @property[cellSize] Size of cells.
 * @property[cellPadding] Padding of cells.
 * @property[marginBetweenLines] Margin of each lines.
 * @property[maximumCellsInScreenPerLine] Maximum count of cells per line set by code, related with view width(height in [orientation]: Vertical)
 * @property[textPaint] Paint object of texts.
 * @property[lineNumberPaint] Paint object of lineNumbers.
 * @property[linePaint] Paint object of lines.
 * @property[lineStrokeWidth] Stroke width of lines.
 * @property[maxMeasuredSize2] Max value of size2. Used to move canvas when [orientation] is [ORIENTATION_VERTICAL_TO_LEFT], to align canvas to right end of view.
 * @property[fullSize2] Real value of size2. Used to move canvas when [orientation] is [ORIENTATION_VERTICAL_TO_LEFT], to align canvas to right end of view.
 */
class Manuscript(context: Context, attrs: AttributeSet?): View(context, attrs) {

    /**
     * @property[TAG] Log tag constant.
     * @property[ORIENTATION_HORIZONTAL] Orientation constant.
     * @property[ORIENTATION_VERTICAL_TO_LEFT] Orientation constant.
     * @property[ORIENTATION_VERTICAL_TO_RIGHT] Orientation constant.
     */
    companion object {
        const val TAG = "Manuscript"

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

    var minimumCellsPerLine = -1
        set(value) {
            field = value
            if((minimumCellsPerLine != -1 && maximumCellsPerLine != -1) && (minimumCellsPerLine > maximumCellsPerLine)){
                Log.e(TAG, "Passed minimum column count value is bigger than maximum column count.")
                Log.e(TAG, "Setting maximum column count to minimum column count...")
                maximumCellsPerLine = minimumCellsPerLine
            }
            requestLayout()
        }

    var maximumCellsPerLine = -1
        set(value) {
            field = value
            if((minimumCellsPerLine != -1 && maximumCellsPerLine != -1) && (minimumCellsPerLine > maximumCellsPerLine)){
                Log.e(TAG, "Passed maximum column count value is smaller than minimum column count.")
                Log.e(TAG, "Setting minimum column count to maximum column count...")
                minimumCellsPerLine = maximumCellsPerLine
            }
            requestLayout()
        }

    var orientation = 0
        set(value) {
            field = value
            requestLayout()
        }

    var removeBeginningAndEndOutline = false
        set(value) {
            field = value
            invalidate()
        }

    var removeLineNumber = false
        set(value) {
            field = value
            invalidate()
        }

    private var linesInParagraph: MutableList<String> = ArrayList()

    private var cellSize = 0
    private var cellPadding = 0
    private var marginBetweenLines = 0
    private var maximumCellsInScreenPerLine = 0

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

                minimumCellsPerLine = getInt(R.styleable.Manuscript_minimumCellsPerLine, -1).coerceAtLeast(-1)

                maximumCellsPerLine = getInt(R.styleable.Manuscript_maximumCellsPerLine, -1).coerceAtLeast(-1)

                orientation = getInt(R.styleable.Manuscript_manuscriptOrientation, 0)

                color = getColor(R.styleable.Manuscript_manuscriptColor, Color.parseColor("#60FFFFFF"))

                textColor = getColor(R.styleable.Manuscript_manuscriptTextColor, Color.parseColor("#FFFFFF"))

                removeBeginningAndEndOutline = getBoolean(R.styleable.Manuscript_removeBeginningAndEndOutline, false)

                removeLineNumber = getBoolean(R.styleable.Manuscript_removeLineNumber, false)
            }finally {
                recycle()
            }
        }
        cellPadding = context.resources.getDimensionPixelSize(R.dimen.manuscript_text_padding)
        lineStrokeWidth = context.resources.getDimensionPixelSize(R.dimen.manuscript_stroke_width)
    }

    /**
     * Override function of [onMeasure].
     * Firstly, measure itself's width(height in vertical orientation) without screen size.
     * Secondly, measure itself's width(height in vertical orientation) with screen size and value of first step.
     * Lastly, measure itself's max cell count per line and separate text with measured max cell count.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if((minimumCellsPerLine != -1 && maximumCellsPerLine != -1) && (minimumCellsPerLine > maximumCellsPerLine)){
            throw Exception("minimumColumnCount is bigger than maximumColumnCount!!")
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        cellSize = textSize + cellPadding
        marginBetweenLines = (textSize / 2.5f).toInt()

        linesInParagraph.clear()
        linesInParagraph = separateText(maximumCellsPerLine) as MutableList<String>
        var maxLength = 0
        linesInParagraph.forEach{
            if(it.length > maxLength) maxLength = it.length
        }

        val sizeOfItself1 = maxLength.coerceAtLeast(minimumCellsPerLine) * cellSize + paddingLeft + paddingRight
        val sizeOfItself2: Int

        val measuredWidth: Int
        val measuredHeight: Int

        if(orientation == ORIENTATION_HORIZONTAL){
            measuredWidth = when(widthMode){
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
                MeasureSpec.UNSPECIFIED, MeasureSpec.AT_MOST -> sizeOfItself1.coerceAtMost(MeasureSpec.getSize(widthMeasureSpec))
                else -> sizeOfItself1
            }

            maximumCellsInScreenPerLine = measureMaxCellsInScreenPerLine(sizeOfItself1, measuredWidth)

            linesInParagraph = separateText(maximumCellsInScreenPerLine.coerceAtLeast(minimumCellsPerLine)) as MutableList<String>

            sizeOfItself2 = linesInParagraph.size * (cellSize + marginBetweenLines) + marginBetweenLines + paddingTop + paddingBottom

            Log.d(TAG, "size2: $sizeOfItself2")

            maxMeasuredSize2 = MeasureSpec.getSize(heightMeasureSpec)
            fullSize2 = sizeOfItself2

            measuredHeight = when(heightMode){
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
                MeasureSpec.UNSPECIFIED, MeasureSpec.AT_MOST -> sizeOfItself2.coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
                else -> sizeOfItself2
            }
        } else {
            measuredHeight = when(heightMode){
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
                MeasureSpec.UNSPECIFIED, MeasureSpec.AT_MOST -> sizeOfItself1.coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
                else -> sizeOfItself1
            }

            maximumCellsInScreenPerLine = measureMaxCellsInScreenPerLine(sizeOfItself1, measuredHeight)

            linesInParagraph = separateText(maximumCellsInScreenPerLine.coerceAtLeast(minimumCellsPerLine)) as MutableList<String>

            sizeOfItself2 = linesInParagraph.size * (cellSize + marginBetweenLines) + marginBetweenLines + paddingTop + paddingBottom

            maxMeasuredSize2 = MeasureSpec.getSize(widthMeasureSpec)
            fullSize2 = sizeOfItself2

            measuredWidth = when(widthMode){
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
                MeasureSpec.UNSPECIFIED, MeasureSpec.AT_MOST -> sizeOfItself2.coerceAtMost(MeasureSpec.getSize(widthMeasureSpec))
                else -> sizeOfItself2
            }
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /**
     * Override function [onDraw]
     * Draws text and lines.
     */
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
        lineNumberPaint.textSize = marginBetweenLines.coerceAtMost(context.resources.getDimensionPixelSize(R.dimen.max_line_number_text_size)).toFloat() - 5

        val maxTextLengthInRow = maximumCellsInScreenPerLine.coerceAtLeast(minimumCellsPerLine)

        canvas?.save()
        if(orientation == ORIENTATION_VERTICAL_TO_LEFT && fullSize2 - maxMeasuredSize2 > 0){
            canvas?.translate(-(fullSize2 - maxMeasuredSize2).toFloat(), 0f)
        }

        for(sentenceIndex in 0 until linesInParagraph.size){
            val sentence = linesInParagraph[sentenceIndex]
            for(letterIndex in 0 until maxTextLengthInRow){
                if(letterIndex < sentence.length) {
                    drawText(canvas, sentence[letterIndex], letterIndex, sentenceIndex)
                }
                if(letterIndex != 0)
                    drawLetterLine(canvas, letterIndex, sentenceIndex)
            }
            if(!removeLineNumber)
                drawLineNumber(canvas, sentenceIndex, maxTextLengthInRow)
            drawLineBetweenLinesInParagraph(canvas, sentenceIndex, maxTextLengthInRow)
        }
        drawOutlines(canvas, maxTextLengthInRow)

        canvas?.restore()
    }

    /**
     * Measures max cell counts in screen per line.
     * @param[size1] Size of real line's size.
     * @param[measuredSize] Size of max line's size.
     */
    private fun measureMaxCellsInScreenPerLine(size1: Int, measuredSize: Int): Int{
        var newSize1 = size1
        while(newSize1 > measuredSize){
            newSize1 -= cellSize
        }
        val padding = paddingTop + paddingBottom
        return (newSize1 - padding) / cellSize
    }

    /**
     * Separates text with given length and newline character.
     * @param[maxLength] Max length of separated sentences.
     */
    private fun separateText(maxLength: Int): List<String>{
        linesInParagraph.clear()
        val newLinedSentences = text.split("\n")
        if(maxLength > 0){
            newLinedSentences.forEach {
                var eachLine = ""
                for (index in it.indices) {
                    eachLine += it[index]
                    if((index+1) % maxLength == 0 || index == it.length - 1){
                        linesInParagraph.add(eachLine)
                        eachLine = ""
                    }
                }
            }
        }else{
            linesInParagraph.addAll(newLinedSentences)
        }
        return linesInParagraph
    }

    /**
     * Draws texts in canvas.
     * @param[canvas] Target canvas to draw.
     * @param[letter] Target letter to draw.
     * @param[letterIndex] Index of letter, used to position this letter.
     * @param[lineIndexInParagraph] [linesInParagraph]'s index which contains [letter], used to position [letter]
     */
    private fun drawText(canvas: Canvas?, letter: Char, letterIndex: Int, lineIndexInParagraph: Int){
        val textHeight = abs(textPaint.ascent()) + textPaint.descent()
        val diff = abs(textPaint.ascent()) - textHeight/2f

        var x = 0f; var y = 0f

        when(orientation){
            ORIENTATION_HORIZONTAL -> {
                x = paddingLeft +
                        (cellSize * letterIndex + cellSize / 2f)
                y = paddingTop +
                        ((marginBetweenLines + cellSize) * (lineIndexInParagraph + 1) + diff - textHeight/2f - (cellSize  - textHeight)/2f)
            }
            ORIENTATION_VERTICAL_TO_LEFT -> {
                x = (canvas?.width ?: measuredWidth) - (paddingRight + cellSize/2f + (marginBetweenLines) * (lineIndexInParagraph + 1) + lineIndexInParagraph * cellSize)
                y = paddingTop + ((letterIndex + 1) * cellSize) + diff - textHeight/2f - (cellSize  - textHeight)/2f
            }
            ORIENTATION_VERTICAL_TO_RIGHT -> {
                x = paddingLeft + cellSize/2f + (marginBetweenLines) * (lineIndexInParagraph + 1) + cellSize * lineIndexInParagraph
                y = paddingTop + ((letterIndex + 1) * cellSize) + diff - textHeight/2f - (cellSize  - textHeight)/2f
            }
        }
        if(letter == '.' || letter == ',') {
            val offset = if(orientation == 0 || orientation == -1) -cellSize/2f + 30 else cellSize/2f - 30
            canvas?.drawText(letter.toString(), x + offset, y + 15, textPaint)
        }else{
            canvas?.drawText(letter.toString(), x, y, textPaint)
        }
    }

    /**
     * Draws divider between letter and letter in line.
     * @param[canvas] Target canvas to draw.
     * @param[letterIndex] Index of letter, used to position this line
     * @param[lineIndexInParagraph] [linesInParagraph]'s index, used to position this line.
     */
    private fun drawLetterLine(canvas: Canvas?, letterIndex: Int, lineIndexInParagraph: Int){
        when(orientation) {
            ORIENTATION_HORIZONTAL -> {
                val x = paddingLeft + (letterIndex * cellSize).toFloat()
                val yStart =
                    paddingTop + ((lineIndexInParagraph + 1) * marginBetweenLines + lineIndexInParagraph * cellSize).toFloat()
                val yStop =
                    paddingTop + ((lineIndexInParagraph + 1) * marginBetweenLines + (lineIndexInParagraph + 1) * cellSize).toFloat()

                canvas?.drawLine(x, yStart, x, yStop, linePaint)
            }
            ORIENTATION_VERTICAL_TO_LEFT -> {
                val xStart = (canvas?.width ?: measuredWidth) -
                        (paddingRight + ((lineIndexInParagraph + 1) * marginBetweenLines + lineIndexInParagraph * cellSize)).toFloat()
                val xStop = (canvas?.width ?: measuredWidth) -
                        (paddingRight + ((lineIndexInParagraph + 1) * marginBetweenLines + (lineIndexInParagraph + 1) * cellSize)).toFloat()
                val y = paddingTop + (letterIndex * cellSize).toFloat()

                canvas?.drawLine(xStart, y, xStop, y, linePaint)
            }
            ORIENTATION_VERTICAL_TO_RIGHT -> {
                val xStart =
                    paddingLeft + ((lineIndexInParagraph + 1) * marginBetweenLines + lineIndexInParagraph * cellSize).toFloat()
                val xStop =
                    paddingLeft + ((lineIndexInParagraph + 1) * marginBetweenLines + (lineIndexInParagraph + 1) * cellSize).toFloat()
                val y = paddingTop + (letterIndex * cellSize).toFloat()

                canvas?.drawLine(xStart, y, xStop, y, linePaint)
            }
        }
    }

    /**
     * Draws line number in line margin.
     * @param[canvas] Target canvas to draw.
     * @param[lineIndexInParagraph] [linesInParagraph]'s index.
     * @param[maxCellsPerLineInParagraph] Max cells per line in paragraph, used to calculate current line in paragraph's cell count.
     */
    private fun drawLineNumber(canvas: Canvas?, lineIndexInParagraph: Int, maxCellsPerLineInParagraph: Int){
        var x = 0f; var y = 0f
        val horizontalTransition = 7.5f
        when(orientation){
            ORIENTATION_HORIZONTAL -> {
                x = paddingLeft + horizontalTransition
                y = paddingTop + ((lineIndexInParagraph + 1) * marginBetweenLines + lineIndexInParagraph * cellSize).toFloat() - lineNumberPaint.descent() -1.2f
            }
            ORIENTATION_VERTICAL_TO_LEFT -> {
                x = (canvas?.width ?: width) -
                        (paddingRight + ((lineIndexInParagraph + 1) * marginBetweenLines + lineIndexInParagraph * cellSize).toFloat()) + lineNumberPaint.descent()
                y = paddingTop + horizontalTransition
            }
            ORIENTATION_VERTICAL_TO_RIGHT -> {
                lineNumberPaint.textAlign = Paint.Align.RIGHT
                x = (paddingLeft + ((lineIndexInParagraph + 1) * marginBetweenLines + lineIndexInParagraph * cellSize).toFloat()) - lineNumberPaint.descent()
                y = paddingTop + horizontalTransition
            }
        }
        canvas?.save()
        if(orientation == 1)
            canvas?.rotate(90f, x, y)
        else if(orientation == -1)
            canvas?.rotate(-90f, x, y)
        canvas?.drawText("${maxCellsPerLineInParagraph * lineIndexInParagraph + 1}", x, y, lineNumberPaint)
        canvas?.restore()
        lineNumberPaint.textAlign = Paint.Align.LEFT
    }

    /**
     * Draws lines between linesInParagraphs.
     * @param[canvas] Target canvas to draw.
     * @param[lineIndexInParagraph] [linesInParagraph]'s index, to position this line.
     * @param[maxCellsPerLineInParagraph] Max cells per line in paragraph, used to draw this line.
     */
    private fun drawLineBetweenLinesInParagraph(canvas: Canvas?, lineIndexInParagraph: Int, maxCellsPerLineInParagraph: Int){
        when(orientation){
            ORIENTATION_HORIZONTAL -> {
                val startX = paddingLeft.toFloat()
                val stopX = paddingLeft + (maxCellsPerLineInParagraph * cellSize).toFloat()
                val topStartStopY = paddingTop + ((lineIndexInParagraph + 1) * marginBetweenLines + lineIndexInParagraph * cellSize).toFloat()
                val bottomStartStopY = paddingTop + ((lineIndexInParagraph + 1) * marginBetweenLines + (lineIndexInParagraph + 1) * cellSize).toFloat()

                canvas?.drawLine(startX, topStartStopY, stopX, topStartStopY, linePaint)
                canvas?.drawLine(startX, bottomStartStopY, stopX, bottomStartStopY, linePaint)
            }
            ORIENTATION_VERTICAL_TO_LEFT -> {
                val rightStartStopX = (canvas?.width ?: measuredWidth) - paddingRight - ((lineIndexInParagraph + 1) * marginBetweenLines + lineIndexInParagraph * cellSize).toFloat()
                val leftStartStopX = (canvas?.width ?: measuredWidth) - paddingRight - ((lineIndexInParagraph + 1) * marginBetweenLines + (lineIndexInParagraph + 1) * cellSize).toFloat()
                val startY = paddingTop.toFloat()
                val stopY = paddingTop + (maxCellsPerLineInParagraph * cellSize).toFloat()

                canvas?.drawLine(rightStartStopX, startY, rightStartStopX, stopY, linePaint)
                canvas?.drawLine(leftStartStopX, startY, leftStartStopX, stopY, linePaint)
            }
            ORIENTATION_VERTICAL_TO_RIGHT -> {
                val leftStartStopX = paddingLeft + ((lineIndexInParagraph + 1) * marginBetweenLines + lineIndexInParagraph * cellSize).toFloat()
                val rightStartStopX = paddingLeft + ((lineIndexInParagraph + 1) * marginBetweenLines + (lineIndexInParagraph + 1) * cellSize).toFloat()
                val startY = paddingTop.toFloat()
                val stopY = paddingTop + (maxCellsPerLineInParagraph * cellSize).toFloat()

                canvas?.drawLine(rightStartStopX, startY, rightStartStopX, stopY, linePaint)
                canvas?.drawLine(leftStartStopX, startY, leftStartStopX, stopY, linePaint)
            }
        }
    }

    /**
     * Draws outlines of this view.
     * @param[canvas] Target canvas to draw.
     * @param[maxCellsPerLineInParagraph] Max cells per line in paragraph, used to draw outline.
     */
    private fun drawOutlines(canvas: Canvas?, maxCellsPerLineInParagraph: Int){
        var x1 = 0f; var y1 = 0f; var x2 = 0f; var y2 = 0f
        when(orientation){
            ORIENTATION_HORIZONTAL -> {
                x1 = paddingLeft.toFloat()
                y1 = paddingTop.toFloat()
                x2 = paddingLeft + (cellSize * maxCellsPerLineInParagraph).toFloat()
                y2 = paddingTop + ((linesInParagraph.size + 1) * marginBetweenLines + linesInParagraph.size * cellSize).toFloat()
            }
            ORIENTATION_VERTICAL_TO_LEFT, ORIENTATION_VERTICAL_TO_RIGHT -> {
                x1 = paddingLeft.toFloat()
                y1 = paddingTop.toFloat()
                x2 = paddingLeft + ((linesInParagraph.size + 1) * marginBetweenLines + linesInParagraph.size * cellSize).toFloat()
                y2 = paddingTop + (cellSize * maxCellsPerLineInParagraph).toFloat()
            }
        }

        if(!removeBeginningAndEndOutline) {
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