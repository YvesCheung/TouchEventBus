package mobile.yy.com.toucheventbus.touchBus;

import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by 张宇(G7428) on 2017/9/6.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 * <p>
 * 处理要依附在View上的触摸事件，可以判断事件是否发生在View上，并根据View的位置纠正点击事件的x y坐标。
 */
public abstract class AttachToViewTouchEventHandler<VIEW extends View> extends AbstractTouchEventHandler<VIEW> {
    @Override
    public boolean onTouch(@NonNull VIEW v, @NonNull MotionEvent e, boolean hasBeenIntercepted) {
        final boolean sup = super.onTouch(v, e, hasBeenIntercepted);
        TouchEventHandlerUtil.reviseToView(v, e);
        if (TouchEventHandlerUtil.isOnView(e, v)) {
            return onTouch(v, e, hasBeenIntercepted, true);
        } else if (forceMonitor()) {
            return onTouch(v, e, hasBeenIntercepted, false);
        }
        return sup;
    }

    /**
     * @param v                  接受触摸事件的ui表示
     * @param e                  触摸事件
     * @param hasBeenIntercepted 是否已经被前面的Handler拦截。只有当{@link #forceMonitor()}返回true时该值才有意义
     * @param insideView         是否触摸在View的范围内。只有当{@link #forceMonitor()}返回true时，
     *                           才能收到当前View范围以外的触摸事件
     * @return 是否已经消费这个触摸事件
     */
    public abstract boolean onTouch(@NonNull VIEW v, MotionEvent e, boolean hasBeenIntercepted, boolean insideView);
}
