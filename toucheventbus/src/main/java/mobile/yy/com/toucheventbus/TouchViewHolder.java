package mobile.yy.com.toucheventbus;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author YvesCheung
 * 2017/9/8
 */
public class TouchViewHolder<VIEW> {
    private final Set<VIEW> set = new HashSet<>();

    @NonNull
    Set<VIEW> getView() {
        return set;
    }

    /**
     * 把TouchEventHandler绑定到指定的ui上
     *
     * @param v ui：它具体可能并不是一个View，可以是一个接口，或者对触摸事件作出反应的对象。
     * @see #dettach(Object)
     */
    public void attach(VIEW v) {
        if (v != null) {
            set.add(v);
        }
    }

    /**
     * 当不再需要触摸事件时，需要解除绑定
     *
     * @param v 通过{@link #attach(Object)}方法绑定的表示层
     * @see #attach(Object)
     */
    public void dettach(VIEW v) {
        if (v != null) {
            set.remove(v);
        }
    }
}
