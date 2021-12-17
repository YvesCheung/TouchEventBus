package mobile.yy.com.touchsample.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_sub_tab.*
import mobile.yy.com.toucheventbus.TouchEventBus
import mobile.yy.com.touchsample.App
import mobile.yy.com.touchsample.R
import mobile.yy.com.touchsample.touch.ZoomTextTouchHandler
import mobile.yy.com.touchsample.util.OnVisibleChangeFragment

/**
 * @author YvesCheung
 * 2018/4/25
 */
class SubTabFragment : OnVisibleChangeFragment(), ZoomUi {

    companion object {
        fun newInstance(subBizId: Int) = SubTabFragment().apply {
            arguments = Bundle().apply {
                putInt("subBizId", subBizId)
            }
        }
    }

    private var subBizId = 0

    private val presenter by lazy { App.injector.getSubTabPresenter(subBizId) }

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        subBizId = arguments?.getInt("subBizId") ?: 0
        return inflater.inflate(R.layout.fragment_sub_tab, null)
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subTextView.text = presenter.getContent()
        presenter.getTextSize().subscribe { textSize ->
            subTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        }
    }

    override fun onFragmentVisibleChange(visible: Boolean) {
        if (visible) {
            TouchEventBus.of(ZoomTextTouchHandler::class.java).attach(this)
        } else {
            TouchEventBus.of(ZoomTextTouchHandler::class.java).dettach(this)
        }
    }

    override fun resize(percentage: Float) {
        val textSize = subTextView.textSize
        presenter.setTextSize((percentage + 1f) * textSize)
    }
}