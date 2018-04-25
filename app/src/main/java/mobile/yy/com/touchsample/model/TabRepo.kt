package mobile.yy.com.touchsample.model

import android.util.SparseArray
import io.reactivex.Observable
import io.reactivex.Single
import java.util.*

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
@Suppress("MemberVisibilityCanBePrivate")
class TabRepo {

    private val random = Random()

    private val mainTabStore = SparseArray<MainTab>()

    private val subTabStore = SparseArray<SubTab>()

    private val randomColor
        get() = (0x40 shl 24) or random.nextInt(0xffffff)

    private fun getBizId() = listOf(0, 1, 2, 3, 4)

    fun getTabs(): Single<out List<MainTab>> {
        return Observable.fromIterable(getBizId())
                .flatMapSingle { bizId -> getTab(bizId) }
                .reduce(mutableListOf()) { list: MutableList<MainTab>, tab: MainTab ->
                    list.apply { add(tab) }
                }
    }

    fun getTab(bizId: Int): Single<MainTab> {
        return getSubTabs(bizId)
                .map { MainTab(bizId, "Tab$bizId", randomColor, it) }
                .doOnSuccess { mainTabStore.put(bizId, it) }
    }

    fun getSubTabs(bizId: Int): Single<out List<SubTab>> {
        return Observable.fromArray(0, 1, 2)
                .map { idx -> SubTab(bizId, bizId * 10 + idx, "subTab$idx") }
                .doOnNext { tab -> subTabStore.put(tab.subBizId, tab) }
                .reduce(mutableListOf()) { list: MutableList<SubTab>, tab: SubTab ->
                    list.apply { add(tab) }
                }
    }

    fun getCacheTab(bizId: Int): MainTab {
        return mainTabStore[bizId]!!
    }

    fun getCacheSubTab(subBizId: Int): SubTab {
        return subTabStore[subBizId]!!
    }
}