import axios from 'axios'

const api = axios.create({
    baseURL: '/api',
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// 响应拦截器
api.interceptors.response.use(
    response => response.data,
    error => {
        console.error('API Error:', error)
        return Promise.reject(error)
    }
)

// ========== 认证 ==========
export const authApi = {
    login: (data) => api.post('/auth/login', data),
    getUserInfo: (userId) => api.get('/auth/info', { params: { userId } })
}

// ========== 景区管理 ==========
export const scenicApi = {
    list: () => api.get('/scenic/list'),
    page: (params) => api.get('/scenic/page', { params }),
    active: () => api.get('/scenic/active'),
    getById: (id) => api.get(`/scenic/${id}`),
    add: (data) => api.post('/scenic', data),
    update: (data) => api.put('/scenic', data),
    delete: (id) => api.delete(`/scenic/${id}`)
}

// ========== 大屏数据 ==========
export const dashboardApi = {
    overview: () => api.get('/dashboard/overview'),
    trend: (scenicId) => api.get(`/dashboard/trend/${scenicId}`),
    ranking: () => api.get('/dashboard/ranking')
}

// ========== 阈值配置 ==========
export const thresholdApi = {
    list: () => api.get('/threshold/list'),
    page: (params) => api.get('/threshold/page', { params }),
    getByScenicId: (scenicId) => api.get(`/threshold/scenic/${scenicId}`),
    update: (data) => api.put('/threshold', data)
}

// ========== 预警日志 ==========
export const warningApi = {
    page: (params) => api.get('/warning/page', { params }),
    recent: (limit = 20) => api.get('/warning/recent', { params: { limit } }),
    handle: (id, handleUser, remark) => api.put(`/warning/handle/${id}`, null, {
        params: { handleUser, remark }
    })
}

// ========== ARIMA 预测 ==========
export const predictApi = {
    // 获取 ARIMA 预测
    predict: (scenicId, steps = 12) => {
        return api({
            url: `/predict/${scenicId}`,
            method: 'get',
            params: { steps }
        })
    }
}

export const aiApi = {
    // 发送数据管家聊天消息
    chat: (query) => {
        return axios({
            url: 'http://127.0.0.1:5000/api/ai/chat',
            method: 'post',
            data: { query }
        }).then(res => res.data)
    }
}
export default api
