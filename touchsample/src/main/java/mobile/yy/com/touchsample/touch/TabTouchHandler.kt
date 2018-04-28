package mobile.yy.com.touchsample.touch

import android.support.v4.view.ViewPager
import android.view.MotionEvent
import mobile.yy.com.toucheventbus.AttachToViewTouchEventHandler

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
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