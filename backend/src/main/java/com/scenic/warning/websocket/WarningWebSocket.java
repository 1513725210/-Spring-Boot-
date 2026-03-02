package com.scenic.warning.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 服务端点
 * 用于向前端大屏推送实时客流数据和预警信息
 */
@Slf4j
@Component
@ServerEndpoint("/ws/warning")
public class WarningWebSocket {

    /** 在线连接数 */
    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    /** 存储所有连接的 Session */
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        sessionMap.put(session.getId(), session);
        int count = onlineCount.incrementAndGet();
        log.info("WebSocket 新连接加入, sessionId={}, 当前在线数={}", session.getId(), count);
    }

    @OnClose
    public void onClose() {
        if (session != null) {
            sessionMap.remove(session.getId());
            int count = onlineCount.decrementAndGet();
            log.info("WebSocket 连接断开, sessionId={}, 当前在线数={}", session.getId(), count);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到客户端消息: {}", message);
        // 心跳检测
        if ("ping".equals(message)) {
            sendMessage(session, "pong");
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 发生错误, sessionId={}", session.getId(), error);
    }

    /**
     * 向所有连接发送消息（广播）
     */
    public static void broadcastMessage(Object data) {
        String message = JSON.toJSONString(data);
        sessionMap.values().forEach(session -> {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        });
    }

    /**
     * 发送消息到指定 Session
     */
    private static void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    public static int getOnlineCount() {
        return onlineCount.get();
    }
}
