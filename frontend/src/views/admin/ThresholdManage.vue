<template>
  <div>
    <el-alert
      title="预警阈值说明"
      description="黄色预警 = 客流达到最大承载量的百分比（默认80%），红色预警 = 客流达到最大承载量的百分比（默认100%）。修改后对所有景区实时生效。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <el-table
      :data="tableData"
      stripe
      style="width: 100%"
      :header-cell-style="{ background: 'var(--bg-dark)', color: 'var(--text-primary)', borderColor: 'var(--border-color)' }"
      :cell-style="{ borderColor: 'var(--border-color)' }"
    >
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="scenicName" label="景区名称" min-width="180" />
      <el-table-column label="黄色预警阈值(%)" width="200" align="center">
        <template #default="{ row }">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-input-number
              v-model="row.yellowThreshold"
              :min="50"
              :max="100"
              :step="5"
              size="small"
              style="width: 120px;"
            />
            <span style="color: var(--warning-yellow); font-size: 13px;">⚠️</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="红色预警阈值(%)" width="200" align="center">
        <template #default="{ row }">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-input-number
              v-model="row.redThreshold"
              :min="80"
              :max="150"
              :step="5"
              size="small"
              style="width: 120px;"
            />
            <span style="color: var(--warning-red); font-size: 13px;">🚨</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="预警状态示意" width="200" align="center">
        <template #default="{ row }">
          <div style="display: flex; gap: 12px; justify-content: center;">
            <div class="threshold-preview" style="background: rgba(46, 213, 115, 0.15); color: var(--safe-green);">
              &lt;{{ row.yellowThreshold }}%
            </div>
            <div class="threshold-preview" style="background: rgba(255, 184, 0, 0.15); color: var(--warning-yellow);">
              {{ row.yellowThreshold }}-{{ row.redThreshold }}%
            </div>
            <div class="threshold-preview" style="background: rgba(255, 71, 87, 0.15); color: var(--warning-red);">
              ≥{{ row.redThreshold }}%
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleSave(row)">保存</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { thresholdApi } from '@/api'
import { ElMessage } from 'element-plus'

const tableData = ref([])

const fetchData = async () => {
  try {
    const res = await thresholdApi.list()
    if (res.code === 200) {
      tableData.value = res.data
    }
  } catch (e) {
    console.error('获取阈值配置失败', e)
  }
}

const handleSave = async (row) => {
  if (row.yellowThreshold >= row.redThreshold) {
    ElMessage.warning('黄色预警阈值必须小于红色预警阈值')
    return
  }
  try {
    const res = await thresholdApi.update({
      id: row.id,
      scenicId: row.scenicId,
      yellowThreshold: row.yellowThreshold,
      redThreshold: row.redThreshold
    })
    if (res.code === 200) {
      ElMessage.success(`「${row.scenicName}」阈值保存成功`)
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.threshold-preview {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
</style>
