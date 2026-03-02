import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/dashboard'
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '监控大屏' }
    },
    {
        path: '/prediction',
        name: 'Prediction',
        component: () => import('@/views/dashboard/Prediction.vue'),
        meta: { title: '客流趋势预测' }
    },
    {
        path: '/admin',
        name: 'Admin',
        component: () => import('@/views/admin/AdminLayout.vue'),
        meta: { title: '后台管理' },
        children: [
            {
                path: '',
                redirect: '/admin/scenic'
            },
            {
                path: 'scenic',
                name: 'ScenicManage',
                component: () => import('@/views/admin/ScenicManage.vue'),
                meta: { title: '景区管理' }
            },
            {
                path: 'threshold',
                name: 'ThresholdManage',
                component: () => import('@/views/admin/ThresholdManage.vue'),
                meta: { title: '阈值配置' }
            },
            {
                path: 'warning',
                name: 'WarningLog',
                component: () => import('@/views/admin/WarningLog.vue'),
                meta: { title: '预警日志' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} - 景区预警系统` : '景区预警系统'
    next()
})

export default router
