package com.github.wangyiqian.stockchart.util

import java.text.SimpleDateFormat
import java.util.*

object StockTradingTimeUtil {

    // 定义一个时间段数据类，start 和 end 直接使用时间戳（单位：毫秒）
    data class TradingPeriod(val start: Long, val end: Long)

    // 获取指定日期的交易时间戳列表，支持多个时间段
    fun getTradingTimestampsForDate(date: String): List<Long> {
        // 定义固定的交易时间段（A股的标准交易时间段）
        val periods = listOf(
            // 上午：9:30 - 11:30
            TradingPeriod(getTimestamp(date, "09:30:00"), getTimestamp(date, "11:30:00")),
            // 下午：13:00 - 15:00
            TradingPeriod(getTimestamp(date, "13:00:00"), getTimestamp(date, "15:00:00"))
        )

        return generateTimestamps(periods)
    }

    // 根据日期和时间生成时间戳（单位：毫秒）
    private fun getTimestamp(date: String, time: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateTimeString = "$date $time"
        return sdf.parse(dateTimeString)?.time ?: 0L
    }

    // 生成时间戳列表（每分钟一个时间戳）
    private fun generateTimestamps(periods: List<TradingPeriod>): List<Long> {
        val tradingTimes = mutableListOf<Long>()

        periods.forEach { period ->
            var currentTime = period.start
            while (currentTime <= period.end) {
                tradingTimes.add(currentTime)
                currentTime += 60 * 1000 // 增加1分钟
            }
        }

        return tradingTimes
    }
}
