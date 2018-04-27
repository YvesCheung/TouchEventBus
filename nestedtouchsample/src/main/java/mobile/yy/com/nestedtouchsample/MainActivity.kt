package mobile.yy.com.nestedtouchsample

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import com.scwang.smartrefresh.layout.header.ClassicsHeader
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        refreshLayout.setRefreshHeader(ClassicsHeader(this))
        refreshLayout.setEnableNestedScroll(true)
        stickyContentView.adapter = MainAdapter(supportFragmentManager)
        stickyNavView.setViewPager(stickyContentView)
    }
}

class MainAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int) = DetailFragment()

    override fun getCount() = 3

    override fun getPageTitle(position: Int) = when (position) {
        0 -> "转发"
        1 -> "评论"
        else -> "点赞"
    }
}
