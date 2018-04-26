package mobile.yy.com.toucheventbus;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import static android.view.MotionEvent.ACTION_BUTTON_PRESS;
import static android.view.MotionEvent.ACTION_BUTTON_RELEASE;
import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_HOVER_ENTER;
import static android.view.MotionEvent.ACTION_HOVER_EXIT;
import static android.view.MotionEvent.ACTION_HOVER_MOVE;
import static android.view.MotionEvent.ACTION_MASK;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_OUTSIDE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_INDEX_MASK;
import static android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_SCROLL;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by 张宇(G7428) on 2017/9/6.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
@SuppressWarnings("WeakerAccess")
public class TouchEventHandlerUtil {
    private TouchEventHandlerUtil() {
    }

    /**
     * 把触摸事件修正到对应的view上 使{@link MotionEvent#getX()}和{@link MotionEvent#getY()}的相对位置
     * 是view的相对位置
     */
    public static void reviseToView(View v, MotionEvent event) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        event.offsetLocation(-location[0], -location[1]);
    }

    /**
     * 判断触摸事件是否在对应的view上
     */
    public static boolean isOnView(MotionEvent e, View v) {
        Rect r = new Rect();
        v.getGlobalVisibleRect(r);
        return r.left <= e.getRawX() && e.getRawX() <= r.right
                &&
                r.top <= e.getRawY() && e.getRawY() <= r.bottom;
    }

    /**
     * 两点之间的距离
     */
    public static float distance(float x1, float y1, float x2, float y2) {
        float x = x1 - x2;
        float y = y1 - y2;
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 计算MotionEvent事件二点间的距离
     *
     * @return float 二点间的距离
     */
    public static float spacing(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            return distance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
        }
        return -1;
    }

    /**
     * 如果是多点触摸，只保留一只手指的触摸事件。用于想用多指操作来触发Android设计给单指使用的View
     *
     * @param origin 原触摸事件
     */
    public static void removePointers(MotionEvent origin, Consumer<MotionEvent> newEvent) {
        int action;
        switch (origin.getActionMasked()) {
            case ACTION_POINTER_DOWN:
                action = ACTION_DOWN;
                break;
            case ACTION_POINTER_UP:
                action = ACTION_UP;
                break;
            default:
                action = origin.getAction();
                break;
        }
        final MotionEvent copy = MotionEvent.obtain(origin.getDownTime(), origin.getEventTime(), action,
                origin.getX(), origin.getY(), origin.getMetaState());
        newEvent.accept(copy);
        copy.recycle();
    }

    /**
     * Returns a string that represents the symbolic name of the specified unmasked action
     * such as "ACTION_DOWN", "ACTION_POINTER_DOWN(3)" or an equivalent numeric constant
     * such as "35" if unknown.
     *
     * @param action The unmasked action.
     * @return The symbolic name of the specified action.
     */
    public static String actionToString(int action) {
        switch (action) {
            case ACTION_DOWN:
                return "ACTION_DOWN";
            case ACTION_UP:
                return "ACTION_UP";
            case ACTION_CANCEL:
                return "ACTION_CANCEL";
            case ACTION_OUTSIDE:
                return "ACTION_OUTSIDE";
            case ACTION_MOVE:
                return "ACTION_MOVE";
            case ACTION_HOVER_MOVE:
                return "ACTION_HOVER_MOVE";
            case ACTION_SCROLL:
                return "ACTION_SCROLL";
            case ACTION_HOVER_ENTER:
                return "ACTION_HOVER_ENTER";
            case ACTION_HOVER_EXIT:
                return "ACTION_HOVER_EXIT";
            case ACTION_BUTTON_PRESS:
                return "ACTION_BUTTON_PRESS";
            case ACTION_BUTTON_RELEASE:
                return "ACTION_BUTTON_RELEASE";
        }
        int index = (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        switch (action & ACTION_MASK) {
            case ACTION_POINTER_DOWN:
                return "ACTION_POINTER_DOWN(" + index + ")";
            case ACTION_POINTER_UP:
                return "ACTION_POINTER_UP(" + index + ")";
            default:
                return Integer.toString(action);
        }
    }

    public interface Consumer<T> {
        void accept(T a);
    }
}
