package mobile.yy.com.toucheventbus;

import android.support.annotation.UiThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
    private static List<TouchEventHandler<?, ? extends TouchViewHolder<?>>> orderList = Collections.emptyList();
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
        //开始一串触摸事件
        if (e.getAction() == MotionEvent.ACTION_DOWN) { //获取ViewTree的顺序列表
            orderList = new ArrayList<>(instance().mContainer.getOrderTouchEventHandler());
        }
        //根据parentView修正触摸事件的坐标，处理parentView本身不是全屏的情况
        e.offsetLocation(parentView.getScrollX() + e.getRawX() - e.getX(), parentView.getScrollY() + e.getRawY() - e.getY());
        boolean intercepted = false;
        Iterator<TouchEventHandler<?, ? extends TouchViewHolder<?>>> itr = orderList.iterator();
        while (itr.hasNext()) {
            final TouchEventHandler<?, ? extends TouchViewHolder<?>> handler = itr.next();
            //为了性能这里只在每个Handler开始时copy一次MotionEvent
            //同一个ViewHolder前面的View如果在onTouch的时候很卑鄙地修改了MotionEvent的属性，会导致后面的View很懵逼
            //如果确实担心这种情况，就需要在ViewHolder每次遍历时再copy一次
            //如果完全不担心上层Handler修改MotionEvent，可以把下面这句obtain也去掉进一步提高性能
            MotionEvent copyEvent = MotionEvent.obtain(e);
            if (!intercepted || handler.forceMonitor()) {
                //上层没拦截 或者 自己死活都要触摸事件
                intercepted = dispatchInner(handler, intercepted, copyEvent);
            } else {
                //已经被拦截，发一个Cancel事件，而且不再参与接下来同一串的触摸事件
                copyEvent.setAction(MotionEvent.ACTION_CANCEL);
                dispatchInner(handler, true, copyEvent);
                itr.remove();
            }
            copyEvent.recycle();
        }
        //结束触摸后，清掉orderList防止内存泄漏
        if (e.getAction() == MotionEvent.ACTION_UP) {
            orderList = Collections.emptyList();
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
     *
     * @param <VIEW> ui的类型，可以是具体的View或者抽象的接口
     * @param cls    {@link TouchEventHandler} 的class
     * @return 该 {@link TouchEventHandler} 的 {@link TouchViewHolder}
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
