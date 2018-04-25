package mobile.yy.com.toucheventbus.touchBus;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import java.util.List;

/**
 * Created by 张宇(G7428) on 2017/9/4.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 * <p>
 * 触摸处理器:
 * <p/>
 * 所有需要触摸事件的业务都应该是一个独立的{@link TouchEventHandler}。比如上下滑切换直播间是一个Handler，
 * 左滑出现菜单是一个Handler，开播的摄像头手势缩放或者点击对焦是一个Handler，插件玩法需要触摸事件处理也是一
 * 个Handler等等。
 * <p/>
 * 每个Handler自己定义需要拦截哪些Handler。当触摸事件发生时，TouchEventBus并不是根据View树的顺序来分发事件，
 * 而是根据业务的需要，逐个Handler分发。通过{@link #nextHandler()}方法给出在此业务下方的Handler列表。那么
 * 当前Handler就会先于它们接收到触摸事件，从而判断是否需要拦截或者放开。
 * <p/>
 * 在{@link #onTouch(Object, MotionEvent, boolean)}方法写对触摸事件处理的业务。
 * <p/>
 * 如果当前Handler非常的霸道，无论前面的Handler是否已经拦截处理了触摸事件，自己都还是要收到触摸事件，那么就让
 * {@link #forceMonitor()}返回true。
 */
public interface TouchEventHandler<VIEW, HOLDER extends TouchViewHolder<VIEW>> {

    /**
     * @param view               需要对触摸事件作出反应的ui
     * @param e                  触摸事件
     * @param hasBeenIntercepted 是否已经被前面的Handler处理过。当且仅当{@link #forceMonitor()}返回true时，
     *                           该字段才有意义
     * @return 已经对触摸事件作出处理返回true
     */
    boolean onTouch(@NonNull VIEW view, @NonNull MotionEvent e, boolean hasBeenIntercepted);

    /**
     * @return 返回在自己后面的Handler列表
     */
    @Nullable
    List<Class<? extends TouchEventHandler<?, ? extends TouchViewHolder<?>>>> nextHandler();

    /**
     * 所有的ui都会存放在ViewHolder中。一个Handler会处理ViewHolder中所有View的事件。
     * 理论上一个Handler只会对应一个ui类。
     * ViewHolder是为了存放一个View可能会在同一时间内出现的多个实例。
     * 比如后一个Fragment实例B在前一个实例A的onDestroy之前先onCreate。
     */
    @NonNull
    HOLDER getViewHolder();

    /**
     * 是否强制监听触摸事件
     *
     * @return 返回true的话，无论前面的Handler是否已经处理过触摸事件，{@link #onTouch(Object, MotionEvent, boolean)}
     * 还是能收到触摸事件，但hasBeenIntercepted字段会为true。返回false的话，不会再收到触摸事件除非前面的Handler全
     * 都不处理
     */
    boolean forceMonitor();
}
