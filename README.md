# 🏔️ 景区过度旅游预警系统

> **Scenic Area Over-Tourism Early Warning System**
>
> 一个融合 **AI 大模型**、**时间序列预测**、**实时 WebSocket 推送** 的智能景区管理平台。

---

## 📋 目录

- [项目简介](#-项目简介)
- [核心功能](#-核心功能)
- [系统架构](#-系统架构)
- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [快速启动](#-快速启动)
- [数据库设计](#-数据库设计)
- [API 接口文档](#-api-接口文档)
- [页面说明](#-页面说明)
- [配置说明](#-配置说明)
- [常见问题](#-常见问题)
- [许可证](#-许可证)

---

## 🌟 项目简介

本系统是面向湖北省 **22 个真实景区** 的过度旅游预警管理平台，为景区管理者提供：

- 📊 **实时客流监控大屏**：基于湖北省地图的可视化数据展示
- 🤖 **AI 智能数据管家**：用自然语言查询客流数据（Text-to-SQL），由 DeepSeek 大模型驱动
- 📈 **ARIMA 客流趋势预测**：基于历史数据的时间序列分析，提前预判高峰
- 🚨 **多级预警机制**：黄色（80%）/ 红色（100%）双级别阈值预警，WebSocket 实时推送
- ⚡ **AI 应急预案自动生成（RAG）**：结合 SOP 知识库，实时生成可执行的应急指令
- 💬 **游客舆情情感分析（NLP）**：定时分析游客评论，识别负面情绪触发预警

---

## 🎯 核心功能

### 1. 实时监控大屏 Dashboard

| 模块 | 说明 |
|------|------|
| 湖北省地图 | 基于 ECharts Geo 组件，散点标注各景区位置，颜色随拥挤度动态变化（绿→黄→红） |
| 数据总览 | 实时显示监控景区总数、当前总客流、预警数量 |
| 拥挤度排行榜 | 所有景区按拥挤度百分比排序，支持点击切换预测目标 |
| 实时预警动态 | 滚动显示最新预警日志，包含景区名、预警级别、客流详情 |
| 预警弹窗 | 红色/黄色预警时弹出醒目通知，红色预警附带 AI 应急预案 |

### 2. AI 数据管家（Text-to-SQL）

- 用户在可拖拽的悬浮聊天窗中输入自然语言问题
- 系统自动将问题转为 SQL 查询，执行后由 AI 生成可读性分析报告
- 示例问题：
  - `"哪里人少？"` → 查询所有景区并按拥挤度排序
  - `"今天有过红色预警吗？"` → 查询 warning_log 表
  - `"武汉市景区的平均客流量？"` → 聚合统计

### 3. ARIMA 客流趋势预测

- 基于 Python `statsmodels` 库的 ARIMA 时间序列模型
- 需要至少 20 条历史客流记录才能启用预测
- 预测未来 12 个时间点（每 5 分钟一个），提供置信区间
- 当数据不足时自动回退到简单移动平均算法

### 4. AI 应急预案生成（RAG）

- 当触发红色预警时，系统加载 `emergency_sop.txt` 应急预案知识库
- 结合当前预警信息（景区、人数、拥挤度），由 DeepSeek 生成 3-4 步可执行指令
- 生成的 HTML 格式预案直接嵌入预警弹窗中展示

### 5. 游客舆情情感分析

- Spring Boot 定时任务每 2 分钟执行一次
- 模拟游客评论语料库（含正面/负面/中性评论）
- DeepSeek 大模型进行情感分类，识别 `挤死了`、`退票`、`打架` 等关键负面标签
- 当检测到负面情绪时，自动触发黄色预警并通过 WebSocket 推送

### 6. 客流模拟器

- 使用正弦波形 + 随机扰动模拟真实客流变化
- 模拟规律：早高峰 → 午间峰值 → 晚高峰 → 傍晚下降 → 夜间极少
- 不同景区拥有不同的"热度系数"（如 5A > 4A）
- 每 5 秒生成一次客流数据并通过 WebSocket 推送至前端

### 7. 后台管理系统

| 页面 | 功能 |
|------|------|
| 景区管理 | 增删改查景区信息，包括名称、坐标、最大承载量等 |
| 阈值配置 | 为每个景区单独配置黄色/红色预警阈值百分比 |
| 预警日志 | 分页查询历史预警记录，支持标记处理状态 |

---

## 🏗️ 系统架构

```
┌──────────────────────────┐
│     前端 (Vue 3 + Vite)  │
│  ┌────────────────────┐  │
│  │  Dashboard 监控大屏 │  │
│  │  AI 悬浮聊天窗口    │──────── HTTP ──────┐
│  │  后台管理页面       │  │                   │
│  │  客流预测页面       │  │                   ▼
│  └────────────────────┘  │        ┌────────────────────┐
│         │ WebSocket       │        │  Python Flask 微服务 │
│         ▼                │        │  (端口 5000)        │
│  ┌────────────────────┐  │        │                    │
│  │  实时数据推送       │  │        │  /api/ai/chat      │
│  └────────────────────┘  │        │  /api/ai/generate_plan│
└──────────┬───────────────┘        │  /api/ai/sentiment  │
           │ HTTP Proxy (/api)      │  /api/ai/sql_gen    │
           ▼                        └─────────┬──────────┘
┌──────────────────────────┐                  │
│  后端 (Spring Boot 2.7)  │                  │ OpenAI SDK
│                          │                  ▼
│  ┌────────────────────┐  │        ┌────────────────────┐
│  │  Controller 层      │  │        │  DeepSeek API      │
│  │  Service 业务逻辑   │────────>│  (deepseek-chat)   │
│  │  定时任务调度       │  │ HTTP   └────────────────────┘
│  │  WebSocket 推送     │  │
│  │  FlowSimulator 模拟 │  │
│  └────────────────────┘  │
│         │                │
│         ▼                │
│  ┌────────────────────┐  │
│  │  MySQL 8 数据库     │  │
│  │  tourism_warning    │  │
│  └────────────────────┘  │
└──────────────────────────┘
```

**数据流向说明：**

1. **前端聊天** → 直接调用 Python Flask `/api/ai/chat`（绕过 Java，避免 Java 环境网络问题）
2. **客流模拟** → Java 定时任务 → 写入 MySQL + WebSocket 广播 → 前端实时更新
3. **预警触发** → Java 检测阈值 → 写入预警日志 → 调用 Python 生成应急预案 → WebSocket 推送弹窗
4. **舆情分析** → Java 定时任务 → 调用 Python 情感分析 → 检测到负面情绪 → 触发预警
5. **ARIMA 预测** → Java Controller → 调用 Python 脚本 → 返回预测结果

---

## 🛠️ 技术栈

### 后端 (Java)

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 应用框架 |
| Spring Security | - | 认证授权（开发阶段已放行所有 `/api/**`） |
| Spring WebSocket | - | 实时消息推送 |
| MyBatis-Plus | 3.5.5 | ORM 持久层 |
| Druid | 1.2.21 | 数据库连接池 |
| JWT (jjwt) | 0.9.1 | Token 认证 |
| Fastjson2 | 2.0.43 | JSON 序列化 |
| Lombok | - | 代码简化 |
| MySQL Connector | - | 数据库驱动 |

### 前端 (JavaScript)

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.2.47 | UI 框架 |
| Vite | 2.9.16 | 构建工具 |
| Vue Router | 4.1.6 | 路由管理 |
| Pinia | 2.0.36 | 状态管理 |
| Axios | 1.4.0 | HTTP 客户端 |
| ECharts | 5.4.3 | 数据可视化（地图、折线图） |
| Element Plus | 2.3.14 | UI 组件库 |

### AI 微服务 (Python)

| 技术 | 用途 |
|------|------|
| Flask | Web 框架 |
| Flask-CORS | 跨域支持 |
| OpenAI SDK | 调用 DeepSeek 大模型 API |
| PyMySQL | 直接连接 MySQL（Text-to-SQL 执行） |
| NumPy / Pandas | 数据处理 |
| Statsmodels | ARIMA 时间序列预测 |

### 基础设施

| 技术 | 用途 |
|------|------|
| MySQL 8.x | 关系型数据库 |
| DeepSeek API | AI 大模型（Chat Completions） |

---

## 📦 项目结构

```
project/
├── README.md                          # 项目说明文档
├── .gitignore                         # Git 忽略规则
│
├── backend/                           # ☕ Spring Boot 后端
│   ├── pom.xml                        # Maven 依赖配置
│   └── src/main/
│       ├── java/com/scenic/warning/
│       │   ├── TourismWarningApplication.java    # 启动类
│       │   ├── common/
│       │   │   └── Result.java                   # 统一响应封装
│       │   ├── config/
│       │   │   ├── CorsConfig.java               # CORS 跨域配置
│       │   │   ├── SecurityConfig.java           # Spring Security 配置
│       │   │   ├── WebSocketConfig.java          # WebSocket 配置
│       │   │   └── MybatisPlusMetaHandler.java   # 自动填充处理器
│       │   ├── entity/
│       │   │   ├── ScenicSpot.java               # 景区实体
│       │   │   ├── FlowRecord.java               # 客流记录实体
│       │   │   ├── WarningLog.java               # 预警日志实体
│       │   │   ├── ThresholdConfig.java          # 阈值配置实体
│       │   │   └── SysUser.java                  # 用户实体
│       │   ├── mapper/                           # MyBatis 数据访问层
│       │   ├── service/
│       │   │   ├── ScenicSpotService.java        # 景区服务
│       │   │   ├── FlowRecordService.java        # 客流记录服务
│       │   │   ├── WarningLogService.java        # 预警日志服务
│       │   │   ├── RagEmergencyService.java      # RAG 应急预案（调用 Python）
│       │   │   └── impl/                         # 服务实现类
│       │   ├── controller/
│       │   │   ├── DashboardController.java      # 大屏数据接口
│       │   │   ├── ScenicSpotController.java     # 景区 CRUD 接口
│       │   │   ├── WarningLogController.java     # 预警日志接口
│       │   │   ├── ThresholdConfigController.java # 阈值配置接口
│       │   │   ├── PredictController.java        # ARIMA 预测接口
│       │   │   └── AuthController.java           # 认证登录接口
│       │   ├── scheduler/
│       │   │   └── FlowSimulator.java            # 客流模拟器（正弦波+随机扰动）
│       │   ├── task/
│       │   │   └── SentimentAnalysisTask.java    # 舆情分析定时任务
│       │   └── websocket/
│       │       └── WarningWebSocket.java         # WebSocket 端点
│       └── resources/
│           ├── application.yml                   # 应用配置
│           ├── schema.sql                        # 数据库初始化脚本
│           └── emergency_sop.txt                 # 应急预案 SOP 知识库
│
├── frontend/                          # 🎨 Vue 3 + Vite 前端
│   ├── package.json                   # 依赖配置
│   ├── vite.config.js                 # Vite 配置（代理到后端 8088）
│   ├── index.html                     # 入口 HTML
│   └── src/
│       ├── main.js                    # 应用入口
│       ├── App.vue                    # 根组件
│       ├── api/
│       │   └── index.js              # API 接口封装（Axios）
│       ├── router/
│       │   └── index.js              # 路由配置
│       ├── assets/
│       │   ├── main.css              # 全局样式（含 AI 聊天窗口样式）
│       │   └── map/hubei.json        # 湖北省 GeoJSON 地图数据
│       ├── utils/
│       │   └── websocket.js          # WebSocket 客户端封装
│       └── views/
│           ├── Login.vue             # 登录页面
│           ├── dashboard/
│           │   ├── Dashboard.vue     # 🖥️ 监控大屏（主页面 + AI 聊天）
│           │   └── Prediction.vue    # 📈 客流预测专用页面
│           └── admin/
│               ├── AdminLayout.vue   # 后台管理布局
│               ├── ScenicManage.vue  # 景区管理
│               ├── ThresholdManage.vue # 阈值配置
│               └── WarningLog.vue    # 预警日志
│
└── python/                            # 🐍 AI 微服务 + 预测脚本
    ├── requirements.txt               # Python 依赖
    ├── ai_service.py                  # Flask AI 微服务（5 个路由）
    └── arima_predict.py               # ARIMA 时间序列预测脚本
```

---

## 🚀 快速启动

### 环境要求

| 软件 | 版本要求 |
|------|----------|
| JDK | 1.8+ |
| Maven | 3.6+ |
| Node.js | 14+ |
| Python | 3.8+ |
| MySQL | 8.0+ |

### 第一步：初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本（包含建库、建表、初始数据）
source backend/src/main/resources/schema.sql;
```

> 脚本会自动创建 `tourism_warning` 数据库，并插入湖北省 22 个真实景区数据及默认预警阈值。

### 第二步：启动后端服务（Spring Boot）

```bash
cd backend

# 编译打包
mvn clean package -DskipTests

# 运行（默认端口 8088）
java -jar target/tourism-warning-1.0.0.jar
```

或使用 Maven 直接启动：
```bash
cd backend
mvn spring-boot:run
```

### 第三步：启动 AI 微服务（Python Flask）

```bash
cd python

# 安装依赖
pip install -r requirements.txt
pip install flask-cors pymysql

# 启动（默认端口 5000）
python ai_service.py
```

### 第四步：启动前端（Vite Dev Server）

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器（默认端口 5173）
npm run dev
```

### 第五步：访问系统

| 地址 | 说明 |
|------|------|
| `http://localhost:5173` | 前端页面 |
| `http://localhost:5173/dashboard` | 监控大屏（默认首页） |
| `http://localhost:5173/prediction` | 客流预测页面 |
| `http://localhost:5173/admin` | 后台管理 |
| `http://localhost:5173/login` | 登录页面 |

**默认管理员账号：**
- 用户名：`admin`
- 密码：`admin123`

---

## 💾 数据库设计

共 5 张核心业务表：

### ER 关系

```
sys_user (管理员)
    │
scenic_spot (景区) ─────┬──── flow_record (客流流水)
    │                   │
    │                   └──── warning_log (预警日志)
    │
    └──── threshold_config (阈值配置)
```

### 表结构概览

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `sys_user` | 管理员信息 | username, password(BCrypt), role |
| `scenic_spot` | 景区信息 | name, city, longitude, latitude, max_capacity, current_count, level, status |
| `flow_record` | 客流流水记录 | scenic_id, current_count, in_count, out_count, congestion_rate, record_time |
| `warning_log` | 预警历史记录 | scenic_id, warning_level(YELLOW/RED), congestion_rate, message, plan(AI应急预案), handled |
| `threshold_config` | 预警阈值配置 | scenic_id, yellow_percent(默认80%), red_percent(默认100%), enable_warning |

### 初始景区数据

系统内置 **湖北省 22 个真实景区**（含真实经纬度坐标），覆盖：

| 城市 | 景区 | 等级 |
|------|------|------|
| 武汉市 | 黄鹤楼、东湖风景区、木兰天池、武汉欢乐谷 | 5A/4A |
| 宜昌市 | 三峡大坝、长阳清江画廊、三峡人家 | 5A |
| 十堰市 | 武当山、房县野人谷 | 5A/4A |
| 恩施州 | 恩施大峡谷、腾龙洞、恩施土司城 | 5A/4A |
| 襄阳市 | 古隆中、襄阳古城 | 5A/4A |
| 神农架 | 神农架、神农顶 | 5A |
| 黄冈市 | 东坡赤壁、天堂寨 | 4A |
| 荆州市 | 荆州古城、荆州方特 | 4A |
| 咸宁市 | 赤壁古战场、九宫山 | 5A/4A |

---

## 📡 API 接口文档

### Java 后端接口（端口 8088）

#### 认证模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| GET | `/api/auth/info` | 获取用户信息 |

#### 大屏数据

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard/overview` | 总览数据（景区列表、总客流、预警数） |
| GET | `/api/dashboard/trend/{scenicId}` | 指定景区客流趋势 |
| GET | `/api/dashboard/ranking` | 拥挤度排行榜 |

#### 景区管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/scenic/list` | 景区列表 |
| GET | `/api/scenic/page` | 分页查询 |
| GET | `/api/scenic/active` | 已开放景区 |
| GET | `/api/scenic/{id}` | 景区详情 |
| POST | `/api/scenic` | 新增景区 |
| PUT | `/api/scenic` | 修改景区 |
| DELETE | `/api/scenic/{id}` | 删除景区 |

#### 预警日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/warning/page` | 分页查询预警记录 |
| GET | `/api/warning/recent` | 最近 N 条预警 |
| PUT | `/api/warning/handle/{id}` | 处理预警 |

#### 阈值配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/threshold/list` | 阈值列表 |
| GET | `/api/threshold/scenic/{id}` | 指定景区阈值 |
| PUT | `/api/threshold` | 修改阈值 |

#### ARIMA 预测

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/predict/arima/{scenicId}` | 获取 ARIMA 预测结果 |

#### WebSocket

| 路径 | 说明 |
|------|------|
| `ws://localhost:8088/ws` | 实时推送（FLOW_UPDATE / WARNING） |

### Python AI 微服务接口（端口 5000）

| 方法 | 路径 | 说明 | 请求参数 |
|------|------|------|----------|
| POST | `/api/ai/chat` | AI 数据管家（Text-to-SQL 全流程）| `{ "query": "哪里人少？" }` |
| POST | `/api/ai/sql_generation` | 自然语言转 SQL | `{ "query": "...", "schema_prompt": "..." }` |
| POST | `/api/ai/data_summary` | 数据结果摘要 | `{ "query": "...", "db_result": "..." }` |
| POST | `/api/ai/generate_plan` | 应急预案生成（RAG）| `{ "scenic_name", "level", "current_count", "congestion_rate", "sop_context" }` |
| POST | `/api/ai/sentiment_analysis` | 舆情情感分析 | `{ "reviews": "评论文本..." }` |

---

## 🖥️ 页面说明

### 监控大屏 (`/dashboard`)

- **左侧**：数据总览卡片 + ARIMA 客流预测折线图
- **中间**：湖北省地图（ECharts Geo），散点标注景区实时客流
- **右侧**：拥挤度排行榜 + 实时预警动态
- **右下角**：AI 数据管家悬浮窗（可拖拽 + 可缩放）
- **覆盖层**：预警弹窗（红色/黄色，含 AI 应急预案）

### 客流预测 (`/prediction`)

- 独立的 ARIMA 预测页面
- 选择景区后展示预测折线图 + 置信区间

### 后台管理 (`/admin`)

- **景区管理**：增删改查景区信息（表格 + 表单弹窗）
- **阈值配置**：为每个景区配置黄色/红色预警百分比
- **预警日志**：分页查看历史预警，支持处理操作

### 登录页 (`/login`)

- 管理员登录表单
- 默认账号 `admin` / `admin123`

---

## ⚙️ 配置说明

### 后端配置 (`backend/src/main/resources/application.yml`)

```yaml
server:
  port: 8088                    # 后端端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tourism_warning
    username: root              # ← 修改为你的 MySQL 用户名
    password: 123456            # ← 修改为你的 MySQL 密码

simulator:
  enabled: true                 # 是否开启客流模拟
  interval-ms: 5000             # 模拟间隔（毫秒）

arima:
  python-path: python           # Python 可执行文件路径
  script-path: ../python/arima_predict.py
```

### AI 微服务配置 (`python/ai_service.py`)

```python
# DeepSeek API 配置（第 13-14 行）
API_KEY = "sk-xxxxx"           # ← 替换为你的 DeepSeek API Key
BASE_URL = "https://api.deepseek.com"

# MySQL 数据库配置（第 66-74 行）
db_config = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",             # ← 修改为你的 MySQL 用户名
    "password": "123456",       # ← 修改为你的 MySQL 密码
    "database": "tourism_warning",
}
```

### 前端配置 (`frontend/vite.config.js`)

```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8088',  // Java 后端地址
      changeOrigin: true
    },
    '/ws': {
      target: 'ws://localhost:8088',    // WebSocket 地址
      ws: true
    }
  }
}
```

> **注意：** AI 聊天直接调用 `http://127.0.0.1:5000/api/ai/chat`（绕过 Vite 代理），配置在 `frontend/src/api/index.js` 中。

---

## ❓ 常见问题

### Q: 启动后端报 `JAVA_HOME` 错误？

确保已安装 JDK 1.8+ 并正确配置环境变量：
```bash
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
set PATH=%JAVA_HOME%\bin;%PATH%
```

### Q: AI 聊天显示"网络错误"？

1. 确认 Python Flask 服务已启动（`python ai_service.py`），端口 5000
2. 确认 `ai_service.py` 中的 DeepSeek API Key 有效
3. 确认 MySQL 服务正在运行且 `tourism_warning` 数据库已创建

### Q: ARIMA 预测显示"暂无预测数据"？

- 系统启动后需要 **积累至少 20 条客流记录**（约 100 秒，因为模拟器每 5 秒产生一条）
- 等待一段时间后再点击预测即可

### Q: 地图不显示？

确认 `frontend/src/assets/map/hubei.json` 文件存在，这是湖北省的 GeoJSON 数据。

### Q: 如何获取 DeepSeek API Key？

访问 [DeepSeek 开放平台](https://platform.deepseek.com/) 注册账号并创建 API Key。

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源许可证。

---

## 🙏 致谢

- [DeepSeek](https://www.deepseek.com/) — AI 大模型 API
- [ECharts](https://echarts.apache.org/) — 数据可视化
- [Element Plus](https://element-plus.org/) — Vue 3 UI 组件库
- [Spring Boot](https://spring.io/projects/spring-boot) — Java 应用框架
- [MyBatis-Plus](https://baomidou.com/) — ORM 增强框架
