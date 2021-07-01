package mobile.yy.com.nestedtouch

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.IdRes
import android.support.annotation.MainThread
import android.support.annotation.StringRes
import android.support.v4.view.NestedScrollingChild2
import android.support.v4.view.NestedScrollingChildHelper
import android.support.v4.view.NestedScrollingParent2
import android.support.v4.view.NestedScrollingParentHelper
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewCompat.TYPE_TOUCH
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewConfiguration
import android.view.animation.Interpolator
import android.widget.LinearLayout
import android.widget.Scroller

/**
 * 滑动冲突时起承上启下的作用：
 * 处理外层SmartRefreshLayout和RecyclerView和自己的三层同向滑动。
 * 主要想法是把RecyclerView的NestedScrolling经过自己处理后，再传递给SmartRefreshLayout。
 *
 * @author YvesCheung
 * 2018/4/8
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
open class StickyNestedLayout : LinearLayout,
    NestedScrollingChild2,
    NestedScrollingParent2 {

    companion object {
        private const val DEBUG = false

        private val sQuinticInterpolator: Interpolator = Interpolator { t ->
            val f = t - 1.0f
            f * f * f * f * f + 1.0f
        }
    }

    /**
     * 是否正在嵌套滑动
     */
    private var isNestedScrollingStartedByChild = false
    /**
     * 是否由当前View主动发起的嵌套滑动
     */
    private var isNestedScrollingStartedByThisView = false

    private lateinit var headView: View
    private lateinit var navView: View
    private lateinit var contentView: View

    private val childHelper = NestedScrollingChildHelper(this)
    private val parentHelper = NestedScrollingParentHelper(this)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
        : super(context, attrs, defStyleAttr)

    private inline fun log(str: () -> Any?) {
        if (DEBUG) {
            Log.i("StickyNestedLayout", str()?.toString() ?: "null")
        }
    }

    private val mTouchSlop: Int

    init {
        orientation = VERTICAL
        isNestedScrollingEnabled = true
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledPagingTouchSlop
    }

    private fun string(@StringRes id: Int): String = context.getString(id)

    //<editor-fold desc="基础布局部分">

    override fun onFinishInflate() {
        super.onFinishInflate()

        headView = findChildView(R.id.stickyHeadView, R.string.stickyHeadView,
            "stickyHeadView")
        navView = findChildView(R.id.stickyNavView, R.string.stickyNavView,
            "stickyNavView")
        contentView = findChildView(R.id.stickyContentView, R.string.stickyContentView,
            "stickyContentView")

        //让headView是可以收触摸事件的 dispatchTouchEvent才能处理滑动的事件
        headView.isFocusable = true
        headView.isClickable = true
    }

    private fun findChildView(@IdRes id: Int, @StringRes strId: Int, msg: String): View {
        val viewOptional: View? = findViewById(id)
        if (viewOptional != null) {
            return viewOptional
        } else {
            val singleViewExpect = ArrayList<View>(1)
            findViewsWithText(singleViewExpect, string(strId), FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            return when {
                singleViewExpect.isEmpty() -> throw StickyNestedLayoutException(
                    "在StickyNestedLayout中必须要提供一个含有属性 android:id=\"@id/$msg\" 或者" +
                        "android:contentDescription=\"@string/$msg\" 的子View "
                )
                singleViewExpect.size > 1 -> throw StickyNestedLayoutException(
                    "在StickyNestedLayout中包含了多个含有属性 android:id=\"@id/$msg\" 或者" +
                        "android:contentDescription=\"@string/$msg\" 的子View，" +
                        "StickyNestedLayout无法确定应该使用哪一个"
                )
                else -> singleViewExpect.first()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val wrapContent = makeMeasureSpec(0, UNSPECIFIED)
        val expectContentHeight = makeMeasureSpec(
            measuredHeight - navViewHeight - stickyOffsetHeight,
            MeasureSpec.AT_MOST
        )
        measureChildWithMargins(headView, widthMeasureSpec, wrapContent)
        measureChildWithMargins(navView, widthMeasureSpec, wrapContent)
        measureChildWithMargins(contentView, widthMeasureSpec, expectContentHeight)
        setMeasuredDimension(measuredWidthAndState, measuredHeightAndState)
    }

    private fun measureChildWithMargins(child: View, parentWidthMeasureSpec: Int, parentHeightMeasureSpec: Int) {
        val lp = child.layoutParams as MarginLayoutParams
        val childWidthMeasureSpec =
            getChildMeasureSpec(
                parentWidthMeasureSpec,
                paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin,
                lp.width
            )
        val childHeightMeasureSpec =
            getChildMeasureSpec(
                parentHeightMeasureSpec,
                paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin,
                lp.height
            )
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    //</editor-fold desc="基础布局部分">

    //<editor-fold desc="基础滚动能力部分">

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
    }

    private var mScroller = Scroller(context, sQuinticInterpolator)

    /**
     * 模拟惯性继续滑行
     *
     * @param velocityY 当前的滚动速度
     */
    private fun startFling(velocityX: Float, velocityY: Float) {
        mScroller.fling(0, scrollY, 0, Math.round(velocityY),
            Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
        ViewCompat.postInvalidateOnAnimation(this)

        //自己结束消费手势，交给parent消费
        dispatchNestedFling(velocityX, velocityY, true)
        log { "stopNestedScroll" }
        isNestedScrollingStartedByChild = false
        isNestedScrollingStartedByThisView = false
        stopNestedScroll()
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollToWithUnConsumed(mScroller.currX, mScroller.currY, null)
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    //</editor-fold" desc="基础滚动能力部分">

    //<editor-fold desc="嵌套滑动部分">

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun hasNestedScrollingParent() = childHelper.hasNestedScrollingParent()

    override fun hasNestedScrollingParent(type: Int) = childHelper.hasNestedScrollingParent(type)

    override fun isNestedScrollingEnabled() = childHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int) = childHelper.startNestedScroll(axes)

    override fun startNestedScroll(axes: Int, type: Int) = childHelper.startNestedScroll(axes, type)

    override fun stopNestedScroll(type: Int) = childHelper.stopNestedScroll(type)

    override fun stopNestedScroll() = childHelper.stopNestedScroll()

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?, type: Int
    ) =
        childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed,
            dyUnconsumed, offsetInWindow, type)

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ) =
        childHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, offsetInWindow)

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?, type: Int
    ) =
        childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?
    ) =
        childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean) =
        childHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float) =
        childHelper.dispatchNestedPreFling(velocityX, velocityY)

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) =
        parentHelper.onNestedScrollAccepted(child, target, axes)

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) =
        parentHelper.onNestedScrollAccepted(child, target, axes, type)

    override fun getNestedScrollAxes() = parentHelper.nestedScrollAxes

    override fun onStopNestedScroll(child: View) = parentHelper.onStopNestedScroll(child)

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return onStartNestedScroll(child, target, nestedScrollAxes, TYPE_TOUCH)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        onNestedPreScroll(target, dx, dy, consumed, TYPE_TOUCH)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int
    ) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, TYPE_TOUCH)
    }

    //child告诉我要开始嵌套滑动
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        if (type == TYPE_TOUCH) {
            log { "onStartNestedScroll " }
            isNestedScrollingStartedByThisView = false
            isNestedScrollingStartedByChild = true
            startNestedScroll(nestedScrollAxes or ViewCompat.SCROLL_AXIS_VERTICAL) //开始通知parent的嵌套滑动
            return true
        }
        return false
    }

    //child告诉我要停止嵌套滑动
    override fun onStopNestedScroll(target: View, type: Int) {
        if (type == TYPE_TOUCH) {
            log { "onStopNestedScroll $target " }
            isNestedScrollingStartedByThisView = false
            isNestedScrollingStartedByChild = false
            stopNestedScroll() //结束parent的嵌套滑动
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray?, type: Int) {
        if (isNestedScrollingStartedByChild && type == TYPE_TOUCH) {
            //dy > 0 上滑时处理
            val parentConsumed = IntArray(2)
            val offset = IntArray(2)
            dispatchNestedPreScroll(0, dy, parentConsumed, offset) //先分给parent搞事

            val leftY = dy - parentConsumed[1] //parent留给我的
            val headViewScrollDis = headViewHeight - scrollY - stickyOffsetHeight
            val headViewCanBeExpand = leftY > 0 && headViewScrollDis > 0 //上滑且headView能向上滚

            consumed?.set(0, parentConsumed[0]) //x方向全是parent吃的
            if (headViewCanBeExpand) {
                if (leftY > headViewScrollDis) { //滑的距离超过了能滚的距离
                    scrollByWithUnConsumed(0, headViewScrollDis)
                    consumed?.set(1, headViewScrollDis + parentConsumed[1]) //只消费能滚的最大距离
                } else {
                    scrollByWithUnConsumed(0, leftY) //没超过滚的极限距离，那就滑多少滚多少
                    consumed?.set(1, dy) //把parent吃剩的全吃了 (parentConsumed[1] + leftY)
                }

            } else { //headView不能滑了 全是parent吃的
                consumed?.set(1, parentConsumed[1])
            }
        }
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, type: Int
    ) {
        if (isNestedScrollingStartedByChild && type == TYPE_TOUCH) {
            //dy < 0 下滑时处理
            var dyUnconsumedAfterMe = dyUnconsumed
            var dyConsumedAfterMe = dyConsumed
            val headViewScrollDis = scrollY

            if (dyUnconsumed < 0 && headViewScrollDis >= 0) { //下滑而且headView能向下滚
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

            dispatchNestedScroll(0, dyConsumedAfterMe, 0,
                dyUnconsumedAfterMe, null)
        }
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {

        //velocityY > 0 上滑
        if (dispatchNestedPreFling(velocityX, velocityY)) { //先给parent fling
            return true
        }
        val headViewScrollDis = headViewHeight - scrollY - stickyOffsetHeight
        if (velocityY > 0 && headViewScrollDis > 0) { //用户给了个向上滑动惯性 而且 headView还可以上滑
            startFling(velocityX, velocityY)
            return true
        }
        return false
    }

    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        log { "onNestedFling vy = $velocityY, consumed = $consumed" }
        //velocityY < 0 下滑
        val headViewScrollDis = scrollY
        if (!consumed &&
            velocityY < 0 &&
            !target.canScrollVertically(-1) &&
            headViewScrollDis > 0) {
            startFling(velocityX, velocityY)
            return true
        }
        return dispatchNestedFling(velocityX, velocityY, consumed)
    }

    //</editor-fold desc="嵌套滑动部分">


    //<editor-fold desc="自身手势触摸处理部分">

    private var lastX = 0f
    private var lastY = 0f
    private var downRawY = 0f
    private var downRawX = 0f

    private val gestureHandler = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            if (isNestedScrollingStartedByThisView) {
                val scrollByHuman = Math.round(lastY - e2.y) //手势产生的距离
                log { "scroll y = ${e2.y} lastY = $lastY dy = $scrollByHuman" }
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
            lastX = e2.x
            lastY = e2.y
            return true
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            log { "onFling velocity = $velocityY" }
            return onUpOrCancel(-velocityX, -velocityY)
        }

        override fun onDown(e: MotionEvent): Boolean {
            log { "onDown $e" }
            mScroller.abortAnimation()
            lastY = e.y
            lastX = e.x
            return true
        }

        fun onUpOrCancel(velX: Float = 0f, velY: Float = 0f): Boolean {
            if (isNestedScrollingStartedByThisView) {
                //根据当前速度 进行惯性滑行
                //先让parent消费
                dispatchNestedPreFling(0f, velY)
                startFling(velX, velY)
                return true
            }
            return false
        }
    }
    private val gestureDetector by lazy { GestureDetector(context, gestureHandler) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK
        if (gestureDetector.onTouchEvent(event)) {
            return true
        } else if (action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_CANCEL) {
            log { if (action == MotionEvent.ACTION_UP) "onUp" else "onCancel" }
            return gestureHandler.onUpOrCancel()
        }
        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK
        var intercept = false

        if (action != MotionEvent.ACTION_MOVE) {
            if (isNestedScrollingStartedByThisView) {
                intercept = true
            }
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                log { "onIntercept onDown" }
                mScroller.abortAnimation()
                lastY = event.y
                lastX = event.x
                downRawY = event.rawY
                downRawX = event.rawX
                isNestedScrollingStartedByThisView = false
                isNestedScrollingStartedByChild = false
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL or
                    ViewCompat.SCROLL_AXIS_HORIZONTAL)
            }
            MotionEvent.ACTION_MOVE -> {
                lastY = event.y
                lastX = event.x
                if (!isNestedScrollingStartedByChild) {
                    val dy = Math.abs(event.rawY - downRawY)
                    val dx = Math.abs(event.rawX - downRawX)
                    if (dy > mTouchSlop && dy > 2 * dx) {
                        isNestedScrollingStartedByThisView = true
                        log { "onInterceptTouchEvent requestDisallowIntercept" }
                        requestDisallowParentTouchEvent()
                        intercept = true
                    }
                }
            }
        }
        return intercept || super.onInterceptTouchEvent(event)
    }

    private fun requestDisallowParentTouchEvent() {
        parent?.requestDisallowInterceptTouchEvent(true)
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