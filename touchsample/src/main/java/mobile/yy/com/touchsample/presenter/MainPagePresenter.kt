package mobile.yy.com.touchsample.presenter

import io.reactivex.Single
import mobile.yy.com.touchsample.model.MainTab
import mobile.yy.com.touchsample.model.TabRepo

/**
 * @author YvesCheung
 * 2018/4/25
 */
class MainPagePresenter(private val repo: TabRepo) {

    fun getBizTab(): Single<out List<MainTab>> = repo.getTabs()
}