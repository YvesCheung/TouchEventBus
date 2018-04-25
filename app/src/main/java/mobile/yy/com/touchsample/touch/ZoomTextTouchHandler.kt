package mobile.yy.com.touchsample.touch

import android.view.MotionEvent
import mobile.yy.com.toucheventbus.touchBus.AbstractTouchEventHandler
import mobile.yy.com.toucheventbus.touchBus.TouchEventHandler
import mobile.yy.com.toucheventbus.touchBus.TouchEventHandlerUtil.spacing
import mobile.yy.com.toucheventbus.touchBus.TouchViewHolder
import mobile.yy.com.touchsample.ui.ZoomUi

/**
 * Created by 张宇 on 2018/4/26.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 *
 * 双指缩放字体的触摸处理
 */
class ZoomTextTouchHandler : AbstractTouchEventHandler<ZoomUi>() {

    companion object {
        //判断为缩放的变化距离阈值
        private const val ZOOM_MIN_VALUE = 60f
        //缩放距离转化缩放百分比
        private const val ZOOM_REGULAR = 300f
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

    override fun defineNextHandlers(handlers: MutableList<Class<out TouchEventHandler<*, out TouchViewHolder<*>>>>) {
        handlers.add(SlidingTabTouchHandler::class.java)
        handlers.add(TabTouchHandler::class.java)
        handlers.add(BackgroundImageTouchHandler::class.java)
    }

    override fun name() = "ZoomTextTouchHandler"
}