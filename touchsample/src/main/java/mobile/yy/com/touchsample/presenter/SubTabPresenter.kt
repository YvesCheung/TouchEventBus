package mobile.yy.com.touchsample.presenter

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import mobile.yy.com.touchsample.model.TabRepo

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class SubTabPresenter(subTabBiz: Int, repo: TabRepo) {

    private val subTab = repo.getCacheSubTab(subTabBiz)

    private val mainTab = repo.getCacheTab(subTab.bizId)

    private val onTextSizeChange = BehaviorSubject.createDefault(subTab.textSize)

    fun getContent() = "${mainTab.tabName}_${subTab.tabName}"

    fun getTextSize(): Observable<Float> = onTextSizeChange.hide()

    fun setTextSize(textSize: Float) {
        var size = textSize
        if (size > 200f) size = 200f
        else if (size < 30f) size = 30f
        subTab.textSize = size
        onTextSizeChange.onNext(size)
    }
}