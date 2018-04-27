package mobile.yy.com.touchsample.util

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v4.app.Fragment
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/**
 * Created by 张宇 on 2018/2/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 *
 * 判断 [android.support.v4.app.Fragment] 可见性的辅助工具。
 * 使用时请重写 [android.support.v4.app.Fragment] 的以下几个方法：
 * [android.support.v4.app.Fragment.onCreate]
 * [android.support.v4.app.Fragment.onStart]，
 * [android.support.v4.app.Fragment.onStop]，
 * [android.support.v4.app.Fragment.setUserVisibleHint]，
 * 并在这些方法中调用:
 * [EnterFragmentHelper.onCreate]
 * [EnterFragmentHelper.onStart]
 * [EnterFragmentHelper.onStop]
 * [EnterFragmentHelper.userVisibleHint]。
 * 如果有嵌套的情况，比如首页的多个一级Tab嵌套多个二级Tab，那么还需要提供父Fragment的可见性。
 * 在父Fragment可见性发生变化时调用[EnterFragmentHelper.onParentFragmentVisibleChange]方法。
 *
 * @see OnVisibleChangeFragment
 */
class EnterFragmentHelper(
        /**
         * 当前Fragment可见性改变时的回调
         */
        private val onVisibleChange: (visible: Boolean) -> Unit,
        /**
         * 当前Fragment在应用打开后，第一次可见时的回调
         */
        private val onFirstVisible: () -> Unit
) {

    private var parentFragmentVisible = false

    companion object {
        private var firstTime = true
    }

    private var fragmentVisible by VisibleCheck()

    /**
     * 当前Fragment是否可见
     */
    var isThisFragmentVisible: Boolean = false
        private set(value) {
            field = value
        }

    var userVisibleHint by Delegates.observable(false) { _, _, newValue ->
        fragmentVisible = newValue
    }

    fun onParentFragmentVisibleChange(visible: Boolean) {
        parentFragmentVisible = visible
        fragmentVisible = visible
    }

    fun onCreate(parentFragment: Fragment?) {
        if (parentFragment == null) {
            parentFragmentVisible = true
        }
    }

    fun onStop() {
        fragmentVisible = false
    }

    fun onStart() {
        fragmentVisible = true
    }

    private inner class VisibleCheck : ObservableProperty<Boolean>(initialValue = false) {
        //false 就不要新值
        override fun beforeChange(property: KProperty<*>, oldValue: Boolean, newValue: Boolean): Boolean {
            if (newValue) {
                if (!userVisibleHint || !parentFragmentVisible) {
                    return false
                }
            }
            return true
        }

        override fun afterChange(property: KProperty<*>, oldValue: Boolean, newValue: Boolean) {
            if (oldValue != newValue) {
                isThisFragmentVisible = newValue
                onVisibleChange(newValue)

                if (newValue && firstTime) {
                    firstTime = false
                    onFirstVisible()
                }
            }
        }
    }
}

/**
 * Fragment判断是否第一次可见。
 * 可以继承并重写[onFragmentFirstVisible]和[onFragmentFirstVisible]来获取可见性改变的回调。
 * 如果当前Fragment是嵌套Fragment，比如首页一级Tab里面的二级Tab，则需要提供父Fragment的可见性变化，
 * 在父Fragment可见性变化时主动调用[setParentFragmentVisible]方法。
 */
abstract class OnVisibleChangeFragment : Fragment() {

    private val helper = EnterFragmentHelper(::onFragmentVisibleChange, ::onFragmentFirstVisible)

    protected val isThisFragmentVisible get() = helper.isThisFragmentVisible

    @CallSuper
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        helper.userVisibleHint = isVisibleToUser
    }

    /**
     * 在父Fragment可见性发生变化时请调用这个方法。
     */
    fun setParentFragmentVisible(isParentVisible: Boolean) {
        helper.onParentFragmentVisibleChange(isParentVisible)
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper.onCreate(parentFragment)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        helper.onStart()
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        helper.onStop()
    }

    /**
     * 当前Fragment在onCreate之后可见性发生变化时
     */
    open fun onFragmentVisibleChange(visible: Boolean) {}

    /**
     * 当前Fragment在onCreate之后首次可见时
     */
    open fun onFragmentFirstVisible() {}
}