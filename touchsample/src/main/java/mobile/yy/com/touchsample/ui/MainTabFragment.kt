package mobile.yy.com.touchsample.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_main_tab.*
import mobile.yy.com.touchsample.App
import mobile.yy.com.touchsample.R
import mobile.yy.com.touchsample.model.SubTab
import mobile.yy.com.touchsample.util.OnVisibleChangeFragment

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class MainTabFragment : OnVisibleChangeFragment() {

    companion object {
        fun newInstance(bizId: Int) = MainTabFragment().apply {
            arguments = Bundle().apply {
                putInt("bizId", bizId)
            }
        }
    }

    private var bizId = 0

    private val presenter by lazy { App.injector.getMainTabPresenter(bizId) }

    private val mainTab by lazy { presenter.getTab() }

    private val mAdapter by lazy { SubTabAdapter(childFragmentManager, mainTab.subTab) }

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bizId = arguments.getInt("bizId", bizId)
        return inflater.inflate(R.layout.fragment_main_tab, null).apply {
            setBackgroundColor(mainTab.backgroundColor)
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subViewPager.adapter = mAdapter
        subTabStrip.setViewPager(subViewPager)
    }

    override fun onFragmentVisibleChange(visible: Boolean) {
        mAdapter.onVisibleChange(visible)
    }
}

class SubTabAdapter(fm: FragmentManager, private val tabs: List<SubTab>) : FragmentPagerAdapter(fm) {

    private val fragments = Array(tabs.size) { pos ->
        SubTabFragment.newInstance(tabs[pos].subBizId)
    }

    override fun getCount() = tabs.size

    override fun getItem(position: Int) = fragments[position]

    override fun getPageTitle(position: Int) = tabs[position].tabName

    fun onVisibleChange(visible: Boolean) {
        fragments.forEach { it.setParentFragmentVisible(visible) }
    }
}