package mobile.yy.com.toucheventbus.touchBus;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 张宇(G7428) on 2017/9/19.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 * <p>
 * 基本触摸事件处理框架 绑定指定类型的{@link TouchViewHolder} mViewHolder和 {@link TouchEventHandler} mNextHandler
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractTouchEventHandler<T> implements TouchEventHandler<T, TouchViewHolder<T>> {
    private List<Class<? extends TouchEventHandler>> mHandlers = new ArrayList<>();
    private TouchViewHolder<T> mViewHolder = new TouchViewHolder<>();

    public AbstractTouchEventHandler() {
        defineNextHandlers(mHandlers);
    }

    @NonNull
    @Override
    public TouchViewHolder<T> getViewHolder() {
        return mViewHolder;
    }

    @Override
    public List<Class<? extends TouchEventHandler>> nextHandler() {
        return mHandlers;
    }

    public abstract void defineNextHandlers(@NonNull List<Class<? extends TouchEventHandler>> handlers);

    @NonNull
    protected abstract String name();

    @Override
    public boolean forceMonitor() {
        return false;
    }

    @Override
    @CallSuper
    public boolean onTouch(@NonNull T t, MotionEvent e, boolean hasBeenIntercepted) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                Log.i("TouchEventHandler", name() + " intercepted = " +
                        hasBeenIntercepted + " event = " + e);
                break;
            default:
                break;
        }
        return false;
    }
}
