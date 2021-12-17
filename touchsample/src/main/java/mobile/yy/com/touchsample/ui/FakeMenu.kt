package mobile.yy.com.touchsample.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import mobile.yy.com.toucheventbus.TouchEventBus
import mobile.yy.com.touchsample.R
import mobile.yy.com.touchsample.touch.MenuTouchHandler

/**
 * @author YvesCheung
 * 2018/4/26
 */
class FakeMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_VELOCITY = 6f
    }

    init {
        text = "PANEL"
        setTextColor(Color.parseColor("#ff3399"))
        textSize = 25f
        gravity = Gravity.CENTER
        setBackgroundColor(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resources.getColor(R.color.colorPrimary, null)
        } else {
            resources.getColor(R.color.colorPrimary)
        })
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

    private
    var animator = ValueAnimator.ofFloat(1f)

    fun down() {
        animator.cancel()
    }

    fun move(dx: Float) {
        x = Math.min(Math.max(dx + x, -measuredWidth.toFloat()), 0f)
    }

    fun up(velocity: Float) {
        val startX = x
        fun animate(velocity: Float, dis: Float) {
            val duration = Math.abs(dis / velocity)
            animator = ValueAnimator.ofFloat(1f).apply {
                this.duration = duration.toLong()
                addUpdateListener {
                    val percentage = it.animatedFraction
                    x = startX + dis * percentage
                }
                start()
            }
        }

        fun open(velocity: Float) {
            val dis = -startX
            animate(velocity, dis)
        }

        fun close(velocity: Float) {
            val dis = startX + measuredWidth
            animate(velocity, -dis)
        }

        if (velocity > DEFAULT_VELOCITY) {
            open(velocity)
        } else if (velocity < -DEFAULT_VELOCITY) {
            close(velocity)
        } else if (startX > -measuredWidth / 2f && startX < 0f) {
            open(DEFAULT_VELOCITY)
        } else if (startX <= -measuredWidth / 2f && startX > -measuredWidth) {
            close(DEFAULT_VELOCITY)
        }
    }

    fun isOpenOrOpening() = x > -measuredWidth
}