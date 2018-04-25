package mobile.yy.com.touchsample.ui

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import kotlinx.android.synthetic.main.activity_main.*
import mobile.yy.com.toucheventbus.touchBus.TouchEventBus
import mobile.yy.com.touchsample.App
import mobile.yy.com.touchsample.R
import mobile.yy.com.touchsample.model.MainTab
import mobile.yy.com.touchsample.touch.TabTouchHandler

class MainActivity : AppCompatActivity() {

    private val presenter by lazy { App.injector.getMainPagePresenter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter.getBizTab().subscribe { tabs ->
            mainViewPager.adapter = MainPagerAdapter(supportFragmentManager, tabs)
            mainPagerTabStrip.setViewPager(mainViewPager)
        }

        TouchEventBus.of(TabTouchHandler::class.java).attach(mainViewPager)
    }

    override fun onDestroy() {
        TouchEventBus.of(TabTouchHandler::class.java).dettach(mainViewPager)
        super.onDestroy()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        TouchEventBus.dispatchTouchEvent(ev, mainContainer)
        return true
    }
}

private class MainPagerAdapter(fm: FragmentManager, val tabs: List<MainTab>) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int) = MainTabFragment.newInstance(tabs[position].bizId)

    override fun getCount() = tabs.size

    override fun getPageTitle(position: Int) = tabs[position].tabName
}
