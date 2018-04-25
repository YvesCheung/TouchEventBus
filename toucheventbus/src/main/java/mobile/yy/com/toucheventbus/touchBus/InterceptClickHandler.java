package mobile.yy.com.toucheventbus.touchBus;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by 张宇(G7428) on 2017/9/6.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 * <p>
 * 你可以实现一个子类继承于{@link InterceptClickHandler}，然后拦截所有clickable或longClickable的View的触摸事件。
 * 注意这个Handler只会"拦截"点击事件，但不会处理点击事件，也就是不会触摸onClickListener之类的响应。
 */
@SuppressWarnings("WeakerAccess")
public abstract class InterceptClickHandler extends AttachToViewTouchEventHandler<ViewGroup> {
    private static final String TAG = "TouchEventHandler";

    @Override
    public boolean onTouch(@NonNull ViewGroup v, MotionEvent e, boolean hasBeenIntercepted, boolean insideView) {
        return hasBeenIntercepted
                || insideView
                && e.getAction() == MotionEvent.ACTION_UP
                && performClick(v, e);
    }

    /**
     * 判断当前触摸事件是否击中了可点击的view
     *
     * @param vg 需要判断的view范围
     * @param e  触摸事件
     * @return true如果击中了范围内的某个可点击的子view
     */
    protected boolean performClick(ViewGroup vg, MotionEvent e) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View v = vg.getChildAt(i);
            if (TouchEventHandlerUtil.isOnView(e, v)) {
                if (v.isClickable() || v.isLongClickable()) {
                    Log.i(TAG, name() + " hit v = " + v);
                    return true;
                } else if (v instanceof ViewGroup) {
                    if (performClick((ViewGroup) v, e)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
