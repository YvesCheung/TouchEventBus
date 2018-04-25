package mobile.yy.com.toucheventbus.touchBus;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 张宇(G7428) on 2017/9/5.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 * <p>
 * 用于对{@link TouchEventHandler}根据 {@link TouchEventHandler#nextHandler()}来进行排序
 */
class TouchEventHandlerContainer {
    private static final String TAG = "TouchEventHandler";
    private Map<Class<? extends TouchEventHandler>, TouchEventHandler<?, ? extends TouchViewHolder<?>>>
            mHandlers = new LinkedHashMap<>();
    private List<TouchEventHandler<?, ? extends TouchViewHolder<?>>> mContainer = new ArrayList<>();

    /**
     * 拓扑排序
     */
    @SuppressWarnings("unchecked")
    <VIEW> void put(TouchEventHandler<VIEW, TouchViewHolder<VIEW>> handler) {
        if (handler == null) {
            return;
        }

        Iterator<Map.Entry<Class<? extends TouchEventHandler>, TouchEventHandler<?, ? extends TouchViewHolder<?>>>>
                it = mHandlers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Class<? extends TouchEventHandler>, TouchEventHandler<?, ? extends TouchViewHolder<?>>>
                    entry = it.next();
            if (entry.getValue().getViewHolder().getView().isEmpty()) {
                Log.i(TAG, "remove " + entry.getValue());
                mContainer.remove(entry.getValue());
                it.remove();
            }
        }

        Map<Class<? extends TouchEventHandler>, Integer> handlerCnt = new HashMap<>();
        mHandlers.put(handler.getClass(), handler);
        mContainer.add(handler);
        for (TouchEventHandler<?, ? extends TouchViewHolder<?>> h : mContainer) {
            handlerCnt.put(h.getClass(), 0);
        }
        for (TouchEventHandler<?, ? extends TouchViewHolder<?>> h : mContainer) {
            List<Class<? extends TouchEventHandler<?, ? extends TouchViewHolder<?>>>> clz = h.nextHandler();
            if (clz != null) {
                for (Class<? extends TouchEventHandler> cls : clz) {
                    if (cls != null && mHandlers.containsKey(cls)) {
                        int cnt = handlerCnt.get(cls);
                        handlerCnt.put(cls, cnt + 1);
                    }
                }
            }
            Log.i(TAG, "handlerCnt = " + handlerCnt);
        }
        mContainer = new ArrayList<>();
        while (handlerCnt.size() > 0) {
            Class<? extends TouchEventHandler> hasKey = null;
            List<Class<? extends TouchEventHandler<?, ? extends TouchViewHolder<?>>>> nextKey = null;
            for (Map.Entry<Class<? extends TouchEventHandler>, Integer> e : handlerCnt.entrySet()) {
                if (e.getValue() <= 0) {
                    hasKey = e.getKey();
                    TouchEventHandler<?, ? extends TouchViewHolder<?>> h = mHandlers.get(hasKey);
                    if (h != null) {
                        mContainer.add(h);
                        nextKey = h.nextHandler();
                    }
                    break;
                }
            }

            if (hasKey != null) {
                handlerCnt.remove(hasKey);
            } else {
                //Log.e(TAG, "TouchHandler的事件分发存在环路，请检查nextHandler");
                throw new IllegalStateException("TouchHandler的事件分发存在环路，请检查nextHandler");
            }

            if (nextKey != null) {
                for (Class<? extends TouchEventHandler> nk : nextKey) {
                    if (nk != null && mHandlers.containsKey(nk)) {
                        int cnt = handlerCnt.get(nk);
                        handlerCnt.put(nk, cnt - 1);
                    }
                }
            }
        }
        Log.i(TAG, "order list = " + mContainer);
    }

    public <VIEW> void remove(TouchEventHandler<VIEW, TouchViewHolder<VIEW>> handler) {
        if (handler != null) {
            // mContainer.remove(handler);
            mHandlers.remove(handler.getClass());
        }
    }

    List<TouchEventHandler<?, ? extends TouchViewHolder<?>>> getOrderTouchEventHandler() {
        return mContainer;
    }

    TouchEventHandler getHandler(Class<? extends TouchEventHandler> cls) {
        return mHandlers.get(cls);
    }
}
