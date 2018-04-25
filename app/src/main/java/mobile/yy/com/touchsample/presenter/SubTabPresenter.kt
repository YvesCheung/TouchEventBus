package mobile.yy.com.touchsample.presenter

import mobile.yy.com.touchsample.model.TabRepo

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class SubTabPresenter(subTabBiz: Int, repo: TabRepo) {

    private val subTab = repo.getCacheSubTab(subTabBiz)

    private val mainTab = repo.getCacheTab(subTab.bizId)

    fun getContent() = "${mainTab.tabName}_${subTab.tabName}"
}