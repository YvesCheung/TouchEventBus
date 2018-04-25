package mobile.yy.com.toucheventbus.touchBus;

import android.support.annotation.UiThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 张宇(G7428) on 2017/9/1.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 * <p>
 * 开播端直播间的触摸消息总线，负责把消息按顺序分发给TouchEventHandler
 */
@SuppressWarnings("unused")
public class TouchEventBus {
    private static final String TAG = "TouchEventHandler";
    private final static TouchEventBus mBus = new TouchEventBus();
    private final TouchEventHandlerContainer mContainer;

    private TouchEventBus() {
        mContainer = new TouchEventHandlerContainer();
    }

    private static TouchEventBus instance() {
        return mBus;
    }

    /**
     * 分发触摸事件的起点。
     * 一般在Activity的dispatchEvent开始分发。一个Activity上只能有一个起点。
     *
     * @param e          触摸事件
     * @param parentView 起点接收触摸事件的view。用于对触摸事件的发生坐标进行调整。
     */
    @UiThread
    public static void dispatchTouchEvent(MotionEvent e, View parentView) {
        List<TouchEventHandler<?, ? extends TouchViewHolder<?>>> orderList = new ArrayList<>(
                instance().mContainer.getOrderTouchEventHandler());
        e.offsetLocation(parentView.getScrollX() + e.getRawX() - e.getX(), parentView.getScrollY() + e.getRawY() - e.getY());
        boolean intercepted = false;
        for (TouchEventHandler<?, ? extends TouchViewHolder<?>> h : orderList) {
            MotionEvent copyEvent = MotionEvent.obtain(e);
            intercepted = dispatchInner(h, intercepted, copyEvent);
            copyEvent.recycle();
        }
    }

    @SuppressWarnings("unchecked")
    private static <VIEW> boolean dispatchInner(
            TouchEventHandler<?, ? extends TouchViewHolder<?>> handler,
            boolean intercepted, MotionEvent e) {
        final TouchEventHandler<VIEW, TouchViewHolder<VIEW>> h =
                (TouchEventHandler<VIEW, TouchViewHolder<VIEW>>) handler;
        boolean interceptChild = intercepted;
        TouchViewHolder<VIEW> vh = h.getViewHolder();
        for (VIEW v : vh.getView()) {
            interceptChild = h.onTouch(v, e, intercepted) || interceptChild;
        }
        return interceptChild;
    }

    /**
     * 获取对应的触摸事件处理器
     */
    @SuppressWarnings("unchecked")
    @UiThread
    public static <VIEW> TouchViewHolder<VIEW> of(Class<? extends TouchEventHandler<VIEW, TouchViewHolder<VIEW>>> cls) {
        TouchEventHandler handler = instance().mContainer.getHandler(cls);
        if (handler == null) {
            try {
                handler = cls.newInstance();
                instance().mContainer.put(handler);
            } catch (InstantiationException e) {
                Log.e(TAG, e.getMessage());
            } catch (IllegalAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        if (handler == null) {
            throw new IllegalStateException(cls + "需要一个无参的构造函数");
        }
        return handler.getViewHolder();
    }
}
