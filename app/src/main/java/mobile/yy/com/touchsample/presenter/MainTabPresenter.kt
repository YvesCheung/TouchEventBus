package mobile.yy.com.touchsample.presenter

import mobile.yy.com.touchsample.model.TabRepo

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class MainTabPresenter(private val bizId: Int, private val repo: TabRepo) {

    fun getTab() = repo.getCacheTab(bizId)
}