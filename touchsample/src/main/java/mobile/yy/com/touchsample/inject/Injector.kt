package mobile.yy.com.touchsample.inject

import mobile.yy.com.touchsample.model.TabRepo
import mobile.yy.com.touchsample.presenter.MainPagePresenter
import mobile.yy.com.touchsample.presenter.MainTabPresenter
import mobile.yy.com.touchsample.presenter.SubTabPresenter

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class Injector {

    private val tabRepo = TabRepo()

    fun getMainTabPresenter(bizId: Int) = MainTabPresenter(bizId, tabRepo)

    fun getMainPagePresenter() = MainPagePresenter(tabRepo)

    fun getSubTabPresenter(subBizId: Int) = SubTabPresenter(subBizId, tabRepo)
}