package mobile.yy.com.touchsample.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import mobile.yy.com.toucheventbus.TouchEventBus
import mobile.yy.com.touchsample.R
import mobile.yy.com.touchsample.touch.BackgroundImageTouchHandler

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class BackgroundFragment : Fragment() {

    private lateinit var viewPager: ViewPager

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ViewPager(context).also { viewPager = it }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager.adapter = BackgroundAdapter(context)
        TouchEventBus.of(BackgroundImageTouchHandler::class.java).attach(viewPager)
    }

    override fun onDestroyView() {
        TouchEventBus.of(BackgroundImageTouchHandler::class.java).dettach(viewPager)
        super.onDestroyView()
    }
}

class BackgroundAdapter(private val context: Context) : PagerAdapter() {

    private val gallery = Array(3) { idx ->
        ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(when (idx) {
                0 -> R.drawable.main_background
                1 -> R.drawable.main_background2
                else -> R.drawable.main_background3
            })
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imageView = gallery[position]
        if (imageView.parent == null) {
            container.addView(imageView)
        }
        return imageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, imageView: Any?) {
        if (imageView is View) {
            container.removeView(imageView)
        }
    }

    override fun isViewFromObject(view: View?, imageView: Any?) = view == imageView

    override fun getCount() = gallery.size
}