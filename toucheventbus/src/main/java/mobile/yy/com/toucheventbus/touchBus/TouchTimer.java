package mobile.yy.com.toucheventbus.touchBus;

import android.os.SystemClock;
import android.util.Log;

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */

public class TouchTimer {

    public static void invoke(String name) {
        Log.d("TouchTimer", SystemClock.uptimeMillis() + " : " + name);
    }
}
