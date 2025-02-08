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

package com.github.wangyiqian.stockchart.childchart.kchart

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.github.wangyiqian.stockchart.IStockChart
import com.github.wangyiqian.stockchart.childchart.base.BaseChildChart
import com.github.wangyiqian.stockchart.entities.FLAG_EMPTY
import com.github.wangyiqian.stockchart.entities.FLAG_LINE_STARTER
import com.github.wangyiqian.stockchart.entities.containFlag
import com.github.wangyiqian.stockchart.index.Index
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @author wangyiqian E-mail: wangyiqian9891@gmail.com
 * @version 创建时间: 2021/1/28
 */
open class KChart(
    stockChart: IStockChart,
    chartConfig: KChartConfig
) : BaseChildChart<KChartConfig>(stockChart, chartConfig) {

    //K线绘制效果
    private val lineKChartLinePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    }

    //蜡烛k图绘制效果
    private val candleKChartPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    }

    //空心蜡烛绘制效果
    private val hollowKChartPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    }

    //K条形图绘制效果
    private val barKChartPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    }

    //山峰图绘制效果
    private val mountainKChartPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    }

    //山地渐变 K 图表画图
    private val mountainGradientKChartPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    }

    //山地线性坡度
    private var mountainLinearGradient: LinearGradient? = null

    //山峰图的封闭渐变色
    private var mountainLinearGradientColors = intArrayOf()

    //高光水平线条绘制
    private val highlightHorizontalLinePaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    //高光垂直线绘制
    private val highlightVerticalLinePaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    //成本价 线涂料
    private val costPriceLinePaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    //高亮标签绘制
    private val highlightLabelPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    //高亮标签 BG 绘画
    private val highlightLabelBgPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    //索引涂料
    private val indexPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    }

    //索引文本绘制
    private val indexTextPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    //最高和最低的标签涂料
    private val highestAndLowestLabelPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    //标签涂料
    private val labelPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    //平均价格线油漆
    private val avgPriceLinePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    }

    //索引列表
    private var indexList: List<List<Float?>>? = null

    //上次计算索引类型
    private var lastCalculateIndexType: Index? = null

    //绘制的索引文本高度
    private var drawnIndexTextHeight = 0f

    override fun onKEntitiesChanged() {
        calculateIndexList()
    }

    private fun calculateIndexList() {
        indexList = null
        lastCalculateIndexType = chartConfig.index
        chartConfig.index?.apply {
            when (this) {
                is Index.MA, is Index.EMA, is Index.BOLL -> {
                    indexList = calculate(getKEntities())
                }

                else -> {
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setMountainLinearGradient()
    }

    private fun setMountainLinearGradient() {
        mountainLinearGradientColors = chartConfig.mountainChartLinearGradientColors
        mountainLinearGradient = LinearGradient(
            0f,
            getChartDisplayArea().top,
            0f,
            getChartDisplayArea().bottom,
            mountainLinearGradientColors,
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun getYValueRange(startIndex: Int, endIndex: Int, result: FloatArray) {

        if (chartConfig.index == null || chartConfig.index != lastCalculateIndexType) {
            calculateIndexList()
        }

        if (chartConfig.yValueMin != null && chartConfig.yValueMax != null) {
            result[0] = chartConfig.yValueMin!!
            result[1] = chartConfig.yValueMax!!
            return
        }

        var yMin = 0f
        var yMax = 0f

        getKEntities().filterIndexed { index, kEntity ->
            index in startIndex..endIndex && !kEntity.containFlag(
                FLAG_EMPTY
            )
        }
            .apply {
                when (chartConfig.kChartType) {
                    is KChartConfig.KChartType.CANDLE, is KChartConfig.KChartType.HOLLOW, is KChartConfig.KChartType.BAR -> {
                        yMin = minByOrNull { it.getLowPrice() }?.getLowPrice() ?: 0f
                        yMax = maxByOrNull { it.getHighPrice() }?.getHighPrice() ?: 0f
                    }

                    else -> {
                        forEachIndexed { index, kEntity ->
                            if (index == 0) {
                                yMin = kEntity.getClosePrice()
                                yMax = kEntity.getClosePrice()
                            } else {
                                yMin = min(yMin, kEntity.getClosePrice())
                                yMax = max(yMax, kEntity.getClosePrice())
                            }
                            kEntity.getAvgPrice()?.let { avgPrice ->
                                if (needDrawAvgPriceLine()) {
                                    yMin = min(yMin, avgPrice)
                                    yMax = max(yMax, avgPrice)
                                }
                            }
                        }
                    }
                }
            }

        indexList?.forEach { valueList ->
            valueList.filterIndexed { idx, _ -> idx in startIndex..endIndex }.filterNotNull()
                .apply {
                    if (isNotEmpty()) {
                        yMax = max(yMax, maxOrNull() ?: 0f)
                        yMin = min(yMin, minOrNull() ?: 0f)
                    }
                }
        }

        if (abs(yMin - yMax) > stockChart.getConfig().valueTendToZero) {
            result[0] = yMin
            result[1] = yMax
        } else { // 约等于0
            var delta = abs(chartConfig.costPrice ?: (0f - yMin)) * 2
            if (delta == yMin) {
                delta = abs(yMin / 2f)
            }
            result[0] = yMin - delta
            result[1] = yMax + delta
        }
        chartConfig.yValueMin?.apply { result[0] = this }
        chartConfig.yValueMax?.apply { result[1] = this }
    }

    override fun preDrawBackground(canvas: Canvas) {}

    override fun drawBackground(canvas: Canvas) {}

    override fun preDrawData(canvas: Canvas) {}

    override fun drawData(canvas: Canvas) {
        when (chartConfig.kChartType) {
            is KChartConfig.KChartType.LINE -> {
                //绘制K线分时线，折线图
                drawLineKChart(canvas)
            }

            is KChartConfig.KChartType.CANDLE -> {
                drawCandleKChart(canvas)
            }

            is KChartConfig.KChartType.HOLLOW -> {
                drawHollowKChart(canvas)
            }

            is KChartConfig.KChartType.MOUNTAIN -> {
                drawMountainKChart(canvas)
            }

            is KChartConfig.KChartType.BAR -> {
                drawBarKChart(canvas)
            }
        }
        //绘制均线
        drawAvgPriceLine(canvas)
        //绘制最高最低价格
        drawHighestAndLowestLabel(canvas)
        //绘制指标线和指标文本
        drawIndex(canvas)
    }

    override fun preDrawHighlight(canvas: Canvas) {
        //绘制均线线条，需要设置kChartConfig.costPrice
        drawCostPriceLine(canvas)
        //绘制左右价格标签label
        drawLabels(canvas)
    }

    override fun drawHighlight(canvas: Canvas) {
        getHighlight()?.let { highlight ->
            val highlightAreaTop = getChartDisplayArea().top + drawnIndexTextHeight
            if (stockChart.getConfig().showHighlightHorizontalLine) {
                if (highlight.y >= highlightAreaTop && highlight.y <= getChartDisplayArea().bottom) {

                    highlightHorizontalLinePaint.color =
                        stockChart.getConfig().highlightHorizontalLineColor
                    highlightHorizontalLinePaint.strokeWidth =
                        stockChart.getConfig().highlightHorizontalLineWidth
                    highlightHorizontalLinePaint.pathEffect =
                        stockChart.getConfig().highlightHorizontalLinePathEffect

                    var highlightHorizontalLineLeft = getChartDisplayArea().left
                    var highlightHorizontalLineRight = getChartDisplayArea().right

                    // left highlight label
                    chartConfig.highlightLabelLeft?.let { highlightLabel ->
                        highlightLabelPaint.textSize = highlightLabel.textSize
                        highlightLabelPaint.color = highlightLabel.textColor
                        highlightLabelBgPaint.color = highlightLabel.bgColor
                        val text = highlightLabel.textFormat(highlight.valueY)
                        highlightLabelPaint.getTextBounds(text, 0, text.length, tmpRect)
                        val textWidth = tmpRect.width()
                        val textHeight = tmpRect.height()
                        val bgWidth = textWidth + highlightLabel.padding * 2
                        val bgHeight = textHeight + highlightLabel.padding * 2
                        tmpRectF.left = getChartDisplayArea().left
                        tmpRectF.top = highlight.y - bgHeight / 2
                        tmpRectF.right = bgWidth
                        tmpRectF.bottom = highlight.y + bgHeight / 2
                        if (tmpRectF.top < highlightAreaTop) {
                            tmpRectF.offset(0f, highlightAreaTop - tmpRectF.top)
                        } else if (tmpRectF.bottom > getChartDisplayArea().bottom) {
                            tmpRectF.offset(0f, getChartDisplayArea().bottom - tmpRectF.bottom)
                        }
                        highlightLabelPaint.getFontMetrics(tmpFontMetrics)
                        val textBaseLine =
                            tmpRectF.top + bgHeight / 2 + (tmpFontMetrics.bottom - tmpFontMetrics.top) / 2 - tmpFontMetrics.bottom

                        canvas.drawRoundRect(
                            tmpRectF,
                            highlightLabel.bgCorner,
                            highlightLabel.bgCorner,
                            highlightLabelBgPaint
                        )

                        canvas.drawText(
                            text,
                            tmpRectF.left + highlightLabel.padding,
                            textBaseLine,
                            highlightLabelPaint
                        )

                        highlightHorizontalLineLeft += bgWidth
                    }

                    // right highlight label
                    chartConfig.highlightLabelRight?.let { highlightLabel ->
                        highlightLabelPaint.textSize = highlightLabel.textSize
                        highlightLabelPaint.color = highlightLabel.textColor
                        highlightLabelBgPaint.color = highlightLabel.bgColor
                        val text = highlightLabel.textFormat(highlight.valueY)
                        highlightLabelPaint.getTextBounds(text, 0, text.length, tmpRect)
                        val textWidth = tmpRect.width()
                        val textHeight = tmpRect.height()
                        val bgWidth = textWidth + highlightLabel.padding * 2
                        val bgHeight = textHeight + highlightLabel.padding * 2
                        tmpRectF.left = getChartDisplayArea().right - bgWidth
                        tmpRectF.top = highlight.y - bgHeight / 2
                        tmpRectF.right = getChartDisplayArea().right
                        tmpRectF.bottom = highlight.y + bgHeight / 2
                        if (tmpRectF.top < highlightAreaTop) {
                            tmpRectF.offset(0f, highlightAreaTop - tmpRectF.top)
                        } else if (tmpRectF.bottom > getChartDisplayArea().bottom) {
                            tmpRectF.offset(0f, getChartDisplayArea().bottom - tmpRectF.bottom)
                        }
                        highlightLabelPaint.getFontMetrics(tmpFontMetrics)
                        val textBaseLine =
                            tmpRectF.top + bgHeight / 2 + (tmpFontMetrics.bottom - tmpFontMetrics.top) / 2 - tmpFontMetrics.bottom

                        canvas.drawRoundRect(
                            tmpRectF,
                            highlightLabel.bgCorner,
                            highlightLabel.bgCorner,
                            highlightLabelBgPaint
                        )

                        canvas.drawText(
                            text,
                            tmpRectF.left + highlightLabel.padding,
                            textBaseLine,
                            highlightLabelPaint
                        )

                        highlightHorizontalLineRight -= bgWidth
                    }

                    val saveCount = canvas.saveLayer(
                        getChartMainDisplayArea().left,
                        getChartDisplayArea().top,
                        getChartMainDisplayArea().right,
                        getChartDisplayArea().bottom,
                        null
                    )

                    // highlight horizontal line
                    canvas.drawLine(
                        highlightHorizontalLineLeft,
                        highlight.y,
                        highlightHorizontalLineRight,
                        highlight.y,
                        highlightHorizontalLinePaint
                    )

                    canvas.restoreToCount(saveCount)

                }
            }

            if (stockChart.getConfig().showHighlightVerticalLine) {
                if (highlight.x >= getChartDisplayArea().left && highlight.x <= getChartDisplayArea().right) {

                    highlightVerticalLinePaint.color =
                        stockChart.getConfig().highlightVerticalLineColor
                    highlightVerticalLinePaint.strokeWidth =
                        stockChart.getConfig().highlightVerticalLineWidth
                    highlightVerticalLinePaint.pathEffect =
                        stockChart.getConfig().highlightVerticalLinePathEffect

                    tmp2FloatArray[0] = highlight.getIdx() + 0.5f
                    tmp2FloatArray[1] = 0f
                    mapPointsValue2Real(tmp2FloatArray)
                    val x = tmp2FloatArray[0]

                    var highlightHorizontalLineTop = highlightAreaTop
                    var highlightHorizontalLineBottom = getChartDisplayArea().bottom

                    // top highlight label
                    chartConfig.highlightLabelTop?.let { highlightLabel ->
                        highlightLabelPaint.textSize = highlightLabel.textSize
                        highlightLabelPaint.color = highlightLabel.textColor
                        highlightLabelBgPaint.color = highlightLabel.bgColor
                        val text = highlightLabel.textFormat(highlight.getIdx().toFloat())
                        highlightLabelPaint.getTextBounds(text, 0, text.length, tmpRect)
                        val textWidth = tmpRect.width()
                        val textHeight = tmpRect.height()
                        val bgWidth = textWidth + highlightLabel.padding * 2
                        val bgHeight = textHeight + highlightLabel.padding * 2
                        tmpRectF.left = x - bgWidth / 2
                        tmpRectF.top = highlightAreaTop
                        tmpRectF.right = x + bgWidth / 2
                        tmpRectF.bottom = highlightAreaTop + bgHeight
                        if (tmpRectF.left < getChartDisplayArea().left) {
                            tmpRectF.offset(getChartDisplayArea().left - tmpRectF.left, 0f)
                        } else if (tmpRectF.right > getChartDisplayArea().right) {
                            tmpRectF.offset(getChartDisplayArea().right - tmpRectF.right, 0f)
                        }
                        highlightLabelPaint.getFontMetrics(tmpFontMetrics)
                        val textBaseLine =
                            tmpRectF.top + bgHeight / 2 + (tmpFontMetrics.bottom - tmpFontMetrics.top) / 2 - tmpFontMetrics.bottom

                        canvas.drawRoundRect(
                            tmpRectF,
                            highlightLabel.bgCorner,
                            highlightLabel.bgCorner,
                            highlightLabelBgPaint
                        )

                        canvas.drawText(
                            text,
                            tmpRectF.left + highlightLabel.padding,
                            textBaseLine,
                            highlightLabelPaint
                        )

                        highlightHorizontalLineTop += bgHeight
                    }

                    // bottom highlight label
                    chartConfig.highlightLabelBottom?.let { highlightLabel ->
                        highlightLabelPaint.textSize = highlightLabel.textSize
                        highlightLabelPaint.color = highlightLabel.textColor
                        highlightLabelBgPaint.color = highlightLabel.bgColor
                        val text = highlightLabel.textFormat(highlight.getIdx().toFloat())
                        highlightLabelPaint.getTextBounds(text, 0, text.length, tmpRect)
                        val textWidth = tmpRect.width()
                        val textHeight = tmpRect.height()
                        val bgWidth = textWidth + highlightLabel.padding * 2
                        val bgHeight = textHeight + highlightLabel.padding * 2
                        tmpRectF.left = x - bgWidth / 2
                        tmpRectF.top = getChartDisplayArea().bottom - bgHeight
                        tmpRectF.right = x + bgWidth / 2
                        tmpRectF.bottom = getChartDisplayArea().bottom
                        if (tmpRectF.left < getChartDisplayArea().left) {
                            tmpRectF.offset(getChartDisplayArea().left - tmpRectF.left, 0f)
                        } else if (tmpRectF.right > getChartDisplayArea().right) {
                            tmpRectF.offset(getChartDisplayArea().right - tmpRectF.right, 0f)
                        }
                        highlightLabelPaint.getFontMetrics(tmpFontMetrics)
                        val textBaseLine =
                            tmpRectF.top + bgHeight / 2 + (tmpFontMetrics.bottom - tmpFontMetrics.top) / 2 - tmpFontMetrics.bottom

                        canvas.drawRoundRect(
                            tmpRectF,
                            highlightLabel.bgCorner,
                            highlightLabel.bgCorner,
                            highlightLabelBgPaint
                        )

                        canvas.drawText(
                            text,
                            tmpRectF.left + highlightLabel.padding,
                            textBaseLine,
                            highlightLabelPaint
                        )

                        highlightHorizontalLineBottom -= bgHeight
                    }

                    val saveCount = canvas.saveLayer(
                        getChartMainDisplayArea().left,
                        getChartDisplayArea().top,
                        getChartMainDisplayArea().right,
                        getChartDisplayArea().bottom,
                        null
                    )

                    // highlight vertical line
                    canvas.drawLine(
                        x,
                        highlightHorizontalLineTop,
                        x,
                        highlightHorizontalLineBottom,
                        highlightVerticalLinePaint
                    )

                    canvas.restoreToCount(saveCount)
                }
            }
        }
    }

    override fun drawAddition(canvas: Canvas) {}

    private fun drawCostPriceLine(canvas: Canvas) {
        val saveCount = canvas.saveLayer(
            getChartMainDisplayArea().left,
            getChartDisplayArea().top,
            getChartMainDisplayArea().right,
            getChartDisplayArea().bottom,
            null
        )
        chartConfig.costPrice?.let {
            costPriceLinePaint.color = chartConfig.costPriceLineColor
            costPriceLinePaint.strokeWidth = chartConfig.costPriceLineWidth
            tmp2FloatArray[0] = 0f
            tmp2FloatArray[1] = it
            mapPointsValue2Real(tmp2FloatArray)
            canvas.drawLine(
                getChartDisplayArea().left,
                tmp2FloatArray[1],
                getChartDisplayArea().right,
                tmp2FloatArray[1],
                costPriceLinePaint
            )
        }
        canvas.restoreToCount(saveCount)
    }

    private fun drawHighestAndLowestLabel(canvas: Canvas) {
        val saveCount = canvas.saveLayer(
            getChartMainDisplayArea().left,
            getChartDisplayArea().top,
            getChartMainDisplayArea().right,
            getChartDisplayArea().bottom,
            null
        )

        chartConfig.kChartType.highestAndLowestLabelConfig?.let { config ->
            highestAndLowestLabelPaint.textSize = config.labelTextSize
            highestAndLowestLabelPaint.strokeWidth = config.lineStrokeWidth
            highestAndLowestLabelPaint.color = config.labelColor
            tmp4FloatArray[0] = getChartMainDisplayArea().left
            tmp4FloatArray[1] = 0f
            tmp4FloatArray[2] = getChartMainDisplayArea().right
            tmp4FloatArray[3] = 0f
            mapPointsReal2Value(tmp4FloatArray)
            val leftIdx = (tmp4FloatArray[0] + 0.5f).toInt()
            val rightIdx = (tmp4FloatArray[2] + 0.5f).toInt() - 1

            var maxIdx: Int? = null
            var minIdx: Int? = null
            var maxPrice = 0f
            var minPrice = 0f
            val kEntities = getKEntities()
            for (i in leftIdx..rightIdx) {
                if (i in kEntities.indices && !kEntities[i].containFlag(FLAG_EMPTY)) {
                    if (minIdx == null || maxIdx == null) {
                        maxIdx = i
                        minIdx = i
                        maxPrice = kEntities[i].getHighPrice()
                        minPrice = kEntities[i].getLowPrice()
                    } else {
                        if (kEntities[i].getHighPrice() > maxPrice) {
                            maxIdx = i
                            maxPrice = kEntities[i].getHighPrice()
                        }
                        if (kEntities[i].getLowPrice() < minPrice) {
                            minIdx = i
                            minPrice = kEntities[i].getLowPrice()
                        }
                    }
                }
            }

            maxIdx?.let {
                doDrawHighestAndLowestLabel(canvas, config, it, maxPrice)
            }

            minIdx?.let {
                doDrawHighestAndLowestLabel(canvas, config, it, minPrice)
            }
        }
        canvas.restoreToCount(saveCount)
    }

    private fun doDrawHighestAndLowestLabel(
        canvas: Canvas,
        config: KChartConfig.HighestAndLowestLabelConfig,
        idx: Int,
        price: Float
    ) {
        tmp2FloatArray[0] = idx + 0.5f
        tmp2FloatArray[1] = price
        mapPointsValue2Real(tmp2FloatArray)
        //确定标签绘制方向
        val isLeft =
            tmp2FloatArray[0] - getChartDisplayArea().left > (getChartDisplayArea().right - getChartDisplayArea().left) / 2
        //计算水平线的终点
        val lineLength = config.lineLength
        val lineEndX =
            if (isLeft) tmp2FloatArray[0] - lineLength else tmp2FloatArray[0] + lineLength
        //绘制水平线使用 canvas.drawLine 绘制一条从 (tmp2FloatArray[0], tmp2FloatArray[1]) 到 (lineEndX, tmp2FloatArray[1]) 的水平线，表示价格标签的线段
        canvas.drawLine(
            tmp2FloatArray[0],
            tmp2FloatArray[1],
            lineEndX,
            tmp2FloatArray[1],
            highestAndLowestLabelPaint
        )
        //绘制价格文本
        val text = "${config.formatter.invoke(price)}"
        //计算文本的宽度，这有助于确定文本的起始位置，避免文本超出画布
        val textWidth = highestAndLowestLabelPaint.measureText(text)
        //根据 isLeft 的值计算文本的 X 坐标。如果标签位于左侧，文本应从水平线的左边开始，否则从右边开始
        val textStartX = if (isLeft) lineEndX - textWidth else lineEndX
        //获取当前画笔的字体度量信息，用于计算文本的基线
        highestAndLowestLabelPaint.getFontMetrics(tmpFontMetrics)
        //计算文本的基线 Y 坐标，以确保文本在垂直方向居中显示。baseLine 是基于价格点 tmp2FloatArray[1] 加上字体的上升和下降高度来计算的
        val baseLine =
            tmp2FloatArray[1] + (tmpFontMetrics.bottom - tmpFontMetrics.top) / 2 - tmpFontMetrics.bottom
        canvas.drawText(text, textStartX, baseLine, highestAndLowestLabelPaint)
    }

    /**
     * 个方法 drawIndex 用于在自定义的股票或K线图表中绘制技术指标（如均线、MACD等）和相关文本信息。
     * 它分为两部分：一部分用于绘制指标曲线，另一部分用于绘制指标的文本信息。
     */
    private fun drawIndex(canvas: Canvas) {
        //drawnIndexTextHeight = 0f：初始化绘制的文本高度，用于后续计算文本占据的垂直空间。
        drawnIndexTextHeight = 0f
        if (chartConfig.index == null) {
            return
        }
        //保存当前画布的状态，并创建一个新的绘图层，便于后续操作恢复画布状态，避免影响其他绘制内容。
        val saveCount = canvas.saveLayer(
            getChartMainDisplayArea().left,
            getChartDisplayArea().top,
            getChartMainDisplayArea().right,
            getChartDisplayArea().bottom,
            null
        )
        //设置指标线条的宽度，来自配置 chartConfig.indexStrokeWidth
        indexPaint.strokeWidth = chartConfig.indexStrokeWidth

        //指标数据的列表，可能包含多条指标线（如多条均线）。
        //lineIdx：当前绘制的指标线索引。
        //indexColors：配置的指标线颜色数组。
        //indexPaint.color = indexColors[lineIdx]：根据当前指标线的索引设置颜色。
        indexList?.forEachIndexed { lineIdx, pointList ->
            chartConfig.indexColors.let { indexColors ->
                if (lineIdx < indexColors.size) {
                    indexPaint.color = indexColors[lineIdx]
                    //preIdx = -1：初始化上一个有效点的索引，用于绘制连续线段。
                    var preIdx = -1
                    //pointList.forEachIndexed：遍历当前指标线的每个数据点。
                    pointList.forEachIndexed { pointIdx, point ->
                        //if (point == null)：如果当前点为空，则跳过绘制，并重置 preIdx，避免绘制无效的线段。
                        if (point == null) {
                            preIdx = -1
                            return@forEachIndexed
                        }
                        //如果 preIdx 是初始值，表示这是第一个有效点，保存索引但不绘制线段。
                        if (preIdx == -1) {
                            preIdx = pointIdx
                            return@forEachIndexed
                        }
                        //将逻辑坐标转换为实际的屏幕坐标。
                        tmp4FloatArray[0] = preIdx + 0.5f
                        tmp4FloatArray[1] = pointList[preIdx]!!
                        tmp4FloatArray[2] = pointIdx + 0.5f
                        tmp4FloatArray[3] = pointList[pointIdx]!!
                        mapPointsValue2Real(tmp4FloatArray)
                        //绘制从前一个点到当前点的线段，形成指标线。
                        canvas.drawLine(
                            tmp4FloatArray[0],
                            tmp4FloatArray[1],
                            tmp4FloatArray[2],
                            tmp4FloatArray[3],
                            indexPaint
                        )
                        //更新 preIdx 为当前点索引，为下一个线段的绘制做准备。
                        preIdx = pointIdx
                    }
                }
            }
        }
        //恢复画布到调用 saveLayer 之前的状态，确保接下来的绘制不受影响。
        canvas.restoreToCount(saveCount)

        // draw index text
        // 绘制指标文本
        chartConfig.index?.let { index -> //获取当前的指标配置，包含文本样式和格式化信息。
            indexList?.let { indexList ->
                //获取当前高亮的点，通常是用户点击或长按选中的数据点。
                val highlight = getHighlight()
                //  确定要显示数据的索引。如果有高亮点，使用高亮点的索引，否则使用显示区域内的最后一个非空数据点
                var indexIdx =
                    highlight?.getIdx() ?: stockChart.findLastNotEmptyKEntityIdxInDisplayArea()

                // 设置文本大小。
                indexTextPaint.textSize = index.textSize
                //left 和 top：文本绘制的初始左边距和上边距。
                var left = index.textMarginLeft
                var top = index.textMarginTop
                //计算文本的高度，用于确定行高和换行。
                indexTextPaint.getFontMetrics(tmpFontMetrics)
                val textHeight = tmpFontMetrics.bottom - tmpFontMetrics.top

                //index.startText：绘制指标的起始文本（例如，指标名称）
                if (!index.startText.isNullOrEmpty()) {
                    indexTextPaint.color = index.startTextColor
                    canvas.drawText(
                        index.startText,
                        left,
                        -tmpFontMetrics.top + top,
                        indexTextPaint
                    )
                    //更新左边距，为下一个文本留出空间。
                    left += indexTextPaint.measureText(index.startText) + index.textSpace
                    //记录已绘制文本的高度。
                    drawnIndexTextHeight = textHeight + index.textMarginTop
                }

                //绘制每条指标的数值文本
                var isFirstLine = true
                indexList.forEachIndexed { lineIdx, pointList ->
                    chartConfig.indexColors.let { indexColors ->
                        if (lineIdx < indexColors.size) {
                            indexTextPaint.color = indexColors[lineIdx]
                            //获取当前指标在 indexIdx 位置的值。
                            val value =
                                if (indexIdx != null && indexIdx in pointList.indices && pointList[indexIdx] != null) pointList[indexIdx] else null
                            val text = index.textFormatter.invoke(lineIdx, value)
                            val textWidth = indexTextPaint.measureText(text)
                            //判断文本是否超出绘制区域宽度，如果超出则换行。
                            if (left + textWidth > getChartDisplayArea().width()) {
                                // 需要换行
                                isFirstLine = false
                                left = index.textMarginLeft
                                top += textHeight
                                drawnIndexTextHeight += textHeight
                            }

                            if (isFirstLine) {
                                drawnIndexTextHeight = textHeight + index.textMarginTop
                            }
                            //绘制文本到画布上。
                            canvas.drawText(
                                text,
                                left,
                                -tmpFontMetrics.top + top,
                                indexTextPaint
                            )
                            //更新左边距，准备绘制下一个文本。
                            left += indexTextPaint.measureText(text) + index.textSpace
                        }
                    }
                }
            }
        }
    }

    private fun drawMountainKChart(canvas: Canvas) {
        val saveCount = canvas.saveLayer(
            getChartMainDisplayArea().left,
            getChartDisplayArea().top,
            getChartMainDisplayArea().right,
            getChartDisplayArea().bottom,
            null
        )

        mountainKChartPaint.strokeWidth = chartConfig.mountainChartStrokeWidth
        mountainKChartPaint.color = chartConfig.mountainChartColor

        if (!mountainLinearGradientColors.contentEquals(chartConfig.mountainChartLinearGradientColors)) {
            setMountainLinearGradient()
        }

        mountainGradientKChartPaint.shader = mountainLinearGradient
        tmpPath.reset()

        tmp2FloatArray[1] = getChartDisplayArea().bottom
        mapPointsReal2Value(tmp2FloatArray)
        val yMinValue = tmp2FloatArray[1]

        var preIdx = -1
        for (idx in getKEntities().indices) {
            if (getKEntities()[idx].containFlag(FLAG_EMPTY) || getKEntities()[idx].containFlag(
                    FLAG_LINE_STARTER
                )
            ) {
                if (preIdx != -1) {
                    tmpPath.lineTo(preIdx + 1f, getKEntities()[preIdx].getClosePrice())
                    tmpPath.lineTo(preIdx + 1f, yMinValue)
                    mapPathValue2Real(tmpPath)
                    canvas.drawPath(tmpPath, mountainGradientKChartPaint)
                    tmpPath.reset()
                }
                preIdx = -1
                if (getKEntities()[idx].containFlag(FLAG_EMPTY)) {
                    continue
                }
            }
            if (preIdx == -1) {
                preIdx = idx
                tmpPath.reset()
                tmpPath.moveTo(preIdx.toFloat(), yMinValue)
                tmpPath.lineTo(preIdx.toFloat(), getKEntities()[preIdx].getClosePrice())
                tmpPath.lineTo(preIdx + 0.5f, getKEntities()[preIdx].getClosePrice())


            } else {
                preIdx = idx
            }

            tmpPath.lineTo(idx + 0.5f, getKEntities()[idx].getClosePrice())
        }

        if (preIdx != -1) {
            tmpPath.lineTo(preIdx + 1f, getKEntities()[preIdx].getClosePrice())
            tmpPath.lineTo(preIdx + 1f, yMinValue)
            mapPathValue2Real(tmpPath)
            canvas.drawPath(tmpPath, mountainGradientKChartPaint)
            tmpPath.reset()
        }

        preIdx = -1
        for (idx in getKEntities().indices) {
            if (getKEntities()[idx].containFlag(FLAG_EMPTY)) {
                preIdx = -1
                continue
            }

            if (preIdx == -1 || getKEntities()[idx].containFlag(FLAG_LINE_STARTER)) {
                preIdx = idx
                continue
            }

            tmp4FloatArray[0] = preIdx + 0.5f
            tmp4FloatArray[1] = getKEntities()[preIdx].getClosePrice()
            tmp4FloatArray[2] = idx + 0.5f
            tmp4FloatArray[3] = getKEntities()[idx].getClosePrice()
            mapPointsValue2Real(tmp4FloatArray)
            canvas.drawLine(
                tmp4FloatArray[0],
                tmp4FloatArray[1],
                tmp4FloatArray[2],
                tmp4FloatArray[3],
                mountainKChartPaint
            )
            preIdx = idx
        }
        canvas.restoreToCount(saveCount)
    }

    private fun needDrawAvgPriceLine() =
        chartConfig.showAvgLine && (chartConfig.kChartType is KChartConfig.KChartType.LINE || chartConfig.kChartType is KChartConfig.KChartType.MOUNTAIN)

    private fun drawAvgPriceLine(canvas: Canvas) {
        if (needDrawAvgPriceLine()) {

            val saveCount = canvas.saveLayer(
                getChartMainDisplayArea().left,
                getChartDisplayArea().top,
                getChartMainDisplayArea().right,
                getChartDisplayArea().bottom,
                null
            )

            avgPriceLinePaint.strokeWidth = chartConfig.avgLineStrokeWidth
            avgPriceLinePaint.color = chartConfig.avgLineColor
            var preAvgIdx = -1
            for (idx in getKEntities().indices) {

                if (getKEntities()[idx].containFlag(FLAG_EMPTY) || getKEntities()[idx].getAvgPrice() == null) {
                    preAvgIdx = -1
                    continue
                }

                if (preAvgIdx == -1 || getKEntities()[idx].containFlag(FLAG_LINE_STARTER)) {
                    preAvgIdx = idx
                    continue
                }

                tmp4FloatArray[0] = preAvgIdx + 0.5f
                tmp4FloatArray[1] = getKEntities()[preAvgIdx].getAvgPrice()!!
                tmp4FloatArray[2] = idx + 0.5f
                tmp4FloatArray[3] = getKEntities()[idx].getAvgPrice()!!
                mapPointsValue2Real(tmp4FloatArray)
                canvas.drawLine(
                    tmp4FloatArray[0],
                    tmp4FloatArray[1],
                    tmp4FloatArray[2],
                    tmp4FloatArray[3],
                    avgPriceLinePaint
                )
                preAvgIdx = idx
            }
            canvas.restoreToCount(saveCount)
        }
    }

    private fun drawBarKChart(canvas: Canvas) {
        val saveCount = canvas.saveLayer(
            getChartMainDisplayArea().left,
            getChartDisplayArea().top,
            getChartMainDisplayArea().right,
            getChartDisplayArea().bottom,
            null
        )

        barKChartPaint.strokeWidth = chartConfig.barChartLineStrokeWidth
        val barWidth = 1 * (1 - chartConfig.barSpaceRatio)
        val spaceWidth = 1 * chartConfig.barSpaceRatio
        var left = spaceWidth / 2f
        getKEntities().forEachIndexed { idx, kEntity ->
            if (!kEntity.containFlag(FLAG_EMPTY)) {
                barKChartPaint.color =
                    if (isRise(idx)) stockChart.getConfig().riseColor else stockChart.getConfig().downColor

                tmp12FloatArray[0] = left + barWidth / 2
                tmp12FloatArray[1] = kEntity.getHighPrice()
                tmp12FloatArray[2] = left + barWidth / 2
                tmp12FloatArray[3] = kEntity.getLowPrice()

                tmp12FloatArray[4] = left
                tmp12FloatArray[5] = kEntity.getOpenPrice()
                tmp12FloatArray[6] = left + barWidth / 2
                tmp12FloatArray[7] = kEntity.getOpenPrice()

                tmp12FloatArray[8] = left + barWidth / 2
                tmp12FloatArray[9] = kEntity.getClosePrice()
                tmp12FloatArray[10] = left + barWidth
                tmp12FloatArray[11] = kEntity.getClosePrice()

                mapPointsValue2Real(tmp12FloatArray)

                canvas.drawLines(tmp12FloatArray, barKChartPaint)
            }
            left += barWidth + spaceWidth
        }
        canvas.restoreToCount(saveCount)
    }

    private fun drawHollowKChart(canvas: Canvas) {
        val saveCount = canvas.saveLayer(
            getChartMainDisplayArea().left,
            getChartDisplayArea().top,
            getChartMainDisplayArea().right,
            getChartDisplayArea().bottom,
            null
        )

        hollowKChartPaint.strokeWidth = chartConfig.hollowChartLineStrokeWidth
        val barWidth = 1 * (1 - chartConfig.barSpaceRatio)
        val spaceWidth = 1 * chartConfig.barSpaceRatio
        var left = spaceWidth / 2f
        getKEntities().forEachIndexed { idx, kEntity ->
            if (!kEntity.containFlag(FLAG_EMPTY)) {
                hollowKChartPaint.color =
                    if (isRise(idx)) stockChart.getConfig().riseColor else stockChart.getConfig().downColor

                tmp4FloatArray[0] = left + barWidth / 2
                tmp4FloatArray[1] = kEntity.getHighPrice()
                tmp4FloatArray[2] = tmp4FloatArray[0]
                tmp4FloatArray[3] = max(kEntity.getOpenPrice(), kEntity.getClosePrice())
                mapPointsValue2Real(tmp4FloatArray)
                canvas.drawLines(tmp4FloatArray, hollowKChartPaint)

                tmp4FloatArray[0] = left + barWidth / 2
                tmp4FloatArray[1] = kEntity.getLowPrice()
                tmp4FloatArray[2] = tmp4FloatArray[0]
                tmp4FloatArray[3] = min(kEntity.getOpenPrice(), kEntity.getClosePrice())
                mapPointsValue2Real(tmp4FloatArray)
                canvas.drawLines(tmp4FloatArray, hollowKChartPaint)

                tmpRectF.left = left
                tmpRectF.top = kEntity.getOpenPrice()
                tmpRectF.right = left + barWidth
                tmpRectF.bottom = kEntity.getClosePrice()
                mapRectValue2Real(tmpRectF)
                hollowKChartPaint.style = if (kEntity.getClosePrice() >= kEntity.getOpenPrice()) {
                    // 空心阳线
                    Paint.Style.STROKE
                } else {
                    Paint.Style.FILL
                }
                canvas.drawRect(tmpRectF, hollowKChartPaint)
            }
            left += barWidth + spaceWidth
        }
        canvas.restoreToCount(saveCount)
    }

    private fun drawCandleKChart(canvas: Canvas) {
        val saveCount = canvas.saveLayer(
            getChartMainDisplayArea().left,
            getChartDisplayArea().top,
            getChartMainDisplayArea().right,
            getChartDisplayArea().bottom,
            null
        )

        candleKChartPaint.strokeWidth = chartConfig.candleChartLineStrokeWidth

        val barWidth = 1 * (1 - chartConfig.barSpaceRatio)
        val spaceWidth = 1 * chartConfig.barSpaceRatio
        var left = spaceWidth / 2f
        getKEntities().forEachIndexed { idx, kEntity ->
            if (!kEntity.containFlag(FLAG_EMPTY)) {
                candleKChartPaint.color =
                    if (isRise(idx)) stockChart.getConfig().riseColor else stockChart.getConfig().downColor
                candleKChartPaint.color = candleKChartPaint.color
                tmp4FloatArray[0] = left + barWidth / 2
                tmp4FloatArray[1] = kEntity.getHighPrice()
                tmp4FloatArray[2] = tmp4FloatArray[0]
                tmp4FloatArray[3] = kEntity.getLowPrice()
                mapPointsValue2Real(tmp4FloatArray)
                canvas.drawLines(tmp4FloatArray, candleKChartPaint)
                tmpRectF.left = left
                tmpRectF.top = kEntity.getOpenPrice()
                tmpRectF.right = left + barWidth
                tmpRectF.bottom = kEntity.getClosePrice()
                mapRectValue2Real(tmpRectF)
                candleKChartPaint.style =
                    if (tmpRectF.height() == 0f) Paint.Style.STROKE else Paint.Style.FILL
                canvas.drawRect(tmpRectF, candleKChartPaint)
            }
            left += barWidth + spaceWidth
        }

        canvas.restoreToCount(saveCount)
    }

    //绘制K线分时线，折线图
    private fun drawLineKChart(canvas: Canvas) {
        //离屏缓冲区
        val saveCount = canvas.saveLayer(
            getChartMainDisplayArea().left,
            getChartDisplayArea().top,
            getChartMainDisplayArea().right,
            getChartDisplayArea().bottom,
            null
        )
        //折线图(分时线)才会用到
        lineKChartLinePaint.strokeWidth = chartConfig.lineChartStrokeWidth
        lineKChartLinePaint.color = chartConfig.lineChartColor

        var preIdx = -1 //用于记录前一个有效的索引，用于绘制两点之间的连线。
        for (idx in getKEntities().indices) {
            //返回 K 线数据的列表，containFlag(FLAG_EMPTY) 检查当前数据是否为空数据（如无效的点），containFlag(FLAG_LINE_STARTER) 判断是否为折线图的起始点。
            if (getKEntities()[idx].containFlag(FLAG_EMPTY)) {
                preIdx = -1
                continue
            }

            if (preIdx == -1 || getKEntities()[idx].containFlag(FLAG_LINE_STARTER)) {
                preIdx = idx
                continue
            }

            tmp4FloatArray[0] = preIdx + 0.5f
            tmp4FloatArray[1] = getKEntities()[preIdx].getClosePrice()
            tmp4FloatArray[2] = idx + 0.5f
            tmp4FloatArray[3] = getKEntities()[idx].getClosePrice()
            mapPointsValue2Real(tmp4FloatArray)
            canvas.drawLine(
                tmp4FloatArray[0],
                tmp4FloatArray[1],
                tmp4FloatArray[2],
                tmp4FloatArray[3],
                lineKChartLinePaint
            )
            preIdx = idx
        }
        canvas.restoreToCount(saveCount)
    }

    private fun drawLabels(canvas: Canvas) {
        chartConfig.leftLabelConfig?.let { config ->
            doDrawLabel(canvas, true, config)
        }

        chartConfig.rightLabelConfig?.let { config ->
            doDrawLabel(canvas, false, config)
        }
    }

    private fun doDrawLabel(canvas: Canvas, isLeft: Boolean, config: KChartConfig.LabelConfig) {
        if (config.count > 0) {
            labelPaint.textSize = config.textSize
            labelPaint.color = config.textColor
            labelPaint.getFontMetrics(tmpFontMetrics)
            val labelHeight = tmpFontMetrics.bottom - tmpFontMetrics.top
            val areaTop =
                getChartDisplayArea().top + drawnIndexTextHeight + config.marginTop
            val areaBottom = getChartDisplayArea().bottom - config.marginBottom
            var verticalSpace = 0f
            if (config.count > 1) {
                verticalSpace =
                    (areaBottom - areaTop - config.count * labelHeight) / (config.count - 1)
            }
            var pos = areaTop
            for (i in 1..config.count) {
                tmp2FloatArray[0] = 0f
                tmp2FloatArray[1] = pos + labelHeight / 2
                mapPointsReal2Value(tmp2FloatArray)
                val text = config.formatter.invoke(tmp2FloatArray[1])
                val startX = if (isLeft) {
                    config.horizontalMargin
                } else {
                    getChartDisplayArea().right - config.horizontalMargin - labelPaint.measureText(
                        text
                    )
                }
                canvas.drawText(text, startX, pos - tmpFontMetrics.top, labelPaint)
                pos += verticalSpace + labelHeight
            }
        }
    }
}