<template>
  <div class="dashboard-container">
    <!-- 顶部标题栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="online-badge">
          <div class="online-dot" :class="wsStatus === 'connected' ? '' : 'disconnected'"></div>
          {{ wsStatus === 'connected' ? '系统已连接' : '连接断开' }}
        </div>
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
                   width: Math.min(item.congestionRate, 100) + '%',
                   background: getCongestionColor(item.congestionRate)
                 }"></div>
              </div>
              <div class="ranking-percent" :style="{ color: getCongestionColor(item.congestionRate) }">
                {{ Number(item.congestionRate).toFixed(1) }}%
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
            <div v-for="log in warningLogs" :key="log.id || log.timestamp" class="warning-item" :class="'level-' + log.level">
              <div class="warning-badge" :class="log.level">{{ log.level === 'RED' ? '红色' : '黄色' }}</div>
              <div class="warning-content">
                <div style="font-weight: 600; margin-bottom: 4px; color: var(--text-primary)">{{ log.scenicName || log.scenic_name }}</div>
                <div>当前客流 {{ log.currentCount || log.current_count }} 人，拥挤度 <span :style="{color: log.level === 'RED' ? 'var(--warning-red)' : 'var(--warning-yellow)'}">{{ log.congestionRate || log.congestion_rate }}%</span></div>
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
      warningLogs.value = warningRes.data
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
    const res = await predictApi.predict(selectedScenicForTrend.value, 12)
    
    if (res.code === 200 && res.data && res.data.predictions && res.data.predictions.length > 0) {
      isTrendDataEmpty.value = false
      const { predictions, confidence_lower, confidence_upper } = res.data
      
      // 生成时间 X 轴 (假设预测未来1小时，每5分钟一个点)
      const now = new Date()
      const xAxisData = []
      for (let i = 1; i <= predictions.length; i++) {
        const d = new Date(now.getTime() + i * 5 * 60000)
        xAxisData.push(`${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`)
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
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
      formatter: function (params) {
        let html = `${params[0].axisValue}<br/>`
        html += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:${params[0].color};"></span>`
        html += `预测客流: <b>${params[0].value}</b>人<br/>`
        if (lower && upper) {
          html += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:rgba(0, 0, 0, 0.1);"></span>`
          html += `置信区间: ${lower[params[0].dataIndex]} ~ ${upper[params[0].dataIndex]}`
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
      // 预测线
      {
        name: '预测值',
        type: 'line',
        data: predictions,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#1890ff' },
        lineStyle: { color: '#1890ff', width: 2, shadowColor: 'rgba(24, 144, 255, 0.3)', shadowBlur: 10 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24, 144, 255, 0.5)' },
            { offset: 1, color: 'rgba(24, 144, 255, 0.1)' }
          ])
        }
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
 * 初始化地图
 */
const initMapChart = () => {
  if (!mapChartRef.value) return
  
  mapChart.value = echarts.init(mapChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
      formatter: function(params) {
        if (params.componentSubType === 'effectScatter' || params.componentSubType === 'scatter') {
          const data = params.data
          return `
            <div style="padding: 4px;">
              <div style="font-weight: bold; margin-bottom: 8px; border-bottom: 1px solid rgba(0,0,0,0.1); padding-bottom: 4px; color: var(--primary-dark);">
                ${data.name}
              </div>
              <div>当前客流：<span style="color: var(--accent); font-weight: bold;">${data.currentCount}</span> 人</div>
              <div>最大承载：${data.maxCapacity} 人</div>
              <div>拥挤度：<span style="color: ${getCongestionColor(data.congestionRate)}; font-weight: bold;">${data.congestionRate}%</span></div>
            </div>
          `
        }
        return params.name
      }
    },
    geo: {
      map: 'hubei',
      roam: true,
      zoom: 1.2,
      center: [112.2, 31],
      label: {
        show: true,
        color: 'var(--text-secondary)',
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
        label: { show: true, color: '#1f1f1f' },
        itemStyle: {
          areaColor: '#bae0ff',
          borderColor: '#1890ff'
        }
      }
    },
    series: [
      {
        name: '景区热力',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: [],
        symbolSize: function (val) {
          // 方案B: 缩小散点大小，减轻重叠 (尺寸 5-15)
          const rate = val[2]
          return Math.max(5, Math.min(15, rate / 6))
        },
        showEffectOn: 'render',
        rippleEffect: {
          brushType: 'stroke',
          scale: 2.5
        },
        itemStyle: {
          color: function(params) {
            // 继续使用交通灯红黄绿来表示拥挤度危险等级
            return getCongestionColor(params.data.congestionRate)
          },
          opacity: 0.9,
          shadowBlur: 5,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        },
        zlevel: 1
      }
    ]
  }
  
  mapChart.value.setOption(option)
}

/**
 * 更新地图数据
 */
const updateMapData = () => {
  if (!mapChart.value || !overview.value.spots) return
  
  const scatterData = overview.value.spots.map(spot => ({
    name: spot.name,
    value: [spot.longitude, spot.latitude, spot.congestionRate],
    id: spot.id,
    currentCount: spot.currentCount,
    maxCapacity: spot.maxCapacity,
    congestionRate: spot.congestionRate,
    status: spot.status
  }))
  
  mapChart.value.setOption({
    series: [{ data: scatterData }]
  })
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
      
      // 更新地图数据
      if (mapChart.value) {
        const scatterData = newSpots.map(spot => {
          totalCurrent += spot.currentCount
          if (spot.congestionRate >= 80) warningCnt++
          
          return {
            name: spot.scenicName,
            value: [spot.longitude, spot.latitude, spot.currentCount],
            id: spot.scenicId,
            currentCount: spot.currentCount,
            maxCapacity: spot.maxCapacity,
            congestionRate: spot.congestionRate
          }
        })
        
        mapChart.value.setOption({ series: [{ data: scatterData }] })
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
    const log = message
    
    // 添加到日志列表头部，保持最多20条
    warningLogs.value.unshift(log)
    if (warningLogs.value.length > 20) {
      warningLogs.value.pop()
    }
    
    // 弹窗提示
    const alertId = Date.now().toString()
    
    // 如果是红色预警或存在生成的计划，使用返回的完整 message (可能包含 RAG HTML 计划)
    let displayMsg = log.message || `当前拥挤度达到 <strong style="color: ${log.level === 'RED' ? 'var(--warning-red)' : 'var(--warning-yellow)'}">${log.congestionRate}%</strong>，请安排疏导。`;
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
