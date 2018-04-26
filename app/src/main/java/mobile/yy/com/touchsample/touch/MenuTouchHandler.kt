package mobile.yy.com.touchsample.touch

import android.view.MotionEvent
import android.view.VelocityTracker
import mobile.yy.com.toucheventbus.AbstractTouchEventHandler
import mobile.yy.com.toucheventbus.TouchEventHandler
import mobile.yy.com.toucheventbus.TouchViewHolder
import mobile.yy.com.touchsample.ui.FakeMenu

/**
 * Created by 张宇 on 2018/4/26.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class MenuTouchHandler : AbstractTouchEventHandler<FakeMenu>() {

    private var touchForMenu = false
    private var lastX = 0f
    private var velocityTracker: VelocityTracker? = null

    override fun onTouch(ui: FakeMenu, e: MotionEvent, hasBeenIntercepted: Boolean): Boolean {
        super.onTouch(ui, e, hasBeenIntercepted)
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                if (e.x < 100 || ui.isOpenOrOpening()) {
                    touchForMenu = true
                    lastX = e.x
                    ui.down()
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    touchForMenu = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchForMenu) {
                    velocityTracker?.addMovement(e)
                    ui.move(e.x - lastX)
                    lastX = e.x
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (touchForMenu) {
                    velocityTracker?.computeCurrentVelocity(1)
                    ui.up(velocityTracker?.xVelocity ?: 0f)
                    velocityTracker?.recycle()
                }
            }
        }
        return touchForMenu
    }

    override fun defineNextHandlers(handlers: MutableList<Class<out TouchEventHandler<*, out TouchViewHolder<*>>>>) {
        handlers.add(BackgroundImageTouchHandler::class.java)
        handlers.add(TabTouchHandler::class.java)
        handlers.add(ZoomTextTouchHandler::class.java)
    }

    override fun name() = "MenuTouchHandler"
}