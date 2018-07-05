package mobile.yy.com.toucheventbus;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 张宇 on 2017/9/19.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 * <p>
 * 基本触摸事件处理骨架 绑定指定类型的{@link TouchViewHolder} viewHolder和 {@link TouchEventHandler} nextHandler
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractTouchEventHandler<T> implements TouchEventHandler<T, TouchViewHolder<T>> {

    private List<Class<? extends TouchEventHandler<?, ? extends TouchViewHolder<?>>>> mHandlers
            = new ArrayList<>();

    private TouchViewHolder<T> mViewHolder = new TouchViewHolder<>();

    public AbstractTouchEventHandler() {
        defineNextHandlers(mHandlers);
    }

    @Nullable
    @Override
    public List<Class<? extends TouchEventHandler<?, ? extends TouchViewHolder<?>>>> nextHandler() {
        return mHandlers;
    }

    @NonNull
    @Override
    public TouchViewHolder<T> getViewHolder() {
        return mViewHolder;
    }

    @Override
    public boolean forceMonitor() {
        return false;
    }

    @Override
    public boolean onTouch(@NonNull T t, @NonNull MotionEvent e, boolean hasBeenIntercepted) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                Log.i("TouchEventHandler", name() + " intercepted = " +
                        hasBeenIntercepted + " event = " + e);
                break;
            default:
                Log.v("TouchEventHandler", name() + " intercepted = " +
                        hasBeenIntercepted + " event = " + e);
                break;
        }
        return false;
    }

    /**
     * 返回当前Handler的名字，用于标志和打印日志
     *
     * @return 当前Handler名字，一般是类名
     */
    @NonNull
    protected abstract String name();

    /**
     * {@link #nextHandler()}的另一种实现方式，可以选择这个方法来实现
     *
     * @param handlers 一个可变的空列表，可以把下层的Handler添加到列表中
     * @see #nextHandler()
     */
    protected void defineNextHandlers(
            @NonNull List<Class<? extends TouchEventHandler<?, ? extends TouchViewHolder<?>>>> handlers) {
    }
}
