/*
 * Copyright 2021 WangYiqian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */

package com.github.wangyiqian.stockchart.sample

import android.content.Context
import com.github.wangyiqian.stockchart.entities.FLAG_DEFAULT
import com.github.wangyiqian.stockchart.entities.FLAG_EMPTY
import com.github.wangyiqian.stockchart.entities.FLAG_LINE_STARTER
import com.github.wangyiqian.stockchart.entities.IKEntity
import com.github.wangyiqian.stockchart.entities.KEntity
import com.github.wangyiqian.stockchart.sample.sample3.data.ActiveChartKEntity
import com.github.wangyiqian.stockchart.sample.sample3.data.ActiveInfo
import com.github.wangyiqian.stockchart.sample.sample3.data.ActiveResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * 模拟加载数据
 *
 * @author wangyiqian E-mail: wangyiqian9891@gmail.com
 * @version 创建时间: 2021/1/29
 */
object DataMock {

    const val basePrice = 100f  // 假设昨日收盘价为100
    const val date = "2025-02-07"

    private const val MOCK_DELAY = 0L // 模拟耗时

    fun loadDayTimeData(context: Context, callback: (List<IKEntity>) -> Unit) {
        MainScope().launch {
            delay(MOCK_DELAY)

            val list = StockDataGenerator.generateDailyStockData(basePrice, date)
//            val list = loadDataFromTimeDataAsserts(context, "mock_time_data_day.txt")
//            val stockList: List<IKEntity> = List(10) {
//                KEntity(0f, 0f, 0f, 0f, 0, 0, 0f, FLAG_EMPTY)
//            }
//            list.addAll(stockList)
            callback.invoke(list)
        }
    }

    fun loadDayData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {

        if (page > 2 || page < 0) {
            callback.invoke(listOf())
            return
        }

        loadData(context, "mock_data_day_page_$page.txt", callback)
    }

    fun loadFiveDayData(context: Context, callback: (List<IKEntity>) -> Unit) {
        MainScope().launch {
            delay(MOCK_DELAY)
            val result = loadDataFromTimeDataAsserts(context, "mock_time_data_five_day.txt")
            val dateFormat = SimpleDateFormat("MM/dd")
            val date = Date()
            var dateStr = ""
            result.forEach { kEntity ->
                date.time = kEntity.getTime()
                val formatDate = dateFormat.format(date)

                if (formatDate != dateStr) {
                    dateStr = formatDate
                    kEntity.setFlag(FLAG_LINE_STARTER)
                }
            }
            callback.invoke(result)
        }
    }

    fun loadWeekData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 1 || page < 0) {
            callback.invoke(listOf())
            return
        }

        loadData(context, "mock_data_week_page_$page.txt", callback)
    }

    fun loadMonthData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 0 || page < 0) {
            callback.invoke(listOf())
            return
        }

        loadData(context, "mock_data_month_page_$page.txt", callback)
    }

    fun loadQuarterData(context: Context, callback: (List<IKEntity>) -> Unit) {
        loadData(context, "mock_data_quarter.txt", callback)
    }

    fun loadYearData(context: Context, callback: (List<IKEntity>) -> Unit) {
        loadData(context, "mock_data_year.txt", callback)
    }

    fun loadFiveYearData(context: Context, callback: (List<IKEntity>) -> Unit) {
        loadData(context, "mock_data_five_year.txt", callback)
    }

    fun loadYTDData(context: Context, callback: (List<IKEntity>) -> Unit) {
        MainScope().launch {
            delay(MOCK_DELAY)
            val result = loadDataFromAsserts(context, "mock_data_ytd.txt")
            if (result.isNotEmpty()) {
                val time = result[0].getTime()
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = time
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                val maxDaysOfYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                val weekCount = (maxDaysOfYear - dayOfYear) / 7 + 1
                for (i in 0 until (weekCount - result.size)) {
                    result.add(KEntity.obtainEmptyKEntity()) // 一年内还未产生的数据用EmptyKEntity()填充
                }
            }
            callback.invoke(result)
        }
    }

    fun loadOneMinuteData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 4 || page < 0) {
            callback.invoke(listOf())
            return
        }

        loadData(context, "mock_data_one_minute_page_$page.txt", callback)
    }

    fun loadFiveMinutesData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 3 || page < 0) {
            callback.invoke(listOf())
            return
        }

        loadData(context, "mock_data_five_minutes_page_$page.txt", callback)
    }

    fun loadSixtyMinutesData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 3 || page < 0) {
            callback.invoke(listOf())
            return
        }

        loadData(context, "mock_data_sixty_minutes_page_$page.txt", callback)
    }

    private fun loadData(
        context: Context,
        assertsFileName: String,
        callback: (List<IKEntity>) -> Unit
    ) {
        MainScope().launch {
            delay(MOCK_DELAY)
            val list = loadDataFromAsserts(context, assertsFileName)
            val stockList: List<IKEntity> = List(10) {
                KEntity(0f, 0f, 0f, 0f, 0, 0, 0f, FLAG_EMPTY)
            }
            list.addAll(stockList)
            callback.invoke(list)
        }
    }

    private suspend fun loadDataFromAsserts(
        context: Context,
        fileName: String
    ): MutableList<IKEntity> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<IKEntity>()
            context.assets.open(fileName).use { inputStream ->
                var buffer = ByteArray(inputStream.available())
                inputStream.read(buffer)
                val jsonStr = String(buffer)
                var data = JSONArray(jsonStr)
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val kEntity = KEntity(
                        item.getString("high").toFloat(),
                        item.getString("low").toFloat(),
                        item.getString("open").toFloat(),
                        item.getString("close").toFloat(),
                        item.getLong("volume"),
                        item.getLong("time")
                    )
                    result.add(kEntity)
                }
            }
            result
        }
    }

    private suspend fun loadDataFromTimeDataAsserts(
        context: Context,
        fileName: String
    ): MutableList<IKEntity> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<IKEntity>()
            context.assets.open(fileName).use { inputStream ->
                var buffer = ByteArray(inputStream.available())
                inputStream.read(buffer)
                val jsonStr = String(buffer)
                var data = JSONArray(jsonStr)
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val kEntity = KEntity(
                        item.getString("price").toFloat(),
                        item.getString("price").toFloat(),
                        item.getString("price").toFloat(),
                        item.getString("price").toFloat(),
                        item.getLong("volume"),
                        item.getLong("time"),
                        item.getString("avgPrice").toFloat()
                    )
                    result.add(kEntity)
                }
            }
            result
        }

    }


    fun loadActiveChartData(context: Context, fileIdx: Int, callback: (ActiveResponse) -> Unit) {
        MainScope().launch {
            callback.invoke(
                loadDataFromActiveDataAsserts(
                    context,
                    "mock_active_data_${fileIdx}.txt"
                )
            )
        }
    }

    private suspend fun loadDataFromActiveDataAsserts(
        context: Context,
        fileName: String
    ): ActiveResponse {
        return withContext(Dispatchers.IO) {
            var dataList = mutableListOf<IKEntity>()
            var preClosePrice = 0f
            context.assets.open(fileName).use { inputStream ->
                var buffer = ByteArray(inputStream.available())
                inputStream.read(buffer)
                val jsonStr = String(buffer)
                val json = JSONObject(jsonStr)
                preClosePrice = json.getString("preClosePrice").toFloat()
                var data = json.getJSONArray("data")
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)

                    var activeInfo: ActiveInfo? = null
                    item.optJSONObject("active")?.let {
                        activeInfo = ActiveInfo(it.getString("industry"), it.getBoolean("red"))
                    }
                    val activeChartEntity = ActiveChartKEntity(
                        item.getString("price").toFloat(),
                        item.getString("avgPrice").toFloat(),
                        item.getLong("time"),
                        item.getLong("volume"),
                        activeInfo
                    )
                    dataList.add(activeChartEntity)
                }
            }
            ActiveResponse(preClosePrice, dataList)
        }

    }
}


object StockDataGenerator {

    // 模拟A股全天走势数据
    fun generateDailyStockData(basePrice: Float, date: String): MutableList<IKEntity> {
        val tradingTimes = getTradingTimestamps(date)
        val stockDataList = mutableListOf<IKEntity>()
        var lastClosePrice = basePrice

        for (time in tradingTimes) {
            val entity = generateMinuteData(lastClosePrice, time)
            stockDataList.add(entity)
            lastClosePrice = entity.getClosePrice()
        }

        return stockDataList
    }

    // 生成一分钟的数据
    private fun generateMinuteData(lastClosePrice: Float, time: Long): KEntity {
        val openPrice = lastClosePrice
        val closePrice = openPrice * Random.nextDouble(0.995, 1.005).toFloat()  // 每分钟波动±0.5%
        val highPrice = maxOf(openPrice, closePrice) * Random.nextDouble(1.0, 1.01).toFloat()
        val lowPrice = minOf(openPrice, closePrice) * Random.nextDouble(0.99, 1.0).toFloat()
        val volume = Random.nextLong(1000, 50000)  // 每分钟成交量

        val avgPrice = (openPrice + closePrice + highPrice + lowPrice) / 4

        return KEntity(
            highPrice = highPrice,
            lowPrice = lowPrice,
            openPrice = openPrice,
            closePrice = closePrice,
            volume = volume,
            time = time,
            avgPrice = avgPrice,
            flag = FLAG_EMPTY
        )
    }

    // 获取A股交易时间戳（每分钟一条数据）
    private fun getTradingTimestamps(date: String): List<Long> {
        val periods = listOf(
            Pair("09:30", "11:30"),
            Pair("13:00", "15:00")
        )
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
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