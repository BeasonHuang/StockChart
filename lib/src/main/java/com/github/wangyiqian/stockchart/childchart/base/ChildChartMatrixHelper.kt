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

package com.github.wangyiqian.stockchart.childchart.base

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import com.github.wangyiqian.stockchart.IStockChart
import kotlin.math.abs
import kotlin.math.round

/**
 * 管理子图的matrix
 * @author wangyiqian E-mail: wangyiqian9891@gmail.com
 * @version 创建时间: 2021/2/3
 */
internal class ChildChartMatrixHelper<O : BaseChildChartConfig>(
    private val stockChart: IStockChart,
    private val chart: BaseChildChart<O>
) {

    // 用于逻辑坐标转实际坐标，这个matrix转换的结果是所有数据填满显示区域，而不关注需要显示哪些指定范围的数据
    val coordinateMatrix by lazy { Matrix() }

    // 如果需要"一格一格"滑动，则x轴可能由于缩放引起"半个"数据显示在边缘，需要调整
    private val fixXMatrix by lazy { Matrix() }

    // 调整Y，使得最终的内容填满显示区域
    private val fixYMatrix by lazy { Matrix() }

    // 多个matrix组合后
    private val concatMatrix by lazy { Matrix() }

    // 临时载体
    private val tmp2FloatArray by lazy { FloatArray(2) }
    private val tmp4FloatArray by lazy { FloatArray(4) }
    private val tmpMatrix by lazy { Matrix() }

    /**
     * 初始准备
     */
    fun prepare() {
        prepareCoordinateMatrix()
    }

    /**
     * 每次绘制的时候，需要设置相关matrix
     */
    fun setOnDraw() {
        setFixXMatrix()
        setFixYMatrix()
        setConcatMatrix()
    }

    private fun prepareCoordinateMatrix() {
        //清空当前的 Matrix 对象，确保每次变换时不会叠加之前的变换。
        coordinateMatrix.reset()
        //获取图表的主显示区域，这个区域是最终图形渲染的区域（例如，图表在屏幕上的矩形区域）。
        val chartMainDisplayArea = chart.getChartMainDisplayArea()
        //获取显示的 X 轴数据范围（showStartIndex 和 showEndIndex 决定显示的数据区间），并将结果存储到 tmp2FloatArray 中。
        //tmp2FloatArray[0] 是 X 轴的起始值，tmp2FloatArray[1] 是 X 轴的结束值。
        chart.getXValueRange(
            stockChart.getConfig().showStartIndex,
            stockChart.getConfig().showEndIndex,
            tmp2FloatArray
        )

        //计算 X 轴的长度（结束值减去起始值）。
        val xValueRangeFrom = tmp2FloatArray[0]
        val xValueRangeEnd = tmp2FloatArray[1]
        val xValueRangeLen = xValueRangeEnd - xValueRangeFrom
        //获取 Y 轴数据范围，存储在 tmp2FloatArray 中。
        //tmp2FloatArray[0] 是 Y 轴的起始值，tmp2FloatArray[1] 是 Y 轴的结束值。
        chart.getYValueRange(
            stockChart.getConfig().showStartIndex,
            stockChart.getConfig().showEndIndex,
            tmp2FloatArray
        )
        val yValueRangeFrom = tmp2FloatArray[0]
        val yValueRangeEnd = tmp2FloatArray[1]
        var yValueRangeLen = yValueRangeEnd - yValueRangeFrom

        /**
         * . 例子直观展示
         * 假设我们有以下设置：
         *
         * 显示区域：chartMainDisplayArea = RectF(100, 50, 500, 450)，即显示区域从屏幕上的 (100,50) 开始。
         * 数据：X 轴数据范围是从 x = 10 到 x = 110。
         * 无平移时的绘制情况：
         * 数据 x = 10 会被绘制在屏幕的 x = 10，完全在显示区域之外，用户看不到数据。
         * 数据 x = 100 会被绘制在屏幕的 x = 100，这时数据才进入显示区域，但这显然不是我们想要的效果。
         * 平移 90 像素后的绘制情况：
         * 数据 x = 10 被平移到屏幕的 x = 100，正好对齐到显示区域的左边界。
         * 数据 x = 110 被平移到屏幕的 x = 200，正常绘制在显示区域内。
         * 效果：所有数据都对齐到显示区域，图表显示完整。
         */
        if (yValueRangeLen == 0f) {
            // 非正常情况，y轴逻辑区间无法算出（所有值相等），之前处于原始逻辑坐标，将需要显示的逻辑区域移动到显示区域左边垂直居中位置
            coordinateMatrix.postTranslate(
                chartMainDisplayArea.left - xValueRangeFrom,
                (chartMainDisplayArea.bottom - chartMainDisplayArea.top) / 2
            )
        } else {
            // 正常情况，之前处于原始逻辑坐标，将需要显示的逻辑区域移动到显示区域左上角
            coordinateMatrix.postTranslate(
                chartMainDisplayArea.left - xValueRangeFrom,
                chartMainDisplayArea.top - yValueRangeFrom
            )
        }
        //sx 是 X 轴的缩放比例，将 X 轴的逻辑范围缩放到显示区域的宽度。
        //sy 是 Y 轴的缩放比例，如果 Y 轴的范围为零，则不缩放；否则，根据 Y 轴的值范围和显示区域的高度进行缩放。
        val sx = (chartMainDisplayArea.right - chartMainDisplayArea.left) / xValueRangeLen
        val sy = if (yValueRangeLen == 0f) {
            // 非正常情况，y轴逻辑区间无法算出（所有值相等），直接保持原状不缩放
            1f
        } else {
            // 正常情况，y按照区间比缩放即可
            (chartMainDisplayArea.bottom - chartMainDisplayArea.top) / yValueRangeLen
        }

        // 缩放使得需要显示的内容刚好撑满显示区域，再向上翻转，使得y内容翻转在显示区域上方
        //-sy：负的 Y 轴缩放因子，这是 Y 轴反转的核心。将 Y 轴的数据反转，使得 Y 轴从 上往下递增 变成 下往上递增。
        coordinateMatrix.postScale(
            sx,
            -sy,
            chartMainDisplayArea.left,
            chartMainDisplayArea.top
        )

        // 正常情况，下移一个显示区域
        coordinateMatrix.postTranslate(0f, chartMainDisplayArea.bottom - chartMainDisplayArea.top)
    }

    /**
     * 计算[fixXMatrix]
     */
    private fun setFixXMatrix() {
        fixXMatrix.reset()

        if (!stockChart.getConfig().scrollSmoothly) {

            // "一格一格"地滑

            val mainChartDisplayArea = chart.getChartMainDisplayArea()

            tmp2FloatArray[0] = mainChartDisplayArea.left
            tmp2FloatArray[1] = 0f
            // 反算出会被移动到显示区域的第一个逻辑坐标值（数据下标）
            tmpMatrix.apply {
                reset()
                postConcat(coordinateMatrix)
                postConcat(stockChart.getXScaleMatrix())
                postConcat(stockChart.getFixXScaleMatrix())
                postConcat(stockChart.getScrollMatrix())
                invert(tmpMatrix)
                mapPoints(tmp2FloatArray)
            }
            var indexFrom = (round(tmp2FloatArray[0])).toInt()
            if (indexFrom !in 0 until stockChart.getConfig().getKEntitiesSize()) {
                indexFrom = 0
            }

            // 计算出"一格"长度
            tmp4FloatArray[0] = indexFrom.toFloat()
            tmp4FloatArray[1] = 0f
            tmp4FloatArray[2] = (indexFrom + 1).toFloat()
            tmp4FloatArray[3] = 0f
            tmpMatrix.apply {
                reset()
                postConcat(coordinateMatrix)
                postConcat(stockChart.getXScaleMatrix())
                postConcat(stockChart.getFixXScaleMatrix())
                postConcat(stockChart.getScrollMatrix())
                mapPoints(tmp4FloatArray)
            }
            val first = tmp4FloatArray[0]
            val second = tmp4FloatArray[2]
            val lengthOfOneIndex = second - first

            if (lengthOfOneIndex != 0f) {
                val unalignedDis = (first - mainChartDisplayArea.left) % lengthOfOneIndex

                val dx = when {
                    // 右移
                    unalignedDis < 0 && abs(unalignedDis) < lengthOfOneIndex / 2 -> {
                        abs(unalignedDis)
                    }
                    // 左移
                    unalignedDis < 0 && abs(unalignedDis) > lengthOfOneIndex / 2 -> {
                        -abs(lengthOfOneIndex - unalignedDis)
                    }
                    // 左移
                    unalignedDis > 0 && abs(unalignedDis) < lengthOfOneIndex / 2 -> {
                        -abs(unalignedDis)
                    }

                    // 右移
                    unalignedDis > 0 && abs(unalignedDis) > lengthOfOneIndex / 2 -> abs(
                        lengthOfOneIndex - unalignedDis
                    )
                    // 不移动
                    else -> 0f
                }

                fixXMatrix.postTranslate(dx, 0f)
            }
        }
    }

    /**
     * 计算[fixYMatrix]，使y轴内容刚好填满显示区域
     */
    private fun setFixYMatrix() {
        fixYMatrix.reset()

        val mainChartDisplayArea = chart.getChartMainDisplayArea()

        tmp4FloatArray[0] = mainChartDisplayArea.left
        tmp4FloatArray[1] = 0f
        tmp4FloatArray[2] = mainChartDisplayArea.right
        tmp4FloatArray[3] = 0f
        // 反算出哪个下标（逻辑坐标）范围会被移动到显示区域
        tmpMatrix.apply {
            reset()
            postConcat(coordinateMatrix)
            postConcat(stockChart.getXScaleMatrix())
            postConcat(stockChart.getFixXScaleMatrix())
            postConcat(stockChart.getScrollMatrix())
            postConcat(fixXMatrix)
            invert(tmpMatrix)
            mapPoints(tmp4FloatArray)
        }
        var indexFrom = (round(tmp4FloatArray[0])).toInt()
        if (indexFrom !in 0 until stockChart.getConfig().getKEntitiesSize()) {
            indexFrom = 0
        }
        var indexEnd = (round(tmp4FloatArray[2])).toInt()
        if (indexEnd !in 0 until stockChart.getConfig().getKEntitiesSize()) {
            indexEnd = stockChart.getConfig().getKEntitiesSize() - 1
        }

        // 算出移到实际显示区域后保持原缩放比例时的y实际坐标范围
        chart.getYValueRange(indexFrom, indexEnd, tmp2FloatArray)
        val yValueRangeFrom = tmp2FloatArray[0]
        val yValueRangeEnd = tmp2FloatArray[1]
        tmp4FloatArray[0] = 0f
        tmp4FloatArray[1] = yValueRangeFrom
        tmp4FloatArray[2] = 0f
        tmp4FloatArray[3] = yValueRangeEnd
        tmpMatrix.apply {
            reset()
            postConcat(coordinateMatrix)
            postConcat(stockChart.getXScaleMatrix())
            postConcat(stockChart.getFixXScaleMatrix())
            postConcat(stockChart.getScrollMatrix())
            postConcat(fixXMatrix)
            mapPoints(tmp4FloatArray)
        }
        val yMin: Float
        val yMax: Float
        if (tmp4FloatArray[3] > tmp4FloatArray[1]) {
            yMin = tmp4FloatArray[1]
            yMax = tmp4FloatArray[3]
        } else {
            yMin = tmp4FloatArray[3]
            yMax = tmp4FloatArray[1]
        }

        if (yMin != yMax) {
            // 先贴顶
            fixYMatrix.postTranslate(0f, mainChartDisplayArea.top - yMin)
            val sy = (mainChartDisplayArea.bottom - mainChartDisplayArea.top) / (yMax - yMin)
            // 再缩放
            fixYMatrix.postScale(1f, sy, 0f, mainChartDisplayArea.top)
        }
    }

    private fun setConcatMatrix() {
        concatMatrix.apply {
            reset()
            postConcat(coordinateMatrix)
            postConcat(stockChart.getXScaleMatrix())
            postConcat(stockChart.getFixXScaleMatrix())
            postConcat(stockChart.getScrollMatrix())
            postConcat(fixXMatrix)
            postConcat(fixYMatrix)
            stockChart.getConfig().extMatrix?.also {
                postConcat(it)
            }
        }
    }

    fun mapPointsValue2Real(pts: FloatArray) {
        setConcatMatrix()
        concatMatrix.mapPoints(pts)
    }

    fun mapRectValue2Real(rect: RectF) {
        setConcatMatrix()
        concatMatrix.mapRect(rect)
    }

    fun mapPathValue2Real(path: Path) {
        setConcatMatrix()
        path.transform(concatMatrix)
    }

    fun mapPointsReal2Value(pts: FloatArray) {
        setConcatMatrix()
        concatMatrix.invert(tmpMatrix)
        tmpMatrix.mapPoints(pts)
    }

    fun mapRectReal2Value(rect: RectF) {
        setConcatMatrix()
        concatMatrix.invert(tmpMatrix)
        tmpMatrix.mapRect(rect)
    }

    fun mapPathReal2Value(path: Path) {
        setConcatMatrix()
        concatMatrix.invert(tmpMatrix)
        path.transform(tmpMatrix)
    }

}