package com.herok.doodle

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout

class ShootingStar(context: Context, attrs: AttributeSet?): FrameLayout(context, attrs) {

    companion object {
        const val SHOOTING_TYPE_DYNAMIC = 0
        const val SHOOTING_TYPE_STATIC = 1
    }

    private var _maxStarCount = 0
    val maxStarCount get() = _maxStarCount

    private var _starSizes = arrayOf(20, 30, 40, 50, 60, 70, 80)
    val starSizes get() = _starSizes

    private var _shootingType = 0
    val shootingType get() = _shootingType

    var starColor = Color.WHITE
        set(value){
            field = value
            resetStarColor()
        }

    constructor(context: Context): this(context, null)

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AnimatedMenu,
            0, 0
        ).apply {
            try {
                _maxStarCount = getInt(R.styleable.ShootingStar_maxStarCount, 0)

                val rawStarSizes = getTextArray(R.styleable.ShootingStar_starSizes)
                _starSizes = Array(rawStarSizes.size){ rawStarSizes[it].toString().toInt() }

                _shootingType = getInt(R.styleable.ShootingStar_shootingType, 0)

                starColor = getColor(R.styleable.ShootingStar_starColor, Color.WHITE)
            } finally {
                recycle()
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        setUp()
    }

    fun setUp(){

    }

    fun resetStarColor(){

    }

}