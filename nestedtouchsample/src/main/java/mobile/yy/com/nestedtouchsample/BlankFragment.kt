package mobile.yy.com.nestedtouchsample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


/**
 * Created by 张宇 on 2018/5/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class BlankFragment : Fragment() {

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_blank, null)
    }
}