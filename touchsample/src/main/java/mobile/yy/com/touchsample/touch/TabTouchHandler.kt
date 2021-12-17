package mobile.yy.com.touchsample.touch

import androidx.viewpager.widget.ViewPager
import android.view.MotionEvent
import mobile.yy.com.toucheventbus.AttachToViewTouchEventHandler

/**
 * @author YvesCheung
 * 2018/4/25
 */
class TabTouchHandler : AttachToViewTouchEventHandler<ViewPager>() {

    override fun onTouch(v: ViewPager, e: MotionEvent, hasBeenIntercepted: Boolean, insideView: Boolean): Boolean {
        return hasBeenIntercepted || v.dispatchTouchEvent(e)
    }

    //强行接收触摸事件 即使不是在ViewPager上的滑动也能收到
    //所以在下面的导航栏上滑动可以切换一级选项卡
    override fun forceMonitor() = true

    override fun name() = "TabTouchEventBus"
}