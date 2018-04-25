package mobile.yy.com.touchsample.model

import android.support.annotation.ColorInt

/**
 * Created by 张宇 on 2018/4/25.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
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
        val tabName: String
)