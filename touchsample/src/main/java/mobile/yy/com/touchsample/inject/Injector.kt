package mobile.yy.com.touchsample.inject

import mobile.yy.com.touchsample.model.TabRepo
import mobile.yy.com.touchsample.presenter.MainPagePresenter
import mobile.yy.com.touchsample.presenter.MainTabPresenter
import mobile.yy.com.touchsample.presenter.SubTabPresenter

/**
 * @author YvesCheung
 * 2018/4/25
 */
class Injector {

    private val tabRepo = TabRepo()

    fun getMainTabPresenter(bizId: Int) = MainTabPresenter(bizId, tabRepo)

    fun getMainPagePresenter() = MainPagePresenter(tabRepo)

    fun getSubTabPresenter(subBizId: Int) = SubTabPresenter(subBizId, tabRepo)
}