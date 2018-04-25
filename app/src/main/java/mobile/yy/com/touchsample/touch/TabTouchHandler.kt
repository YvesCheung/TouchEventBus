package mobile.yy.com.touchsample.touch

import android.support.v4.view.ViewPager
import android.view.MotionEvent
import mobile.yy.com.toucheventbus.touchBus.AttachToViewTouchEventHandler

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class TabTouchHandler : AttachToViewTouchEventHandler<ViewPager>() {
    override fun onTouch(v: ViewPager, e: MotionEvent, hasBeenIntercepted: Boolean, insideView: Boolean): Boolean {
        return v.dispatchTouchEvent(e)
    }

    override fun name() = "TabTouchEventBus"
}