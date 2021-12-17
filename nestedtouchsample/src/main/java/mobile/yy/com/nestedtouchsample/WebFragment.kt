package mobile.yy.com.nestedtouchsample

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.view.MotionEventCompat
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.max


/**
 * @author YvesCheung
 * 2021/1/15
 */
class WebFragment : Fragment() {

    private lateinit var webView: WebView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return NestedScrollWebView(inflater.context).also { webView = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView.settings.apply {
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadsImagesAutomatically = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }
        webView.loadUrl("https://www.yy.com/")
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        webView.destroy()
        super.onDestroyView()
    }

    /**
     * Copy from [https://github.com/tobiasrohloff/NestedScrollWebView/blob/master/lib/src/main/java/com/tobiasrohloff/view/NestedScrollWebView.java]
     */
    private class NestedScrollWebView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : WebView(context, attrs, defStyleAttr), NestedScrollingChild {

        private var mLastMotionY = 0
        private val mScrollOffset = IntArray(2)
        private val mScrollConsumed = IntArray(2)
        private var mNestedYOffset = 0
        private var mChildHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)

        init {
            isNestedScrollingEnabled = true
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            var result = false
            val trackedEvent = MotionEvent.obtain(event)
            val action = MotionEventCompat.getActionMasked(event)
            if (action == MotionEvent.ACTION_DOWN) {
                mNestedYOffset = 0
            }
            val y = event.y.toInt()
            event.offsetLocation(0f, mNestedYOffset.toFloat())
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    mLastMotionY = y
                    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                    result = super.onTouchEvent(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    var deltaY = mLastMotionY - y
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                        deltaY -= mScrollConsumed[1]
                        trackedEvent.offsetLocation(0f, mScrollOffset[1].toFloat())
                        mNestedYOffset += mScrollOffset[1]
                    }
                    mLastMotionY = y - mScrollOffset[1]
                    val oldY = scrollY
                    val newScrollY = max(0, oldY + deltaY)
                    val dyConsumed = newScrollY - oldY
                    val dyUnconsumed = deltaY - dyConsumed
                    if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mScrollOffset)) {
                        mLastMotionY -= mScrollOffset[1]
                        trackedEvent.offsetLocation(0f, mScrollOffset[1].toFloat())
                        mNestedYOffset += mScrollOffset[1]
                    }
                    result = super.onTouchEvent(trackedEvent)
                    trackedEvent.recycle()
                }
                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopNestedScroll()
                    result = super.onTouchEvent(event)
                }
            }
            return result
        }

        override fun setNestedScrollingEnabled(enabled: Boolean) {
            mChildHelper.isNestedScrollingEnabled = enabled
        }

        override fun isNestedScrollingEnabled(): Boolean {
            return mChildHelper.isNestedScrollingEnabled
        }

        override fun startNestedScroll(axes: Int): Boolean {
            return mChildHelper.startNestedScroll(axes)
        }

        override fun stopNestedScroll() {
            mChildHelper.stopNestedScroll()
        }

        override fun hasNestedScrollingParent(): Boolean {
            return mChildHelper.hasNestedScrollingParent()
        }

        override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
            return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
        }

        override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
            return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
        }

        override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
            return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
        }

        override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
            return mChildHelper.dispatchNestedPreFling(velocityX, velocityY)
        }
    }
}