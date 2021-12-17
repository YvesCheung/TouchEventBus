package mobile.yy.com.touchsample.presenter

import mobile.yy.com.touchsample.model.TabRepo

/**
 * @author YvesCheung
 * 2018/4/25
 */
class MainTabPresenter(private val bizId: Int, private val repo: TabRepo) {

    fun getTab() = repo.getCacheTab(bizId)
}