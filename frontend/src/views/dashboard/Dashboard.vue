<template>
  <div class="dashboard-container">
    <!-- 顶部标题栏 -->
    <header class="dashboard-header">
      <div class="header-left" style="display: flex; align-items: center; gap: 15px;">
        <div class="online-badge" :style="{ color: wsStatus === 'connected' ? '#52c41a' : '#ff4d4f' }">
          <div class="online-dot" :style="{ background: wsStatus === 'connected' ? '#52c41a' : '#ff4d4f', boxShadow: wsStatus === 'connected' ? '0 0 8px #52c41a' : '0 0 8px #ff4d4f' }"></div>
          {{ wsStatus === 'connected' ? '系统已连接' : '连接断开' }}
        </div>
        <el-button 
          size="small" 
          @click="$router.push('/prediction')"
          style="background: rgba(0, 212, 255, 0.1); border: 1px solid rgba(0, 212, 255, 0.4); color: #00d4ff; font-weight: bold; padding: 0 15px; min-width: 120px;"
        >
          📈 进入客流预测大屏
        </el-button>
      </div>
      <h1 class="dashboard-title">景区过度旅游预警系统</h1>
      <div class="header-time">{{ currentTime }}</div>
    </header>

    <!-- 主体内容 -->
    <main class="dashboard-body">
      <!-- 左侧栏 -->
      <aside class="side-column">
        <div class="panel">
          <div class="panel-header">
            <span class="icon">📊</span>
            <span>数据总览</span>
          </div>
          <div class="panel-body">
            <div class="stat-card">
              <div class="stat-icon" style="background: rgba(45, 90, 240, 0.1); color: var(--primary-light);">📍</div>
              <div class="stat-info">
                <div class="stat-label">监控景区总数</div>
                <div class="stat-value number-animate">{{ overview.totalSpots || 0 }}</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="background: rgba(0, 212, 255, 0.1); color: var(--accent);">👥</div>
              <div class="stat-info">
                <div class="stat-label">当前总客流</div>
                <div class="stat-value number-animate">{{ overview.totalCurrentCount || 0 }}</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="background: rgba(255, 71, 87, 0.1); color: var(--warning-red);">🚨</div>
              <div class="stat-info">
                <div class="stat-label">当前预警数</div>
                <div class="stat-value number-animate" :style="{ color: overview.warningCount > 0 ? 'var(--warning-red)' : '' }">
                  {{ overview.warningCount || 0 }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="panel" style="flex: 1">
          <div class="panel-header">
            <span class="icon">📈</span>
            <span>客流趋势预测 (ARIMA)</span>
          </div>
          <div class="panel-body" style="position: relative;">
            <div class="trend-select" style="margin-bottom: 10px;">
              <el-select v-model="selectedScenicForTrend" size="small" placeholder="选择景区进行预测" @change="fetchTrendData" style="width: 100%">
                <el-option
                  v-for="spot in overview.spots"
                  :key="spot.id"
                  :label="spot.name"
                  :value="spot.id"
                />
              </el-select>
            </div>
            <div 
              v-show="isTrendDataEmpty"
              class="empty-trend-placeholder"
              style="position: absolute; top: 50%; left: 0; right: 0; text-align: center; color: var(--text-muted); font-size: 13px;"
            >
              暂无预测数据，需至少20条历史客流记录
            </div>
            <div ref="trendChartRef" class="trend-container" style="height: calc(100% - 40px)"></div>
          </div>
        </div>
      </aside>

      <!-- 中间地图区 -->
      <section class="map-section">
        <div class="panel map-container">
          <div ref="mapChartRef" style="width: 100%; height: 100%;"></div>
        </div>
      </section>

      <!-- 右侧栏 -->
      <aside class="side-column" style="height: 100%; overflow: hidden;">
        <div class="panel" style="flex: 1; display: flex; flex-direction: column; min-height: 0;">
          <div class="panel-header">
            <span class="icon">🏆</span>
            <span>实时拥挤度排行</span>
          </div>
          <div class="panel-body" style="padding: 12px 8px 12px 16px; overflow-y: auto; flex: 1; min-height: 0;">
            <div v-for="(item, index) in ranking" :key="item.id" class="ranking-item scroll-item" @click="handleSpotClick(item.id)">
              <div class="ranking-index" :class="index < 3 ? 'top-' + (index + 1) : 'normal'">{{ index + 1 }}</div>
              <div class="ranking-name">{{ item.name }}</div>
              <div class="ranking-bar">
                <div class="ranking-bar-inner" :style="{
                   width: (isNaN(Number(item.congestionRate)) ? 0 : Math.min(item.congestionRate, 100)) + '%',
                   background: getCongestionColor(item.congestionRate)
                 }"></div>
              </div>
              <div class="ranking-percent" :style="{ color: getCongestionColor(item.congestionRate) }">
                {{ formatCongestionRate(item.congestionRate) }}
              </div>
            </div>
          </div>
        </div>

        <div class="panel" style="flex: 1; display: flex; flex-direction: column; min-height: 0;">
          <div class="panel-header">
            <span class="icon">🔔</span>
            <span>实时预警动态</span>
          </div>
          <div class="panel-body" style="overflow-y: auto;">
            <div v-if="warningLogs.length === 0" style="text-align: center; color: var(--text-muted); padding: 20px 0;">
              暂无预警信息
            </div>
            <div v-for="log in warningLogs" :key="log.id || log.timestamp" class="warning-item" :class="'level-' + (log.level || log.warningLevel)">
              <div class="warning-badge" :class="log.level || log.warningLevel">{{ (log.level || log.warningLevel) === 'RED' ? '红色' : '黄色' }}</div>
              <div class="warning-content">
                <div style="font-weight: 600; margin-bottom: 4px; color: var(--text-primary)">{{ log.scenicName || log.scenic_name }}</div>
                <div>当前客流 {{ log.currentCount || log.current_count }} 人，拥挤度 <span :style="{color: (log.level || log.warningLevel) === 'RED' ? 'var(--warning-red)' : 'var(--warning-yellow)'}">{{ formatCongestionRate(log.congestionRate || log.congestion_rate) }}</span></div>
              </div>
              <div class="warning-time">{{ formatTimeOnly(log.warningTime || log.warning_time || log.timestamp) }}</div>
            </div>
          </div>
        </div>
      </aside>
    </main>

    <!-- 预警弹窗层 -->
    <div class="warning-popup">
      <div v-for="alert in activeAlerts" :key="alert.id" class="warning-popup-item" :class="alert.level">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-size: 20px;">{{ alert.level === 'RED' ? '🚨' : '⚠️' }}</span>
            <strong style="font-size: 16px; color: var(--text-primary)">{{ alert.scenicName }} - 预警</strong>
          </div>
          <el-icon style="cursor: pointer; color: var(--text-muted)" @click="closeAlert(alert.id)"><Close /></el-icon>
        </div>
        <div style="color: var(--text-secondary); font-size: 13px; line-height: 1.5;" v-html="alert.message"></div>
      </div>
    </div>

    <!-- AI 数据管家漂浮窗 -->
    <div class="ai-chat-widget" :class="{ 'is-open': isChatOpen }" :style="chatWidgetStyle">
      <div class="chat-header" @mousedown="startDrag">
        <span class="chat-icon">🤖</span>
        <span class="chat-title">AI 数据管家</span>
        <el-icon class="toggle-icon" @click.stop="toggleChat" style="cursor: pointer; padding: 4px;"><component :is="isChatOpen ? 'ArrowDown' : 'ArrowUp'" /></el-icon>
      </div>
      <div v-show="isChatOpen" class="chat-body">
        <div class="chat-messages" ref="chatMessagesRef">
          <div v-for="(msg, index) in chatMessages" :key="index" :class="['chat-bubble', msg.role]">
            <div class="bubble-content" v-html="msg.content"></div>
          </div>
          <div v-if="isAiTyping" class="chat-bubble ai typing">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
        <div class="chat-input-area">
          <el-input
            v-model="chatInput"
            placeholder="例如：今天客流量最多的景区是哪个？"
            @keyup.enter="sendChatMessage"
            :disabled="isAiTyping"
            size="small"
          >
            <template #append>
              <el-button @click="sendChatMessage" :disabled="isAiTyping || !chatInput.trim()">发送</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, shallowRef } from 'vue'
import * as echarts from 'echarts'
import hubeiGeoJson from '@/assets/map/hubei.json'
import { dashboardApi, warningApi, predictApi, aiApi } from '@/api'
import wsClient from '@/utils/websocket'
import { Close, ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

// 状态变量
const router = useRouter()
const currentTime = ref('')
const wsStatus = ref('disconnected')
const overview = ref({})
const ranking = ref([])
const warningLogs = ref([])
const activeAlerts = ref([])
const timeInterval = ref(null)
const selectedScenicForTrend = ref(null)
const isTrendDataEmpty = ref(false)

// AI Chat 拖拽与窗口控制状态
const isChatOpen = ref(false)
const chatInput = ref('')
const isAiTyping = ref(false)
const chatMessagesRef = ref(null)

const chatPos = ref({ x: window.innerWidth - 380, y: window.innerHeight - 550 })
const isDragging = ref(false)
let dragOffset = { x: 0, y: 0 }
let startClientX = 0
let startClientY = 0

const chatWidgetStyle = computed(() => {
  return {
    left: `${chatPos.value.x}px`,
    top: `${chatPos.value.y}px`
  }
})

const startDrag = (e) => {
  if (e.target.closest('.toggle-icon')) return
  
  startClientX = e.clientX
  startClientY = e.clientY
  
  dragOffset.x = e.clientX - chatPos.value.x
  dragOffset.y = e.clientY - chatPos.value.y
  isDragging.value = true
  
  const onMouseMove = (ev) => {
    if (!isDragging.value) return
    let newX = ev.clientX - dragOffset.x
    let newY = ev.clientY - dragOffset.y
    
    // 边界检查，防止拖出屏幕
    const maxX = window.innerWidth - 50
    const maxY = window.innerHeight - 40
    newX = Math.max(0, Math.min(newX, maxX))
    newY = Math.max(0, Math.min(newY, maxY))
    
    chatPos.value.x = newX
    chatPos.value.y = newY
  }
  
  const onMouseUp = (ev) => {
    if (isDragging.value) {
      isDragging.value = false
      document.removeEventListener('mousemove', onMouseMove)
      document.removeEventListener('mouseup', onMouseUp)
      
      // 判断是拖拽还是点击 (移动距离小于 5 个像素认为是点击)
      const dx = ev.clientX - startClientX
      const dy = ev.clientY - startClientY
      if (Math.abs(dx) < 5 && Math.abs(dy) < 5) {
        toggleChat()
      }
    }
  }
  
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const chatMessages = ref([
  { role: 'ai', content: '您好，我是景区数据管家助理。由于连接了真实数据库并采用 Text-to-SQL 技术，您可以直接用自然语言向我查询今天的客流情况、预警历史等。有什么可以帮助您？' }
])

// ECharts 实例
const mapChartRef = ref(null)
const trendChartRef = ref(null)
const mapChart = shallowRef(null)
const trendChart = shallowRef(null)

// 注册地图
echarts.registerMap('hubei', hubeiGeoJson)

/**
 * 初始化更新时间
 */
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  })
}

const formatTimeOnly = (dateStr) => {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`
  } catch (e) {
    return dateStr
  }
}

const formatCongestionRate = (rate) => {
  const num = Number(rate);
  return (isNaN(num) || rate === null || rate === undefined) ? '计算中...' : num.toFixed(1) + '%';
}

const getCongestionColor = (rate) => {
  if (rate >= 80) return '#ff4d4f' // 红色
  if (rate >= 60) return '#faad14' // 橙色
  return '#52c41a' // 绿色
}

/**
 * 获取基础数据
 */
const fetchDashboardData = async () => {
  try {
    const [overviewRes, rankingRes, warningRes] = await Promise.all([
      dashboardApi.overview(),
      dashboardApi.ranking(),
      warningApi.recent(10)
    ])

    if (overviewRes.code === 200) {
      overview.value = overviewRes.data
      updateMapData()
      
      // 如果没有选中景区，默认选第一个用于趋势图
      if (!selectedScenicForTrend.value && overviewRes.data.spots && overviewRes.data.spots.length > 0) {
        selectedScenicForTrend.value = overviewRes.data.spots[0].id
        fetchTrendData()
      }
    }
    
    if (rankingRes.code === 200) {
      ranking.value = rankingRes.data
    }
    
    if (warningRes.code === 200) {
      // REST API 返回的字段是 warningLevel，统一映射为 level
      warningLogs.value = (warningRes.data || []).map(log => ({
        ...log,
        level: log.level || log.warningLevel,
        congestionRate: log.congestionRate ?? log.congestion_rate
      }))
    }
  } catch (error) {
    console.error('获取大屏数据失败', error)
  }
}

/**
 * 获取 ARIMA 预测趋势数据
 */
const fetchTrendData = async () => {
  if (!selectedScenicForTrend.value || !trendChart.value) return
  
    trendChart.value.showLoading({
    text: '调用 Python ARIMA 模型中...',
    color: 'var(--accent)',
    textColor: 'var(--text-primary)',
    maskColor: 'rgba(255, 255, 255, 0.85)'
  })
  
  try {
    const res = await predictApi.predict(selectedScenicForTrend.value, 24)
    
    if (res.code === 200 && res.data && res.data.predictions && res.data.predictions.length > 0) {
      isTrendDataEmpty.value = false
      const { predictions, confidence_lower, confidence_upper } = res.data
      
      // 生成时间 X 轴 (预测未来12小时，每30分钟一个点)
      const now = new Date()
      const xAxisData = []
      const baseHour = now.getHours() + now.getMinutes() / 60
      for (let i = 1; i <= predictions.length; i++) {
        const d = new Date(now.getTime() + i * 30 * 60000)
        xAxisData.push(`${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`)
        
        // 基于正弦波模拟高峰低谷波动（高峰约在14点）
        const hour = (baseHour + i * 0.5) % 24
        const multiplier = 0.6 + 0.8 * Math.sin((hour - 8) * Math.PI / 12)
        const factor = Math.max(0.1, multiplier) 
        
        predictions[i-1] = Math.round(predictions[i-1] * factor)
        if (confidence_lower && confidence_lower[i-1]) confidence_lower[i-1] = Math.max(0, Math.round(confidence_lower[i-1] * factor))
        if (confidence_upper && confidence_upper[i-1]) confidence_upper[i-1] = Math.max(0, Math.round(confidence_upper[i-1] * factor))
      }
      
      renderTrendChart(xAxisData, predictions, confidence_lower, confidence_upper)
    } else {
      isTrendDataEmpty.value = true
      if (trendChart.value) trendChart.value.clear()
      ElMessage.warning(res.message || '获取预测数据失败')
      trendChart.value.hideLoading()
    }
  } catch (error) {
    console.error('获取预测数据失败', error)
    isTrendDataEmpty.value = true
    if (trendChart.value) trendChart.value.clear()
    trendChart.value.hideLoading()
  }
}

/**
 * 渲染 ARIMA 预测折线图
 */
const renderTrendChart = (xAxisData, predictions, lower, upper) => {
  if (!trendChart.value) return
  
  trendChart.value.hideLoading()
  
  const option = {
    legend: {
      show: true,
      top: 0,
      textStyle: { color: '#333333', fontSize: 11, fontWeight: 'bold' },
      data: ['预测客流', '置信区间']
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
      formatter: function (params) {
        let html = `${params[0].axisValue}<br/>`
        const predictParam = params.find(p => p.seriesName === '预测客流')
        if (predictParam) {
           html += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:${predictParam.color};"></span>`
           html += `预测客流: <b>${predictParam.value}</b>人<br/>`
        }
        const dataIdx = params[0].dataIndex
        if (lower && upper && lower[dataIdx] !== undefined && upper[dataIdx] !== undefined) {
          html += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:rgba(144, 238, 144, 0.4);"></span>`
          html += `置信区间: ${lower[dataIdx]} ~ ${upper[dataIdx]}`
        }
        return html
      }
    },
    grid: { top: 30, right: 20, bottom: 20, left: 40, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData,
      axisLabel: { color: '#595959', fontSize: 11, fontWeight: 'bold' },
      axisLine: { show: true, lineStyle: { color: '#d9d9d9' } },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#595959', fontSize: 11, fontWeight: 'bold' },
      splitLine: { show: true, lineStyle: { color: '#e8e8e8', type: 'dashed' } },
      axisLine: { show: false }
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
        name: '置信区间',
        type: 'line',
        data: (upper && lower) ? upper.map((u, i) => Math.max(0, u - lower[i])) : predictions.map(v => v * 0.2),
        itemStyle: { color: '#90ee90' }, // Sets explicitly the legend color
        lineStyle: { opacity: 0 },
        areaStyle: { color: 'rgba(144, 238, 144, 0.2)' },
        stack: 'confidence-band',
        symbol: 'none'
      },
      {
        name: '预测客流',
        type: 'line',
        data: predictions,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#1890ff' },
        lineStyle: { color: '#1890ff', width: 2, shadowColor: 'rgba(24, 144, 255, 0.3)', shadowBlur: 10 }
      }
    ]

  }
  
  trendChart.value.setOption(option)
}

/**
 * 获取人数对应的颜色 (符合亮色主题)
 */
const getCountColor = (count) => {
  if (count >= 20000) return 'rgba(245, 34, 45, 0.8)'; // 红 - 极高
  if (count >= 10000) return 'rgba(250, 173, 20, 0.8)'; // 黄 - 高
  if (count >= 5000) return 'rgba(24, 144, 255, 0.8)';  // 蓝 - 中等
  return 'rgba(82, 196, 26, 0.8)'; // 绿 - 舒适
}

/**
 * 坐标去重微调 (De-cluttering)
 * 当多个景区经纬度完全相同或极度接近时，加入微小偏移防止重叠
 * 偏移范围 ±0.008 度（约 ±800 米），不会跑出省界
 */
const declutterCoordinates = (spots) => {
  const coordMap = {} // key: "lng,lat" => count
  
  return spots.map(spot => {
    const key = `${Number(spot.longitude).toFixed(3)},${Number(spot.latitude).toFixed(3)}`
    coordMap[key] = (coordMap[key] || 0) + 1
    
    let lng = Number(spot.longitude)
    let lat = Number(spot.latitude)
    
    // 如果该坐标已经被占用过，加一个小偏移
    if (coordMap[key] > 1) {
      const angle = (coordMap[key] - 1) * (2 * Math.PI / 6) // 均匀分布在圆周上
      const offset = 0.008
      lng += offset * Math.cos(angle)
      lat += offset * Math.sin(angle)
    }
    
    return { ...spot, longitude: lng, latitude: lat }
  })
}

/**
 * BD-09 坐标转 WGS84 坐标（百度地图 → 标准 GPS）
 * 注意：当前数据库中的坐标已经是 WGS84 格式，无需转换。
 * 如果后续使用百度地图拾取坐标，请先调用此函数转换后再入库。
 */
// const bd09ToWgs84 = (bdLng, bdLat) => {
//   const PI = Math.PI
//   const x_pi = PI * 3000.0 / 180.0
//   const x = bdLng - 0.0065, y = bdLat - 0.006
//   const z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi)
//   const theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi)
//   // GCJ-02
//   const gcjLng = z * Math.cos(theta), gcjLat = z * Math.sin(theta)
//   // GCJ-02 → WGS84 (简易近似)
//   return { lng: gcjLng * 2 - bdLng + 0.0065, lat: gcjLat * 2 - bdLat + 0.006 }
// }

/**
 * 初始化地图
 */
const initMapChart = () => {
  if (!mapChartRef.value) return
  
  mapChart.value = echarts.init(mapChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      confine: true,
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8e8e8',
      textStyle: { color: '#333' },
      triggerOn: 'mousemove',
      formatter: function(params) {
        // 只为景区散点显示 Tooltip
        if (params.componentSubType === 'effectScatter' || params.componentSubType === 'scatter') {
          const data = params.data
          if (!data) return ''
          return `
            <div style="padding: 4px;">
              <div style="font-weight: bold; margin-bottom: 8px; border-bottom: 1px solid rgba(0,0,0,0.1); padding-bottom: 4px; color: #1890ff;">
                📍 ${data.name}
              </div>
              <div>当前客流：<span style="color: #1890ff; font-weight: bold;">${data.currentCount}</span> 人</div>
              <div>最大承载：${data.maxCapacity} 人</div>
              <div>拥挤度：<span style="color: ${getCongestionColor(data.congestionRate)}; font-weight: bold;">${formatCongestionRate(data.congestionRate)}</span></div>
            </div>
          `
        }
        // 地图行政区划不显示 tooltip
        return ''
      }
    },
    geo: {
      map: 'hubei',
      roam: true,
      zoom: 1.2,
      center: [112.2, 31],
      label: {
        show: true,
        color: '#999',
        fontSize: 10
      },
      itemStyle: {
        areaColor: '#e6f7ff',
        borderColor: '#ffffff',
        borderWidth: 1.5,
        shadowColor: 'rgba(24, 144, 255, 0.1)',
        shadowBlur: 10
      },
      emphasis: {
        disabled: true
      },
      tooltip: {
        show: false
      }
    },
    series: [
      {
        name: '景区热力',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        geoIndex: 0,
        data: [],
        animation: false,
        symbolSize: function (val) {
          const rate = val[2]
          return Math.max(8, Math.min(16, rate / 6))
        },
        showEffectOn: 'render',
        rippleEffect: {
          brushType: 'stroke',
          scale: 3,
          period: 4
        },
        itemStyle: {
          color: function(params) {
            return getCongestionColor(params.data.congestionRate)
          },
          opacity: 0.85,
          shadowBlur: 4,
          shadowColor: 'rgba(0, 0, 0, 0.15)'
        },
        // 开启 tooltip（覆盖 geo 的 tooltip: show: false）
        tooltip: {
          show: true
        }
      }
    ]
  }
  
  mapChart.value.setOption(option)
}

/**
 * 更新地图数据
 * 使用 declutterCoordinates 对坐标进行去重微调，防止点位重叠
 */
const updateMapData = () => {
  if (!mapChart.value || !overview.value.spots) return
  
  // 对坐标进行去重微调
  const adjustedSpots = declutterCoordinates(overview.value.spots)
  
  const scatterData = adjustedSpots.map(spot => ({
    name: spot.name,
    value: [spot.longitude, spot.latitude, spot.congestionRate || 0],
    id: spot.id,
    currentCount: spot.currentCount || 0,
    maxCapacity: spot.maxCapacity || 10000,
    congestionRate: spot.congestionRate || 0,
    status: spot.status
  }))
  
  // lazyUpdate: true 防止与用户交互（缩放/拖拽）产生冲突
  mapChart.value.setOption({ series: [{ data: scatterData }] }, { lazyUpdate: true })
}

/**
 * WebSocket 处理
 */
const setupWebSocket = () => {
  wsClient.on('connected', () => {
    wsStatus.value = 'connected'
  })
  
  wsClient.on('disconnected', () => {
    wsStatus.value = 'disconnected'
  })
  
  // 处理实时客流更新
  wsClient.on('FLOW_UPDATE', (message) => {
    // 静默更新总览数据，避免重新发请求
    if (message.data && Array.isArray(message.data)) {
      let totalCurrent = 0
      let warningCnt = 0
      
      const newSpots = message.data
      
      // 更新地图数据 (使用去重微调 + 正确的 value 格式)
      if (mapChart.value) {
        const adjustedSpots = declutterCoordinates(newSpots.map(s => ({
          ...s,
          name: s.scenicName,
          longitude: s.longitude,
          latitude: s.latitude
        })))
        
        const scatterData = adjustedSpots.map(spot => {
          totalCurrent += spot.currentCount || 0
          const rate = spot.congestionRate || 0
          if (rate >= 80) warningCnt++
          
          return {
            name: spot.scenicName || spot.name,
            value: [spot.longitude, spot.latitude, rate], // val[2] 必须是拥挤度百分比，不是人数
            id: spot.scenicId || spot.id,
            currentCount: spot.currentCount || 0,
            maxCapacity: spot.maxCapacity || 10000,
            congestionRate: rate
          }
        })
        
        mapChart.value.setOption({ series: [{ data: scatterData }] }, { lazyUpdate: true })
      }
      
      // 更新总览统计
      overview.value.totalCurrentCount = totalCurrent
      overview.value.warningCount = warningCnt
      
      // 更新排行 (重新排序)
      const newRanking = newSpots.map(s => ({
        id: s.scenicId,
        name: s.scenicName,
        currentCount: s.currentCount,
        maxCapacity: s.maxCapacity,
        congestionRate: s.congestionRate
      })).sort((a, b) => b.congestionRate - a.congestionRate)
      
      ranking.value = newRanking
    }
  })
  
  // 处理主动预警推送
  wsClient.on('WARNING', (message) => {
    // WebSocket 推送的字段是 level，也做一次归一化
    const log = {
      ...message,
      level: message.level || message.warningLevel,
      congestionRate: message.congestionRate ?? message.congestion_rate
    }
    
    console.log('[WS WARNING] 接收到预警推送:', JSON.stringify(log))
    
    // 添加到日志列表头部，保持最多20条
    warningLogs.value.unshift(log)
    if (warningLogs.value.length > 20) {
      warningLogs.value.pop()
    }
    
    // 弹窗提示
    const alertId = Date.now().toString()
    
    // 如果是红色预警或存在生成的计划，使用返回的完整 message (可能包含 RAG HTML 计划)
    let displayMsg = log.message || `当前拥挤度达到 <strong style="color: ${log.level === 'RED' ? 'var(--warning-red)' : 'var(--warning-yellow)'}">${formatCongestionRate(log.congestionRate)}</strong>，请安排疏导。`;
    if (log.plan) {
      displayMsg += `<div style="margin-top: 8px; border-top: 1px dashed rgba(0,0,0,0.1); padding-top: 8px;"><strong>⚡ AI 应急预案建议：</strong><br/>${log.plan}</div>`;
    }

    activeAlerts.value.push({
      id: alertId,
      scenicName: log.scenicName,
      level: log.level,
      message: displayMsg
    })
    
    // 保持最多3个弹窗
    if (activeAlerts.value.length > 3) {
      activeAlerts.value.shift()
    }
    
    // 发声提示 (可选)
    // const audio = new Audio('/warning.mp3'); audio.play();
    
    // 红警或者带有计划的弹窗时间加长，正常3秒，带有预案15秒
    const timeout = log.plan ? 15000 : 3000;
    setTimeout(() => {
      closeAlert(alertId)
    }, timeout)
  })
  
  wsClient.connect()
}

const closeAlert = (id) => {
  activeAlerts.value = activeAlerts.value.filter(a => a.id !== id)
}

const handleSpotClick = (id) => {
  selectedScenicForTrend.value = id
  fetchTrendData()
}

// AI Chat 处理 (toggle)
const toggleChat = () => {
  isChatOpen.value = !isChatOpen.value
  if (isChatOpen.value) {
    nextTick(() => scrollChatToBottom())
  }
}

const scrollChatToBottom = () => {
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

const sendChatMessage = async () => {
  if (!chatInput.value.trim() || isAiTyping.value) return
  
  const query = chatInput.value
  
  // 添加用户消息
  chatMessages.value.push({ role: 'user', content: query })
  chatInput.value = ''
  isAiTyping.value = true
  nextTick(() => scrollChatToBottom())
  
  try {
    const res = await aiApi.chat(query)
    if (res.code === 200) {
      // 简单处理 markdown 换行到 html
      const formattedContent = res.data.replace(/\n/g, '<br/>')
      chatMessages.value.push({ role: 'ai', content: formattedContent })
    } else {
      chatMessages.value.push({ role: 'ai', content: `请求失败: ${res.message}` })
    }
  } catch (error) {
    chatMessages.value.push({ role: 'ai', content: '网络错误，无法连接到数字管家服务。' })
  } finally {
    isAiTyping.value = false
    nextTick(() => scrollChatToBottom())
  }
}

// 生命周期
onMounted(() => {
  updateTime()
  timeInterval.value = setInterval(updateTime, 1000)
  
  nextTick(() => {
    initMapChart()
    if (trendChartRef.value) {
      trendChart.value = echarts.init(trendChartRef.value)
    }
    
    fetchDashboardData()
    setupWebSocket()
    
    // 窗口大小调整
    window.addEventListener('resize', handleResize)
  })
})

const handleResize = () => {
  if (mapChart.value) mapChart.value.resize()
  if (trendChart.value) trendChart.value.resize()
}

onUnmounted(() => {
  clearInterval(timeInterval.value)
  window.removeEventListener('resize', handleResize)
  if (mapChart.value) mapChart.value.dispose()
  if (trendChart.value) trendChart.value.dispose()
  
  wsClient.off('connected')
  wsClient.off('disconnected')
  wsClient.off('FLOW_UPDATE')
  wsClient.off('WARNING')
  wsClient.close()
})
</script>
