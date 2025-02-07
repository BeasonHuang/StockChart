package com.github.wangyiqian.stockchart.sample.sample4

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.github.wangyiqian.stockchart.sample.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class Sample4Activity : AppCompatActivity() {
    // recyclerView中的item数据源
    private val itemData: MutableList<String> = ArrayList()

    // tabLayout的标题
    private val tabTitle =
        arrayOf("标题1", "标题2", "标题3", "标题4", "标题5", "标题6", "标题7", "标题8", "标题9")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample4)
        initItemData()

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        // 设置横向滑动
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE

        val viewPager2 = findViewById<ViewPager2>(R.id.viewPager2)
        viewPager2.adapter = MyViewPager2Adapter(itemData, tabTitle, this)
        val tabLayoutMediator = TabLayoutMediator(
            tabLayout, viewPager2
        ) { tab, position -> // 设置tabLayout的标题
            tab.setText(tabTitle[position])
        }
        // 应用生效
        tabLayoutMediator.attach()
    }

    /**
     * 初始化recyclerView中的item数据源
     */
    private fun initItemData() {
        for (i in 0..99) {
            itemData.add(i.toString() + "")
        }
    }
}