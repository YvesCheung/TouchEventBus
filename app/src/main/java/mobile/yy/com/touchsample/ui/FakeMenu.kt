package mobile.yy.com.touchsample.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import mobile.yy.com.toucheventbus.touchBus.TouchEventBus
import mobile.yy.com.touchsample.R
import mobile.yy.com.touchsample.touch.MenuTouchHandler

/**
 * Created by 张宇 on 2018/4/26.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class FakeMenu : TextView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

//    companion object {
//        const val OPEN = 0
//        const val CLOSE = 1
//        const val OPENING = 2
//        const val CLOSING = 3
//        const val DRAG = 4
//    }
//
//    private var state = CLOSE

    init {
        text = "I AM MENU"
        setTextColor(Color.parseColor("#ff3399"))
        textSize = 25f
        gravity = Gravity.CENTER
        setBackgroundColor(resources.getColor(R.color.colorPrimary))
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        x = -w.toFloat()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        TouchEventBus.of(MenuTouchHandler::class.java).attach(this)
    }

    override fun onDetachedFromWindow() {
        TouchEventBus.of(MenuTouchHandler::class.java).dettach(this)
        super.onDetachedFromWindow()
    }

    private var animator = ValueAnimator.ofFloat(1f)

    private val velocity = 10f

    fun down() {
        animator.cancel()
    }

    fun move(dx: Float) {
        Log.i("zycheck", "$dx ${dx + x}")
        x = Math.min(Math.max(dx + x, -measuredWidth.toFloat()), 0f)
    }

    fun up() {
        val startX = x
        if (startX > -measuredWidth / 2f && startX < 0f) { //can open
            val dis = -startX
            val duration = dis / velocity
            animator = ValueAnimator.ofFloat(1f).apply {
                this.duration = duration.toLong()
                addUpdateListener {
                    val percentage = it.animatedFraction
                    x = startX + dis * percentage
                }
                start()
            }
        } else if (startX <= -measuredWidth / 2f && startX > -measuredWidth) { //can close
            val dis = startX + measuredWidth
            val duration = dis / velocity
            animator = ValueAnimator.ofFloat(1f).apply {
                this.duration = duration.toLong()
                addUpdateListener {
                    val percentage = it.animatedFraction
                    x = startX - dis * percentage
                }
                start()
            }
        }
    }

    fun isOpenOrOpening() = x > -measuredWidth
}