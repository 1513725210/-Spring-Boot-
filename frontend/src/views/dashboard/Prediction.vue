<template>
  <div class="dashboard-container">
    <!-- 顶部标题栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <el-button type="primary" plain size="small" @click="router.push('/dashboard')" style="background: transparent; color: var(--accent); border-color: var(--accent)">
          返回大屏
        </el-button>
      </div>
      <h1 class="dashboard-title">客流趋势精准预测系统 (ARIMA)</h1>
      <div class="header-time">{{ currentTime }}</div>
    </header>

    <!-- 主体内容 -->
    <main class="dashboard-body" style="display: block; padding: 24px;">
      <div class="panel" style="height: 100%;">
        <div class="panel-header" style="font-size: 18px; padding: 20px;">
          <span class="icon" style="font-size: 24px; margin-right: 12px;">📈</span>
          <span style="font-weight: bold; color: #ffffff">客流量时序预测分析</span>
          
          <div style="margin-left: auto; display: flex; align-items: center; gap: 16px;">
            <span style="font-size: 14px; color: var(--text-primary);">选择分析景区：</span>
            <el-select v-model="selectedScenicForTrend" placeholder="选择景区进行预测" @change="fetchTrendData" style="width: 240px">
              <el-option
                v-for="spot in spots"
                :key="spot.id"
                :label="spot.name"
                :value="spot.id"
              />
            </el-select>
          </div>
        </div>
        <div class="panel-body" style="padding: 24px;">
          <div ref="trendChartRef" class="trend-container" style="height: 100%; min-height: 600px"></div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, shallowRef } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi, predictApi } from '@/api'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const currentTime = ref('')
const spots = ref([])
const selectedScenicForTrend = ref(null)
const timeInterval = ref(null)

const trendChartRef = ref(null)
const trendChart = shallowRef(null)

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  })
}

const fetchSpots = async () => {
  try {
    const res = await dashboardApi.overview()
    if (res.code === 200 && res.data && res.data.spots) {
      spots.value = res.data.spots
      if (spots.value.length > 0) {
        selectedScenicForTrend.value = spots.value[0].id
        fetchTrendData()
      }
    }
  } catch (error) {
    console.error('获取列表失败', error)
  }
}

const fetchTrendData = async () => {
  if (!selectedScenicForTrend.value || !trendChart.value) return
  
  trendChart.value.showLoading({
    text: '调用 Python ARIMA 模型中...',
    color: 'var(--accent)',
    textColor: '#ffffff',
    maskColor: 'rgba(15, 32, 66, 0.8)'
  })
  
  try {
    const res = await predictApi.predict(selectedScenicForTrend.value, 12)
    
    if (res.code === 200 && res.data && res.data.predictions) {
      const { predictions, confidence_lower, confidence_upper } = res.data
      
      const now = new Date()
      const xAxisData = []
      for (let i = 1; i <= predictions.length; i++) {
        const d = new Date(now.getTime() + i * 5 * 60000)
        xAxisData.push(`${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`)
      }
      
      renderTrendChart(xAxisData, predictions, confidence_lower, confidence_upper)
    } else {
      ElMessage.warning(res.message || '获取预测数据失败')
      trendChart.value.hideLoading()
    }
  } catch (error) {
    console.error('获取预测数据失败', error)
    trendChart.value.hideLoading()
  }
}

const renderTrendChart = (xAxisData, predictions, lower, upper) => {
  if (!trendChart.value) return
  
  trendChart.value.hideLoading()
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 32, 66, 0.9)',
      borderColor: 'var(--border-color)',
      textStyle: { color: '#ffffff', fontSize: 16 },
      formatter: function (params) {
        let html = `${params[0].axisValue}<br/>`
        html += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:${params[0].color};"></span>`
        html += `预测客流: <b style="color: var(--accent); font-size: 18px;">${params[0].value}</b> 人<br/>`
        if (lower && upper) {
          html += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:rgba(0, 212, 255, 0.2);"></span>`
          html += `置信区间: ${lower[params[0].dataIndex]} ~ ${upper[params[0].dataIndex]}`
        }
        return html
      }
    },
    grid: { top: 40, right: 30, bottom: 40, left: 60, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData,
      axisLabel: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.3)', width: 2 } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#ffffff', fontSize: 14, fontWeight: 'bold' },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)', type: 'dashed' } }
    },
    series: [
      {
        name: 'L',
        type: 'line',
        data: lower || predictions.map(v => v * 0.9),
        lineStyle: { opacity: 0 },
        stack: 'confidence-band',
        symbol: 'none'
      },
      {
        name: 'U',
        type: 'line',
        data: (upper && lower) ? upper.map((u, i) => u - lower[i]) : predictions.map(v => v * 0.2),
        lineStyle: { opacity: 0 },
        areaStyle: { color: 'rgba(0, 212, 255, 0.2)' },
        stack: 'confidence-band',
        symbol: 'none'
      },
      {
        name: '预测值',
        type: 'line',
        data: predictions,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: 'var(--accent)' },
        lineStyle: { width: 4, shadowColor: 'rgba(0, 212, 255, 0.8)', shadowBlur: 15 }
      }
    ]
  }
  
  trendChart.value.setOption(option)
}

onMounted(() => {
  updateTime()
  timeInterval.value = setInterval(updateTime, 1000)
  
  nextTick(() => {
    if (trendChartRef.value) {
      trendChart.value = echarts.init(trendChartRef.value)
    }
    fetchSpots()
    window.addEventListener('resize', handleResize)
  })
})

const handleResize = () => {
  if (trendChart.value) trendChart.value.resize()
}

onUnmounted(() => {
  clearInterval(timeInterval.value)
  window.removeEventListener('resize', handleResize)
  if (trendChart.value) trendChart.value.dispose()
})
</script>

<style scoped>
</style>
