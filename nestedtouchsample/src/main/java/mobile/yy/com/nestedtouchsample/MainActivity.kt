package mobile.yy.com.nestedtouchsample

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewTreeObserver
import android.widget.Toast
import com.scwang.smartrefresh.layout.header.ClassicsHeader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.moment_head_view.*
import mobile.yy.com.nestedtouch.StickyNestedLayout

class MainActivity : AppCompatActivity() {

    private val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            titleBar.viewTreeObserver.removeGlobalOnLayoutListener(this)
            stickyNestedLayout.stickyOffsetHeight = titleBar.height

            val headViewHeight = (stickyNestedLayout.headViewHeight - titleBar.height).toFloat()
            stickyNestedLayout.addOnScrollListener(object : StickyNestedLayout.OnScrollListener {
                override fun onScroll(view: StickyNestedLayout, scrollX: Int, scrollY: Int) {
                    titleBar.alpha = 1f - (headViewHeight - scrollY) / headViewHeight
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        refreshLayout.setRefreshHeader(ClassicsHeader(this))
        refreshLayout.setEnableNestedScroll(true)
        refreshLayout.isEnableLoadMore = false

        titleBar.alpha = 0f
        titleBar.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        contentView.adapter = MainAdapter(supportFragmentManager)

        navView.setViewPager(contentView)

        momentItemQrCode.setOnClickListener {
            Toast.makeText(this, "click qr code", Toast.LENGTH_SHORT).show()
        }
    }
}

class MainAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int) =
        when (position) {
            0 -> DetailFragment()
            1 -> WebFragment()
            else -> BlankFragment()
        }

    override fun getCount() = 3

    override fun getPageTitle(position: Int) = when (position) {
        0 -> "转发"
        1 -> "评论"
        else -> "点赞"
    }
}
