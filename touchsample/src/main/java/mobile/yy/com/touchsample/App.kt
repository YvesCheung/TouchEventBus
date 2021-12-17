package mobile.yy.com.touchsample

import android.app.Application
import mobile.yy.com.touchsample.inject.Injector

/**
 * @author YvesCheung
 * 2018/4/25
 */
class App : Application() {

    companion object {
        val injector = Injector()

        fun instance() = this
    }
}