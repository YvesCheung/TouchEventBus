package mobile.yy.com.touchsample.touch

import android.support.v4.view.ViewPager
import android.view.MotionEvent
import mobile.yy.com.toucheventbus.AttachToViewTouchEventHandler
import mobile.yy.com.toucheventbus.TouchEventHandler
import mobile.yy.com.toucheventbus.TouchEventHandlerUtil.removePointers
import mobile.yy.com.toucheventbus.TouchViewHolder

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 *
 * 双指左右滑动的时候，使背景图的ViewPager发生滑动。
 * 1，在onTouch方法中判断是否双指滑动，双指的话就给背景图dispatch触摸。如果不是双指的move，就不处理。
 * 2，在defineNextHandler方法中拦截选项卡滑动的处理。
 */
class BackgroundImageTouchHandler : AttachToViewTouchEventHandler<ViewPager>() {

    private var twoPointer = false

    override fun onTouch(v: ViewPager, e: MotionEvent, hasBeenIntercepted: Boolean, insideView: Boolean): Boolean {
        //针对双指滑动的情况，全部触摸事件交由背景图处理
        if (e.pointerCount == 2) {
            twoPointer = true
        }
        //down up cancel比较特殊，都要传递下去
        if (twoPointer
                || e.action == MotionEvent.ACTION_DOWN
                || e.action == MotionEvent.ACTION_UP
                || e.action == MotionEvent.ACTION_CANCEL) {
            //双指滑动在ViewPager上会很不流畅，所以先把它模拟成单指传递下去
            removePointers(e) { event -> v.dispatchTouchEvent(event) }
        }
        //结束双指滑动
        if (e.action == MotionEvent.ACTION_UP
                || e.action == MotionEvent.ACTION_CANCEL) {
            twoPointer = false
        }
        return twoPointer
    }

    /**
     * 背景滑动的优先级要比选项卡滑动高
     */
    override fun defineNextHandlers(handlers: MutableList<Class<out TouchEventHandler<*, out TouchViewHolder<*>>>>) {
        handlers.add(TabTouchHandler::class.java)
    }

    override fun name() = "BackgroundImageTouchHandler"
}