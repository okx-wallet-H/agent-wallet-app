# ADR 003: 多 Agent 协作架构

## 状态
✅ 已决定

## 背景
单一 Agent 无法同时处理：
- 用户对话（多轮上下文 + 意图理解）
- 7×24 信号监控（WebSocket 长连接 + 噪声过滤）
- 交易执行（询价 + 签名 + 广播 + 状态追踪）
- 知识学习（事后分析 + 向量检索）

单 Agent 模型下，任一操作阻塞都会影响全局。

## 决策
**采用 7 个专业化 Agent 协作架构。**

### Agent 职责矩阵

| Agent | 职责 | 模型 | 通信 |
|-------|------|------|------|
| **Orchestrator** | 意图分类 + 路由分发 | Claude | Redis pub/sub |
| **Chat** | 多轮对话 + 意图提取 + 策略参数化 | Claude | 直接调用 |
| **Signal** | 7×24 WebSocket 监听 + 噪声过滤 | Gemini Flash | Redis pub/sub |
| **Analysis** | Token 安全 + Dev 画像 + 热度分析 | Claude | Redis pub/sub |
| **Execution** | 询价 + 风控 + 签名 + 广播 | 确定性逻辑 | 直接调用 |
| **Knowledge** | 事后学习 + 向量检索 + 信号源打分 | Embedding + 轻量 | Redis pub/sub |
| **Ops** | 心跳检测 + 自动重连 + 熔断 | 确定性逻辑 | 直接调用 |

### 通信模型
- **同步调用**：Orchestrator → Chat / Execution（需要返回值）
- **异步事件**：Signal → Analysis → Execution（Redis Pub/Sub 解耦）
- **共享上下文**：Redis 存储用户状态快照

## 原因
1. **解耦** — 信号 Agent 崩了不影响用户对话
2. **弹性** — 每个 Agent 可独立扩缩容
3. **模型适配** — 高频信号筛选用便宜的 Gemini Flash，复杂分析用 Claude
4. **ExecutionAgent 零 LLM** — 交易执行不需要推理，确定性逻辑避免幻觉
5. **可测试** — 每个 Agent 可单独测试

## 后果

### 正面
- 模块化，各自独立开发部署
- 信号流不阻塞对话流
- 模型成本可控（高频用便宜模型）

### 负面
- 部署复杂度增加（多进程 / 多协程）
- 需要 Redis 做 Agent 间通信
- 调试跨 Agent 链路更复杂

### 缓解措施
- MVP 阶段所有 Agent 跑在同一进程（协程隔离）
- 通过结构化日志追踪跨 Agent 调用链
- 生产环境逐步拆分到独立进程

## 替代方案
- **单 Agent 全包**：简单但阻塞，放弃
- **两个 Agent（对话 + 交易）**：解决了隔离但不够细粒度，放弃
