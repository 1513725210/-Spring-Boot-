<template>
  <div>
    <!-- 筛选栏 -->
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;">
      <el-select v-model="filters.level" placeholder="预警等级" clearable style="width: 140px;" @change="fetchData">
        <el-option label="红色预警" value="RED" />
        <el-option label="黄色预警" value="YELLOW" />
      </el-select>
      <el-select v-model="filters.status" placeholder="处理状态" clearable style="width: 140px;" @change="fetchData">
        <el-option label="待处理" :value="0" />
        <el-option label="已处理" :value="1" />
      </el-select>
      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px;"
        @change="fetchData"
      />
      <el-button type="primary" @click="fetchData">查询</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="tableData"
      stripe
      style="width: 100%"
      :header-cell-style="{ background: 'var(--bg-dark)', color: 'var(--text-primary)', borderColor: 'var(--border-color)' }"
      :cell-style="{ borderColor: 'var(--border-color)' }"
    >
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="scenicName" label="景区" min-width="140" />
      <el-table-column label="预警等级" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.level === 'RED' ? 'danger' : 'warning'" size="small" effect="dark">
            {{ row.level === 'RED' ? '🚨 红色' : '⚠️ 黄色' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentCount" label="当时客流" width="100" align="center" />
      <el-table-column label="拥挤度" width="100" align="center">
        <template #default="{ row }">
          <span :style="{ color: row.level === 'RED' ? 'var(--warning-red)' : 'var(--warning-yellow)' }">
            {{ row.congestionRate }}%
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="预警信息" min-width="200" show-overflow-tooltip />
      <el-table-column label="预警时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.warningTime) }}
        </template>
      </el-table-column>
      <el-table-column label="处理状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.handleStatus === 1 ? 'success' : 'danger'" size="small">
            {{ row.handleStatus === 1 ? '已处理' : '待处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.handleStatus !== 1"
            type="success"
            link
            size="small"
            @click="handleProcess(row)"
          >
            处理
          </el-button>
          <span v-else style="color: var(--text-muted); font-size: 12px;">
            {{ row.handleUser }}
          </span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { warningApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])

const filters = reactive({
  level: '',
  status: '',
  dateRange: null
})

const formatDate = (str) => {
  if (!str) return '-'
  try {
    return new Date(str).toLocaleString('zh-CN', { hour12: false })
  } catch (e) {
    return str
  }
}

const fetchData = async () => {
  try {
    const params = {
      current: pageNum.value,
      size: pageSize.value
    }
    if (filters.level) params.level = filters.level
    if (filters.status !== '' && filters.status !== null) params.handleStatus = filters.status

    const res = await warningApi.page(params)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } catch (e) {
    console.error('获取预警日志失败', e)
  }
}

const handleProcess = async (row) => {
  try {
    const { value: remark } = await ElMessageBox.prompt('请输入处理备注：', `处理预警 - ${row.scenicName}`, {
      confirmButtonText: '确认处理',
      cancelButtonText: '取消',
      inputPlaceholder: '如：已派遣工作人员疏导'
    })
    const username = localStorage.getItem('username') || '管理员'
    const res = await warningApi.handle(row.id, username, remark || '已处理')
    if (res.code === 200) {
      ElMessage.success('预警已处理')
      fetchData()
    } else {
      ElMessage.error(res.message || '处理失败')
    }
  } catch (e) {
    // 用户取消
  }
}

onMounted(fetchData)
</script>
