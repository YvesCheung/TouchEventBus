package mobile.yy.com.nestedtouchsample

import android.content.res.Resources
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.scwang.smartrefresh.layout.footer.ClassicsFooter

/**
 * Created by 张宇 on 2018/4/27.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class DetailFragment : Fragment() {

    private val randomNumber get() = Array(30) { idx -> "${idx}000000" }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = DetailAdapter(randomNumber.toList())
        }
        return SmartRefreshLayout(context).apply {
            isEnableRefresh = false
            setRefreshFooter(ClassicsFooter(context))
            setEnableNestedScroll(true)
            addView(recyclerView)
        }
    }
}

class DetailAdapter(private val list: List<String>) : RecyclerView.Adapter<DetailAdapter.DetailViewHolder>() {

    private val Number.dp2px
        get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
            Resources.getSystem().displayMetrics).toInt()

    private val padding = 20.dp2px

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        return DetailViewHolder(TextView(parent.context).apply {
            setPadding(padding, padding, padding, padding)
        })
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.v.text = list[position]
    }

    class DetailViewHolder(val v: TextView) : RecyclerView.ViewHolder(v)
}