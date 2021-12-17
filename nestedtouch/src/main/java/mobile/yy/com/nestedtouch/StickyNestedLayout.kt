package mobile.yy.com.nestedtouch

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.annotation.Size
import androidx.annotation.StringRes
import androidx.core.view.NestedScrollingChild2
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.TYPE_NON_TOUCH
import androidx.core.view.ViewCompat.TYPE_TOUCH
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

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
    private var navView: View? = null
    private lateinit var contentView: View

    @Suppress("LeakingThis")
    private val childHelper = NestedScrollingChildHelper(this)

    @Suppress("LeakingThis")
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

    @Size(2)
    private val tempXY = IntArray(2)

    @Size(2)
    private fun allocateXY(): IntArray {
        tempXY[0] = 0
        tempXY[1] = 0
        return tempXY
    }

    //<editor-fold desc="基础布局部分">

    override fun onFinishInflate() {
        super.onFinishInflate()

        headView = requireChildView(
            R.id.stickyHeadView, R.string.stickyHeadView, "stickyHeadView"
        )
        navView = optionalChildView(
            R.id.stickyNavView, R.string.stickyNavView, "stickyNavView"
        )
        contentView = requireChildView(
            R.id.stickyContentView, R.string.stickyContentView, "stickyContentView"
        )

        //让headView是可以收触摸事件的 dispatchTouchEvent才能处理滑动的事件
        headView.isFocusable = true
        headView.isClickable = true
    }

    private fun requireChildView(@IdRes id: Int, @StringRes strId: Int, msg: String): View {
        return optionalChildView(id, strId, msg)
            ?: throw StickyNestedLayoutException(
                "在StickyNestedLayout中必须要提供一个含有属性 android:id=\"@id/$msg\" 或者" +
                    "android:contentDescription=\"@string/$msg\" 的子View "
            )
    }

    private fun optionalChildView(@IdRes id: Int, @StringRes strId: Int, msg: String): View? {
        val viewOptional: View? = findViewById(id)
        return if (viewOptional != null) {
            viewOptional
        } else {
            val singleViewExpect = ArrayList<View>(1)
            findViewsWithText(singleViewExpect, string(strId), FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            if (singleViewExpect.size > 1) {
                throw StickyNestedLayoutException(
                    "在StickyNestedLayout中包含了多个含有属性 android:id=\"@id/$msg\" 或者" +
                        "android:contentDescription=\"@string/$msg\" 的子View，" +
                        "StickyNestedLayout无法确定应该使用哪一个"
                )
            } else {
                singleViewExpect.firstOrNull()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val wrapContent = makeMeasureSpec(0, UNSPECIFIED)
        measureChildWithMargins(headView, widthMeasureSpec, wrapContent)
        val navigationView = navView
        if (navigationView != null) {
            measureChildWithMargins(navigationView, widthMeasureSpec, wrapContent)
        }
        val expectContentHeight = makeMeasureSpec(
            measuredHeight - navViewHeight - stickyOffsetHeight,
            MeasureSpec.EXACTLY
        )
        measureChildWithMargins(contentView, widthMeasureSpec, expectContentHeight)
        setMeasuredDimension(measuredWidthAndState, measuredHeightAndState)
    }

    private fun measureChildWithMargins(
        child: View,
        parentWidthMeasureSpec: Int,
        parentHeightMeasureSpec: Int
    ) {
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
            else -> {
                scrollTo(dx, dy)
                unconsumed?.set(1, 0)
            }
        }
        scrollListeners.forEach { it.onScroll(this, scrollX, scrollY) }
    }

    private var mScroller = Scroller(context, sQuinticInterpolator)
    private var lastFlingX = 0
    private var lastFlingY = 0
    private var inStateOfFling = false

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (inStateOfFling) {  //fling
                val curY = mScroller.currY
                val curX = mScroller.currX
                var dy = curY - lastFlingX
                var dx = curX - lastFlingY
                lastFlingX = curY
                lastFlingY = curX

                val consumedByParent = allocateXY()
                if (dispatchNestedPreScroll(dx, dy, consumedByParent, null, TYPE_NON_TOUCH)) {
                    dx -= consumedByParent[0]
                    dy -= consumedByParent[1]
                }

                val consumedByUs = allocateXY()
                scrollByWithUnConsumed(0, dy, consumedByUs)
                dispatchNestedScroll(
                    0, dy - consumedByUs[1],
                    dx, consumedByUs[1], null, TYPE_NON_TOUCH
                )
            } else { //scroll
                scrollToWithUnConsumed(mScroller.currX, mScroller.currY, null)
            }

            if (mScroller.isFinished) {
                abortScrollerAnimation()
            } else {
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    private fun abortScrollerAnimation() {
        mScroller.abortAnimation()
        if (inStateOfFling) {
            stopNestedScroll(TYPE_NON_TOUCH, "abortScrollerAnimation")
            inStateOfFling = false
        }
        isNestedScrollingStartedByChild = false
        isNestedScrollingStartedByThisView = false
    }

    private fun fling(vx: Float, vy: Float) {
        log { "startFling velocityY = $vy" }
        mScroller.fling(
            0, 0, vx.roundToInt(), vy.roundToInt(),
            Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE
        )
        lastFlingX = 0
        lastFlingY = 0
        inStateOfFling = true
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, TYPE_NON_TOUCH, "fling")
        ViewCompat.postInvalidateOnAnimation(this)
    }

    //</editor-fold" desc="基础滚动能力部分">

    //<editor-fold desc="嵌套滑动部分">

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun hasNestedScrollingParent() = childHelper.hasNestedScrollingParent()

    override fun hasNestedScrollingParent(type: Int) = childHelper.hasNestedScrollingParent(type)

    override fun isNestedScrollingEnabled() = childHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int) =
        startNestedScroll(axes, TYPE_TOUCH, "callStartNestedScroll")

    override fun startNestedScroll(axes: Int, type: Int): Boolean =
        startNestedScroll(axes, type, "callStartNestedScroll(type)")

    private fun startNestedScroll(axes: Int, type: Int, reason: String): Boolean {
        log { "startNestedScroll $type by $reason" }
        return childHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll() = stopNestedScroll(TYPE_TOUCH, "CallStopNestedScroll")

    override fun stopNestedScroll(type: Int) =
        stopNestedScroll(TYPE_TOUCH, "CallStopNestedScroll(type)")

    private fun stopNestedScroll(type: Int, reason: String) {
        log { "stopNestedScroll $type by $reason" }
        childHelper.stopNestedScroll(type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?, type: Int
    ) = childHelper.dispatchNestedScroll(
        dxConsumed, dyConsumed, dxUnconsumed,
        dyUnconsumed, offsetInWindow, type
    )

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ) = childHelper.dispatchNestedScroll(
        dxConsumed, dyConsumed,
        dxUnconsumed, dyUnconsumed, offsetInWindow
    )

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?, type: Int
    ) = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?
    ) = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

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

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean =
        onStartNestedScroll(child, target, nestedScrollAxes, TYPE_TOUCH)

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        onNestedPreScroll(target, dx, dy, consumed, TYPE_TOUCH)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int
    ) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, TYPE_TOUCH)
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean =
        dispatchNestedPreFling(velocityX, velocityY)

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean = dispatchNestedFling(velocityX, velocityY, consumed)

    /**
     * 记录当前开始中的嵌套滑动类型
     * @see TYPE_TOUCH
     * @see TYPE_NON_TOUCH
     */
    private val nestedScrollingType = mutableSetOf<Int>()

    //child告诉我要开始嵌套滑动
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        log { "onStartNestedScroll $type" }
        if (axes and SCROLL_AXIS_VERTICAL != 0) { //只响应垂直方向的滑动
            nestedScrollingType.add(type)
            isNestedScrollingStartedByThisView = false
            isNestedScrollingStartedByChild = true
            //开始通知parent的嵌套滑动
            startNestedScroll(
                axes,
                type,
                "onStartNestedScroll"
            )
            return true
        }
        return false
    }

    //child告诉我要停止嵌套滑动
    override fun onStopNestedScroll(target: View, type: Int) {
        log { "onStopNestedScroll $target, type = $type" }
        nestedScrollingType.remove(type)
        if (nestedScrollingType.isEmpty()) {
            isNestedScrollingStartedByThisView = false
            isNestedScrollingStartedByChild = false
        }
        stopNestedScroll(type, "onStopNestedScroll") //结束parent的嵌套滑动
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (isNestedScrollingStartedByChild) {
            log { "onNestedPreScroll dy = $dy, type = $type" }
            //dy > 0 上滑时处理
            val consumedByParent = allocateXY()
            dispatchNestedPreScroll(dx, dy, consumedByParent, null) //先分给parent搞事

            val leftY = dy - consumedByParent[1] //parent留给我的
            val headViewScrollDis = headViewHeight - scrollY - stickyOffsetHeight
            val headViewCanBeExpand = leftY > 0 && headViewScrollDis > 0 //上滑且headView能向上滚

            consumed?.set(0, consumedByParent[0]) //x方向全是parent吃的
            if (headViewCanBeExpand) {
                if (leftY > headViewScrollDis) { //滑的距离超过了能滚的距离
                    scrollByWithUnConsumed(0, headViewScrollDis)
                    consumed?.set(1, headViewScrollDis + consumedByParent[1]) //只消费能滚的最大距离
                } else {
                    scrollByWithUnConsumed(0, leftY) //没超过滚的极限距离，那就滑多少滚多少
                    consumed?.set(1, dy) //把parent吃剩的全吃了 (parentConsumed[1] + leftY)
                }
            } else { //headView不能滑了 全是parent吃的
                consumed?.set(1, consumedByParent[1])
            }
        }
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, type: Int
    ) {
        if (isNestedScrollingStartedByChild) {
            log { "onNestedScroll dyConsumed = $dyConsumed, dyUnconsumed = $dyUnconsumed, type = $type" }
            //dy < 0 下滑时处理
            var dyUnconsumedAfterMe = dyUnconsumed
            var dyConsumedAfterMe = dyConsumed
            val headViewScrollDis = scrollY

            if (dyUnconsumed < 0 && headViewScrollDis >= 0) { //下滑而且headView能向下滚
                if (headViewScrollDis < abs(dyUnconsumed)) { //滑动距离超过了可以滚的范围
                    scrollByWithUnConsumed(0, -headViewScrollDis) //只滚我能滚的
                    dyUnconsumedAfterMe = dyUnconsumed + headViewScrollDis //只消费我能滑的
                    dyConsumedAfterMe = dyConsumed - headViewScrollDis
                } else { //全部都能消费掉
                    scrollByWithUnConsumed(0, dyUnconsumed)
                    dyUnconsumedAfterMe = 0
                    dyConsumedAfterMe = dyConsumed + dyUnconsumed
                }
            }

            dispatchNestedScroll(
                0, dyConsumedAfterMe, 0,
                dyUnconsumedAfterMe, null
            )
        }
    }

    //</editor-fold desc="嵌套滑动部分">


    //<editor-fold desc="自身手势触摸处理部分">

    private var lastX = 0f
    private var lastY = 0f
    private var downRawY = 0f
    private var downRawX = 0f

    private val gestureHandler = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
        ): Boolean {
            if (isNestedScrollingStartedByThisView) {
                val scrollByHuman = (lastY - e2.y).roundToInt() //手势产生的距离
                log { "scroll y = ${e2.y} lastY = $lastY dy = $scrollByHuman" }
                //先给parent消费
                val consumedByParent = allocateXY()
                dispatchNestedPreScroll(0, scrollByHuman, consumedByParent, null)
                val scrollAfterParent = scrollByHuman - consumedByParent[1] //parent吃剩的
                val unconsumed = allocateXY()
                scrollByWithUnConsumed(0, scrollAfterParent, unconsumed) //自己滑
                val consumeY = scrollByHuman - unconsumed[1]
                //滑剩的再给一次parent
                dispatchNestedScroll(0, consumeY, 0, unconsumed[1], null)
            }
            lastX = e2.x
            lastY = e2.y
            return true
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            log { "onFling velocity = $velocityY" }
            val vx = -velocityX
            val vy = -velocityY
            if (isNestedScrollingStartedByThisView) {
                //根据当前速度 进行惯性滑行
                //先让parent消费
                if (!dispatchNestedPreFling(vx, vy)) {
                    dispatchNestedFling(vx, vy, true)

                    fling(vx, vy)
                }
                stopNestedScroll(TYPE_TOUCH, "onFling")
                return true
            }
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            log { "onDown $e" }
            abortScrollerAnimation()
            lastY = e.y
            lastX = e.x
            if (e.x.toInt() in headView.left..headView.right &&
                e.y.toInt() in headView.top..headView.bottom
            ) {
                isNestedScrollingStartedByThisView = true
                isNestedScrollingStartedByChild = false
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, TYPE_TOUCH, "onDown")
            }
            return true
        }
    }
    private val gestureDetector by lazy { GestureDetector(context, gestureHandler) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK
        if (gestureDetector.onTouchEvent(event)) {
            return true
        } else if (action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_CANCEL
        ) {
            log { if (action == MotionEvent.ACTION_UP) "onUp" else "onCancel" }
            if (isNestedScrollingStartedByThisView) {
                stopNestedScroll(
                    TYPE_TOUCH,
                    if (action == MotionEvent.ACTION_UP) "onUp" else "onCancel"
                )
                return true
            }
            return false
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
                abortScrollerAnimation()
                lastY = event.y
                lastX = event.x
                downRawY = event.rawY
                downRawX = event.rawX
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, TYPE_TOUCH, "onInterceptDown")
            }
            MotionEvent.ACTION_MOVE -> {
                lastY = event.y
                lastX = event.x
                if (!isNestedScrollingStartedByChild) {
                    val dy = abs(event.rawY - downRawY)
                    val dx = abs(event.rawX - downRawX)
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
        val toY = navView?.y ?: contentView.y
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
        get() = min(field, headViewHeight)

    /**
     * 获取头部区域的高度
     */
    val headViewHeight: Int get() = headView.measuredHeight

    /**
     * 获取导航栏条的高度
     */
    val navViewHeight: Int get() = navView?.measuredHeight ?: 0

    /**
     * 获取下部区域的高度
     */
    val contentViewHeight: Int get() = contentView.measuredHeight

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