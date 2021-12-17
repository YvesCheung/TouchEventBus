package mobile.yy.com.touchsample.touch

import android.view.MotionEvent
import mobile.yy.com.toucheventbus.AbstractTouchEventHandler
import mobile.yy.com.toucheventbus.TouchEventHandlerUtil.spacing
import mobile.yy.com.touchsample.ui.ZoomUi

/**
 * @author YvesCheung
 * 2018/4/26
 *
 * 双指缩放字体的触摸处理
 */
class ZoomTextTouchHandler : AbstractTouchEventHandler<ZoomUi>() {

    companion object {
        //判断为缩放的变化距离阈值
        private const val ZOOM_MIN_VALUE = 60f
        //缩放距离转化缩放百分比
        private const val ZOOM_REGULAR = 400f
    }

    private var space = 0f
    private var zoom = false

    override fun onTouch(ui: ZoomUi, e: MotionEvent, hasBeenIntercepted: Boolean): Boolean {
        super.onTouch(ui, e, hasBeenIntercepted)
        when (e.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                //两只手指都按到屏幕开始
                space = spacing(e)
            }
            MotionEvent.ACTION_MOVE -> {
                val current = spacing(e)
                val distance = current - space
                if (zoom) {
                    ui.resize(percentage = distance / ZOOM_REGULAR)
                    space = current
                } else if (Math.abs(distance) > ZOOM_MIN_VALUE) {
                    zoom = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                //双指离开屏幕时结束缩放
                zoom = false
                space = 0f
            }
        }
        return zoom
    }

    override fun nextHandler() = listOf(
            SlidingTabTouchHandler::class.java,
            TabTouchHandler::class.java,
            BackgroundImageTouchHandler::class.java
    )

    override fun name() = "ZoomTextTouchHandler"
}