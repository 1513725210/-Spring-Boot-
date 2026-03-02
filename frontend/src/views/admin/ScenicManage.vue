<template>
  <div>
    <!-- 操作栏 -->
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px;">
      <el-input
        v-model="searchName"
        placeholder="搜索景区名称..."
        prefix-icon="Search"
        style="width: 260px;"
        clearable
        @clear="fetchData"
        @keyup.enter="fetchData"
      />
      <el-button type="primary" @click="handleAdd">+ 新增景区</el-button>
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
      <el-table-column prop="name" label="景区名称" min-width="160" />
      <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
      <el-table-column label="经纬度" width="200">
        <template #default="{ row }">
          {{ row.longitude }}, {{ row.latitude }}
        </template>
      </el-table-column>
      <el-table-column prop="maxCapacity" label="最大承载量" width="110" align="center" />
      <el-table-column prop="currentCount" label="当前客流" width="100" align="center">
        <template #default="{ row }">
          <span :style="{ color: getCountColor(row) }">{{ row.currentCount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '开放' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑景区' : '新增景区'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="景区名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入景区名称" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入景区地址" />
        </el-form-item>
        <el-form-item label="经度" prop="longitude">
          <el-input-number v-model="formData.longitude" :precision="4" :step="0.01" style="width: 100%" />
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input-number v-model="formData.latitude" :precision="4" :step="0.01" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大承载量" prop="maxCapacity">
          <el-input-number v-model="formData.maxCapacity" :min="100" :max="500000" :step="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="热度系数" prop="hotFactor">
          <el-slider v-model="formData.hotFactor" :min="0.1" :max="2.0" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">开放</el-radio>
            <el-radio :label="0">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { scenicApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchName = ref('')
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const formData = reactive({
  id: null,
  name: '',
  address: '',
  longitude: 114.0,
  latitude: 30.5,
  maxCapacity: 50000,
  hotFactor: 1.0,
  status: 1
})

const formRules = {
  name: [{ required: true, message: '请输入景区名称', trigger: 'blur' }],
  maxCapacity: [{ required: true, message: '请输入最大承载量', trigger: 'blur' }]
}

const fetchData = async () => {
  try {
    const res = await scenicApi.page({
      current: pageNum.value,
      size: pageSize.value,
      name: searchName.value || undefined
    })
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } catch (e) {
    console.error('获取景区列表失败', e)
  }
}

const getCountColor = (row) => {
  const rate = (row.currentCount / row.maxCapacity) * 100
  if (rate >= 100) return 'var(--warning-red)'
  if (rate >= 80) return 'var(--warning-yellow)'
  return 'var(--safe-green)'
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formData, { id: null, name: '', address: '', longitude: 114.0, latitude: 30.5, maxCapacity: 50000, hotFactor: 1.0, status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = isEdit.value
        ? await scenicApi.update(formData)
        : await scenicApi.add(formData)
      if (res.code === 200) {
        ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
        dialogVisible.value = false
        fetchData()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (e) {
      ElMessage.error('操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除景区「${row.name}」吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    const res = await scenicApi.delete(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    // 用户取消
  }
}

onMounted(fetchData)
</script>
