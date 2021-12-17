package mobile.yy.com.touchsample.model

import androidx.annotation.ColorInt

/**
 * @author YvesCheung
 * 2018/4/25
 */
data class MainTab(
        val bizId: Int,
        val tabName: String,
        @ColorInt val backgroundColor: Int,
        val subTab: List<SubTab>
)

data class SubTab(
        val bizId: Int,
        val subBizId: Int,
        val tabName: String,
        var textSize: Float
)