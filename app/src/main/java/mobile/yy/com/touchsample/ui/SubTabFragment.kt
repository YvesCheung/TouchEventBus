package mobile.yy.com.touchsample.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_sub_tab.*
import mobile.yy.com.touchsample.App
import mobile.yy.com.touchsample.R

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class SubTabFragment : Fragment() {

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
        subBizId = arguments.getInt("subBizId")
        return inflater.inflate(R.layout.fragment_sub_tab, null)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subTextView.text = presenter.getContent()
    }
}