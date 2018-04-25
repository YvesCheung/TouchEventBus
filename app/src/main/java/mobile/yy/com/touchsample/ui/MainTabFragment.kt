package mobile.yy.com.touchsample.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_main_tab.*
import mobile.yy.com.touchsample.App
import mobile.yy.com.touchsample.R
import mobile.yy.com.touchsample.model.SubTab

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class MainTabFragment : Fragment() {

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

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bizId = arguments.getInt("bizId", bizId)
        return inflater.inflate(R.layout.fragment_main_tab, null).apply {
            setBackgroundColor(mainTab.backgroundColor)
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subViewPager.adapter = SubTabAdapter(childFragmentManager, mainTab.subTab)
        subTabStrip.setViewPager(subViewPager)
    }
}

class SubTabAdapter(fm: FragmentManager, private val tabs: List<SubTab>) : FragmentPagerAdapter(fm) {
    override fun getCount() = tabs.size

    override fun getItem(position: Int) = SubTabFragment.newInstance(tabs[position].subBizId)

    override fun getPageTitle(position: Int) = tabs[position].tabName
}