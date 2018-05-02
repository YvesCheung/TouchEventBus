# 滑动冲突解决方案

---
# [非嵌套滑动][1] | 嵌套滑动

> 相比起非嵌套滑动的自定义分发事件的方案，嵌套滑动冲突有比较成熟的 Google 解决方案：**[NestedScrolling][2]** 。

## 三层嵌套的滑动冲突

![嵌套滑动][3]

UI 层级如下：

*  最外层（底层）是一个具有下拉刷新功能的布局
*  中层是本库提供的控件 **``StickNestedLayout``** ，解决导航栏吸顶，以及内外层的滑动冲突
*  最内层（上层）依次是 *headView* / *navView* / *contentView* ，对应上部的内容区域，中部的吸顶导航栏区域，下部的 ``ViewPager`` 区域
*  ``ViewPager`` 里面有 ``RecyclerView`` 列表

![StickNestedLayout][4]

## 使用

你可以通过 ``StickNestedLayout`` 轻松地完成这种页面。

```XML
<RefreshLayout
    android:id="@+id/refreshLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <mobile.yy.com.nestedtouch.StickyNestedLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <include
            android:id="@id/stickyHeadView"
            layout="@layout/moment_head_view" />

        <NavView
            android:id="@id/stickyNavView"
            android:layout_width="match_parent"
            android:layout_height="40dp" />

        <android.support.v4.view.ViewPager
            android:id="@id/stickyContentView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
    </mobile.yy.com.nestedtouch.StickyNestedLayout>
</RefreshLayout>
```

> 其中 *headView* / *navView* / *contentView* 的id必须为 **stickyHeadView** / **stickyNavView** / **stickyContentView** 

可以通过运行工程 *nestedtouchsample* 查看具体代码。例子中涉及的其他第三方库有下拉刷新控件 [SmartRefreshLayout][5] 和导航栏 [PagerSlidingTabStrip][6] ，部分参考 [StickNavLayout][7]

## 配置

1. 项目build.gradle添加

    ```Groovy
    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
    ```
2. 对应模块添加依赖

    ```Groovy
    dependencies {
        compile 'com.github.YvesCheung.TouchEventBus:nestedtouch:1.4.3'
    }
    ```
    
## 许可证

    Copyright 2018 YvesCheung

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


  [1]: https://github.com/YvesCheung/TouchEventBus/blob/master/README.md
  [2]: https://developer.android.com/reference/android/support/v4/view/NestedScrollingParent
  [3]: https://raw.githubusercontent.com/YvesCheung/TouchEventBus/master/img/nestedScrollPreview.gif
  [4]: https://raw.githubusercontent.com/YvesCheung/TouchEventBus/master/img/stickNestedLayout.png
  [5]: https://github.com/scwang90/SmartRefreshLayout
  [6]: https://github.com/ta893115871/PagerSlidingTabStrip
  [7]: https://github.com/hongyangAndroid/Android-StickyNavLayout
