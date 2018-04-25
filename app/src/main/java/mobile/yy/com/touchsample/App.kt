package mobile.yy.com.touchsample

import android.app.Application
import mobile.yy.com.touchsample.inject.Injector

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class App : Application() {

    companion object {
        val injector = Injector()

        fun instance() = this
    }
}