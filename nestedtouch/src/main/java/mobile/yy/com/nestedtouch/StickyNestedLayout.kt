package mobile.yy.com.nestedtouch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.support.annotation.MainThread
import android.support.v4.view.NestedScrollingChild
import android.support.v4.view.NestedScrollingParent
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout

/**
 * Created by 张宇 on 2018/4/8.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 *
 * 滑动冲突时起承上启下的作用：
 * 处理外层SmartRefreshLayout和RecyclerView和自己的三层同向滑动。
 * 主要想法是把RecyclerView的NestedScrolling经过自己处理后，再传递给SmartRefreshLayout。
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class StickyNestedLayout : LinearLayout,
        NestedScrollingChild,
        NestedScrollingParent {

    companion object {
        private const val DEFAULT_DURATION = 250L //惯性下继续滑行的时间
    }

    private lateinit var headView: View
    private lateinit var navView: View
    private lateinit var contentView: View

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    private fun log(str: () -> Any?) = Log.v("StickyNestedLayout", str()?.toString() ?: "null")

    init {
        orientation = VERTICAL
        isNestedScrollingEnabled = true
    }

    //<editor-fold desc="基础布局部分">

    //固定id这个是参考张泓洋的StickyNavLayout 如果有更好解耦的办法 就修改掉吧
    //https://github.com/hongyangAndroid/Android-StickyNavLayout
    //但是张泓洋的项目不支持触摸headView的滑动，不支持在外面再嵌套一个NestedScrollingParent
    override fun onFinishInflate() {
        super.onFinishInflate()
        headView = findViewById(R.id.stickyHeadView)
        navView = findViewById(R.id.stickyNavView)
        contentView = findViewById(R.id.stickyContentView)

        //让headView是可以收触摸事件的 dispatchTouchEvent才能处理滑动的事件
        headView.isFocusable = true
        headView.isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        headView.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val params = contentView.layoutParams
        params.height = measuredHeight - navViewHeight - stickyOffsetHeight
        setMeasuredDimension(measuredWidth,
                headViewHeight + navViewHeight + contentViewHeight)
    }

    //</editor-fold desc="基础布局部分">

    //<editor-fold desc="基础滚动能力部分">

    /**
     * 正则化速度
     * 如果要让滑动速度减少，就除以更大的数值
     */
    private fun normalize(velocity: Float): Float = velocity / 1000f

    /**
     * 跟[scrollBy]类似，但会计算滑动后未消耗的距离。
     * 比如当需要滑行的距离为dy,但当滑行dy/3的距离之后，就已经滑行到顶部或者底部无法继续下去，那么有unconsumed[1]=dy*2/3。
     *
     * @param unconsumed 未消耗的距离
     */
    private fun scrollByWithUnConsumed(dx: Int, dy: Int, unconsumed: IntArray? = null) {
        scrollToWithUnConsumed(scrollX + dx, scrollY + dy, unconsumed)
    }

    /**
     * 跟[scrollTo]类似，但会计算滑动后未消耗的距离。
     * 比如当需要滑行的距离为dy,但当滑行dy/3的距离之后，就已经滑行到顶部或者底部无法继续下去，那么有unconsumed[1]=dy*2/3。
     *
     * @param unconsumed 未消耗的距离
     */
    private fun scrollToWithUnConsumed(dx: Int, dy: Int, unconsumed: IntArray? = null) {
        val scrollMax = headViewHeight - stickyOffsetHeight
        when {
            dy < 0 -> {
                scrollTo(dx, 0)
                unconsumed?.set(1, dy)
            }
            dy > scrollMax -> {
                scrollTo(dx, scrollMax)
                unconsumed?.set(1, dy - scrollMax)
            }
            else -> scrollTo(dx, dy)
        }
        scrollListeners.forEach { it.onScroll(this, scrollX, scrollY) }
        log { "scrollTo ($dx,$dy) unconsumed = ${unconsumed?.get(0)},${unconsumed?.get(1)}" }
    }

    private var valueAnimator: ValueAnimator? = null

    /**
     * 模拟惯性继续滑行
     *
     * @param velocityY 当前的滚动速度
     * @param unconsumed 在滑行结束后，还是没消费完的手势距离
     */
    private fun startFling(velocityY: Float, unconsumed: IntArray? = null) {

        fun end() {
            //自己结束消费手势，交给parent消费
            dispatchNestedFling(0f, velocityY, true)
            stopNestedScroll()
        }

        val velY = normalize(velocityY)
        if (Math.abs(velY) < 1f) { //速度已经很小，不需要惯性滚动
            end()
            return
        }
        val fromY = scrollY
        val toY = fromY + velY * DEFAULT_DURATION
        valueAnimator?.cancel()
        //在下一个时间片开始滑行，确保上一次的动画结束后，parent也消费完
        post {
            valueAnimator = ValueAnimator.ofFloat(1f).apply {
                interpolator = DecelerateInterpolator(2f)
                addUpdateListener {
                    val percent = it.animatedFraction
                    val curY = (toY - fromY) * percent + fromY
                    scrollToWithUnConsumed(0, curY.toInt(), unconsumed)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        end()
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        end()
                    }
                })
                duration = DEFAULT_DURATION
                start()
            }
        }
    }

    //</editor-fold" desc="基础滚动能力部分">

    //<editor-fold desc="嵌套滑动部分">

    //child告诉我要开始嵌套滑动
    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        log { "startNestedScroll" }
        startNestedScroll(nestedScrollAxes or ViewCompat.SCROLL_AXIS_VERTICAL) //开始通知parent的嵌套滑动
        return true
    }

    //child告诉我要停止嵌套滑动
    override fun onStopNestedScroll(child: View) {
        log { "stopNestedScroll $child" }
        stopNestedScroll() //结束parent的嵌套滑动
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        //dy > 0 上滑时处理

        val parentConsumed = IntArray(2)
        val offset = IntArray(2)
        dispatchNestedPreScroll(0, dy, parentConsumed, offset) //先分给parent搞事

        val leftY = dy - parentConsumed[1] //parent留给我的
        val headViewScrollDis = headViewHeight - scrollY - stickyOffsetHeight
        val headViewCanBeExpand = leftY > 0 && headViewScrollDis > 0 //上滑且headView能向上滚

        consumed[0] = parentConsumed[0] //x方向全是parent吃的
        if (headViewCanBeExpand) {
            log {
                "dx = $dx dy = $dy parentConsumed = ${parentConsumed[0]},${parentConsumed[1]}" +
                        " offset = ${offset[0]},${offset[1]} scrollY = $scrollY" +
                        " headViewHeight = $headViewHeight"
            }

            if (leftY > headViewScrollDis) { //滑的距离超过了能滚的距离
                scrollByWithUnConsumed(0, headViewScrollDis)
                consumed[1] = headViewScrollDis + parentConsumed[1]  //只消费能滚的最大距离
            } else {
                scrollByWithUnConsumed(0, leftY) //没超过滚的极限距离，那就滑多少滚多少
                consumed[1] = dy //把parent吃剩的全吃了 (parentConsumed[1] + leftY)
            }

        } else { //headView不能滑了 全是parent吃的
            consumed[1] = parentConsumed[1]
        }
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int,
                                dxUnconsumed: Int, dyUnconsumed: Int) {
        //dy < 0 下滑时处理
        var dyUnconsumedAfterMe = dyUnconsumed
        var dyConsumedAfterMe = dyConsumed
        val headViewScrollDis = scrollY
        if (dyUnconsumed < 0 && headViewScrollDis >= 0) { //下滑而且headView能向下滚
            log {
                "dxC = $dxConsumed dyC = $dyConsumed dxU = $dxUnconsumed dyU = $dyUnconsumed"
            }

            if (headViewScrollDis < Math.abs(dyUnconsumed)) { //滑动距离超过了可以滚的范围
                scrollByWithUnConsumed(0, -headViewScrollDis) //只滚我能滚的
                dyUnconsumedAfterMe = dyUnconsumed + headViewScrollDis //只消费我能滑的
                dyConsumedAfterMe = dyConsumed - headViewScrollDis
            } else { //全部都能消费掉
                scrollByWithUnConsumed(0, dyUnconsumed)
                dyUnconsumedAfterMe = 0
                dyConsumedAfterMe = dyConsumed + dyUnconsumed
            }
        }
        dispatchNestedScroll(0, dyConsumedAfterMe, 0, dyUnconsumedAfterMe, null)
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {

        //velocityY > 0 上滑
        if (dispatchNestedPreFling(velocityX, velocityY)) { //先给parent fling
            return true
        }
        val headViewScrollDis = headViewHeight - scrollY - stickyOffsetHeight
        if (velocityY > 0 && headViewScrollDis > 0) { //用户给了个向上滑动惯性 而且 headView还可以上滑
            startFling(velocityY, null)
            return true
        }
        return false
    }

    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {

        //velocityY < 0 下滑
        val headViewScrollDis = scrollY
        if (velocityY < 0 && !target.canScrollVertically(-1) && headViewScrollDis > 0) {
            startFling(velocityY, null)
            return dispatchNestedFling(velocityX, velocityY, true)
        }
        return dispatchNestedFling(velocityX, velocityY, consumed)
    }

    //</editor-fold desc="嵌套滑动部分">


    //<editor-fold desc="自身手势触摸处理部分">

    private var lastX = 0f
    private var lastY = 0f
    private var verticalScroll = false
    private var isInHeadView = false
    private val mVelocityTracker by lazy { VelocityTracker.obtain() }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        log { "dispatchTouchEvent ${ev.action}" }
        mVelocityTracker.addMovement(ev)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = ev.y
                lastX = ev.x
                verticalScroll = false
                isInHeadView = ev.y < headViewHeight + navViewHeight - scrollY
                if (isInHeadView) { //在头部View上的滑动才需要处理，其他地方的手势由<嵌套滑动部分>处理
                    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL or ViewCompat.SCROLL_AXIS_HORIZONTAL)
                }
            }
            MotionEvent.ACTION_MOVE -> {

                if (isInHeadView) {
                    val scrollByHuman = -Math.round(ev.y - lastY) //手势产生的距离
                    val consumedByParent = IntArray(2)
                    val offset = IntArray(2)
                    //先给parent消费
                    dispatchNestedPreScroll(0, scrollByHuman, consumedByParent, offset)
                    val scrollAfterParent = scrollByHuman - consumedByParent[1] //parent吃剩的
                    val unconsumed = IntArray(2)
                    scrollByWithUnConsumed(0, scrollAfterParent, unconsumed) //自己滑
                    val consumeY = scrollByHuman - unconsumed[1]
                    //滑剩的再给一次parent
                    dispatchNestedScroll(0, consumeY, 0, unconsumed[1], offset)
                }

                lastY = ev.y
                lastX = ev.x
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { //fliping

                if (isInHeadView) {
                    //根据当前速度 进行惯性滑行
                    mVelocityTracker.computeCurrentVelocity(1000)
                    val velY = mVelocityTracker.yVelocity
                    //先让parent消费
                    dispatchNestedPreFling(0f, -velY)
                    startFling(-velY, null)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        mVelocityTracker.recycle()
        super.onDetachedFromWindow()
    }
    //</editor-fold desc="自身手势触摸处理部分">

    //<editor-fold desc="公共API部分">

    /**
     * 滑动到id为stickyNavView的导航栏上方。
     * 比如点击微博的评论按钮进入微博详情页，会直接滑动到导航区域。
     */
    @MainThread
    fun scrollToNavView() {
        val toY = navView.y
        scrollTo(0, toY.toInt())
    }

    /**
     * 吸顶导航栏距离顶部的距离偏移。
     * 默认没有偏移。即导航栏可以滑动到顶部并吸附。
     */
    var stickyOffsetHeight: Int = 0
        @MainThread
        set(value) {
            field = if (value < 0) 0 else value
            requestLayout()
        }
        get() = Math.min(field, headViewHeight)

    /**
     * 获取头部区域的高度
     */
    val headViewHeight get() = headView.measuredHeight

    /**
     * 获取导航栏条的高度
     */
    val navViewHeight get() = navView.measuredHeight

    /**
     * 获取下部区域的高度
     */
    val contentViewHeight get() = contentView.measuredHeight

    private val scrollListeners = mutableListOf<OnScrollListener>()

    /**
     * 添加滚动监听器（内部View的滑动不会触发）
     * @see removeOnScrollChangeListener
     */
    fun addOnScrollListener(listener: OnScrollListener) {
        scrollListeners.add(listener)
    }

    /**
     * 移除滚动监听器
     * @see addOnScrollListener
     */
    fun removeOnScrollChangeListener(listener: OnScrollListener) {
        scrollListeners.remove(listener)
    }

    interface OnScrollListener {
        fun onScroll(view: StickyNestedLayout, scrollX: Int, scrollY: Int)
    }

    //</editor-fold desc="公共API部分">
}