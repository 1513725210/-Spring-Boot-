/**
 * WebSocket 连接管理工具
 */
class WebSocketClient {
    constructor() {
        this.ws = null
        this.url = ''
        this.reconnectTimer = null
        this.heartbeatTimer = null
        this.listeners = new Map()
        this.reconnectAttempts = 0
        this.maxReconnectAttempts = 10
    }

    /**
     * 建立连接
     */
    connect(url) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            return
        }

        this.url = url || `ws://${window.location.host}/ws/warning`

        try {
            this.ws = new WebSocket(this.url)

            this.ws.onopen = () => {
                console.log('[WS] 连接成功')
                this.reconnectAttempts = 0
                this.startHeartbeat()
                this.emit('connected')
            }

            this.ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data)
                    if (data.type) {
                        this.emit(data.type, data)
                    }
                    this.emit('message', data)
                } catch (e) {
                    // 非 JSON 消息（如 pong）
                }
            }

            this.ws.onclose = () => {
                console.log('[WS] 连接断开')
                this.stopHeartbeat()
                this.emit('disconnected')
                this.tryReconnect()
            }

            this.ws.onerror = (err) => {
                console.error('[WS] 连接错误', err)
                this.emit('error', err)
            }
        } catch (e) {
            console.error('[WS] 创建连接失败', e)
            this.tryReconnect()
        }
    }

    /**
     * 注册事件监听
     */
    on(event, callback) {
        if (!this.listeners.has(event)) {
            this.listeners.set(event, [])
        }
        this.listeners.get(event).push(callback)
        return this
    }

    /**
     * 移除事件监听
     */
    off(event, callback) {
        if (this.listeners.has(event)) {
            const cbs = this.listeners.get(event).filter(cb => cb !== callback)
            this.listeners.set(event, cbs)
        }
    }

    /**
     * 触发事件
     */
    emit(event, data) {
        if (this.listeners.has(event)) {
            this.listeners.get(event).forEach(cb => {
                try { cb(data) } catch (e) { console.error('[WS] 回调错误', e) }
            })
        }
    }

    /**
     * 心跳检测
     */
    startHeartbeat() {
        this.stopHeartbeat()
        this.heartbeatTimer = setInterval(() => {
            if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                this.ws.send('ping')
            }
        }, 30000)
    }

    stopHeartbeat() {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer)
            this.heartbeatTimer = null
        }
    }

    /**
     * 自动重连
     */
    tryReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.warn('[WS] 超过最大重连次数')
            return
        }

        const delay = Math.min(3000 * Math.pow(1.5, this.reconnectAttempts), 30000)
        this.reconnectAttempts++
        console.log(`[WS] ${delay / 1000}s 后重连 (第${this.reconnectAttempts}次)`)

        this.reconnectTimer = setTimeout(() => {
            this.connect(this.url)
        }, delay)
    }

    /**
     * 关闭连接
     */
    close() {
        this.stopHeartbeat()
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer)
        }
        if (this.ws) {
            this.ws.close()
            this.ws = null
        }
    }
}

// 单例
const wsClient = new WebSocketClient()
export default wsClient
