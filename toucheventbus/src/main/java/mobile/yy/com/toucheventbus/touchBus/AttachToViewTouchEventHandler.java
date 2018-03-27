package mobile.yy.com.toucheventbus.touchBus;

import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by 张宇(G7428) on 2017/9/6.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 * <p>
 * 处理要依附在View上的触摸事件，可以判断事件是否发生在View上，并根据View的位置纠正点击事件的x y坐标
 */
public abstract class AttachToViewTouchEventHandler<VIEW extends View> extends AbstractTouchEventHandler<VIEW> {
    @Override
    public boolean onTouch(@NonNull VIEW v, MotionEvent e, boolean hasBeenIntercepted) {
        final boolean sup = super.onTouch(v, e, hasBeenIntercepted);
        TouchEventHandlerUtil.reviseToView(v, e);
        if (TouchEventHandlerUtil.isOnView(e, v)) {
            return onTouch(v, e, hasBeenIntercepted, true);
        } else if (forceMonitor()) {
            return onTouch(v, e, hasBeenIntercepted, false);
        }
        return sup;
    }

    public abstract boolean onTouch(@NonNull VIEW v, MotionEvent e, boolean hasBeenIntercepted, boolean insideView);
}
