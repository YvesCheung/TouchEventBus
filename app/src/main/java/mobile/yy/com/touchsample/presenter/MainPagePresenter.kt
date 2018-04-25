package mobile.yy.com.touchsample.presenter

import io.reactivex.Single
import mobile.yy.com.touchsample.model.MainTab
import mobile.yy.com.touchsample.model.TabRepo

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class MainPagePresenter(private val repo: TabRepo) {

    fun getBizTab(): Single<out List<MainTab>> = repo.getTabs()
}