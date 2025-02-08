package com.github.wangyiqian.stockchart.sample

import android.os.Handler
import android.os.Looper
import java.util.*
import kotlin.concurrent.timer
import kotlin.random.Random

// 定义一个工具类
object StockTimer {

    private var timer: Timer? = null
    private var handler: Handler = Handler(Looper.getMainLooper())

    // 定时发送事件的接口
    interface StockTimeListener {
        fun onTick(time: Long)
    }

    // 启动定时任务
    fun startStockTimer(date: String, listener: StockTimeListener) {
        // 获取交易时间段
        val tradingTimes = getTradingTimestamps(date)
        var currentIndex = 0

        // 使用 Timer 定时任务每秒钟发送事件
        timer = timer(period = 1000) {
            if (currentIndex < tradingTimes.size) {
                val currentTime = tradingTimes[currentIndex]
                handler.post {
                    // 发送事件
                    listener.onTick(currentTime)
                }
                currentIndex++
            } else {
                // 任务结束后取消定时器
                this.cancel()
            }
        }
    }

    // 停止定时任务
    fun stopStockTimer() {
        timer?.cancel()
    }

    // 获取 A 股的交易时间戳，每分钟一条数据
    private fun getTradingTimestamps(date: String): List<Long> {
        val periods = listOf(
            Pair("09:30", "11:30"),
            Pair("13:00", "15:00")
        )
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timestamps = mutableListOf<Long>()

        for ((start, end) in periods) {
            val startTime = sdf.parse("$date $start")!!.time
            val endTime = sdf.parse("$date $end")!!.time

            var currentTime = startTime
            while (currentTime <= endTime) {
                timestamps.add(currentTime)
                currentTime += 60 * 1000 // 每分钟增加
            }
        }
        return timestamps
    }
}