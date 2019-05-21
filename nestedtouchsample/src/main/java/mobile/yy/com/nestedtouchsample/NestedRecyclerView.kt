package mobile.yy.com.nestedtouchsample

import android.content.Context
import android.support.v4.view.NestedScrollingChild
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet

/**
 * Created by 张宇 on 2019/5/21.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class NestedRecyclerView : RecyclerView, NestedScrollingChild {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)

    constructor(context: Context, attrSet: AttributeSet, defStyle: Int) : super(context, attrSet, defStyle)

    /**
     * 原生的 RecyclerView 在 [consumed] 字段的处理上有点草率。
     * 只要满足方向上的特征就会把 fling 事件吃掉，因此这里再次做一个判读：
     * 当且仅当（ 方向上的特征满足 && 还能继续滑动 ）才把 fling 吃掉。
     */
    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {

        fun normalize(velocity: Float) = if (velocity > 0) 1 else -1

        val canScroll = canScrollHorizontally(normalize(velocityX)) ||
            canScrollVertically(normalize(velocityY))
        val realConsumed = consumed && canScroll
        return super.dispatchNestedFling(velocityX, velocityY, realConsumed)
    }
}