# ADR 004: 持久会话与知识库

## 状态
✅ 已决定

## 背景
Agent 需要：
- 跨会话记忆（用户偏好、历史对话）
- 策略状态持久化（哪些策略在运行、盈亏如何）
- 从历史交易中学习（哪些信号源靠谱、哪些 Dev 会 Rug）
- 快速检索相关经验（RAG）

## 决策
**三层存储架构：PostgreSQL + Redis + pgvector。**

| 层 | 存储 | 内容 |
|----|------|------|
| **对话记忆** | PostgreSQL JSONB | 对话历史、摘要、用户画像 |
| **策略状态（热）** | Redis | 运行中的策略计数、日限额消耗、WebSocket 连接状态 |
| **策略历史（冷）** | PostgreSQL | 策略参数、执行记录、盈亏明细 |
| **知识库** | PostgreSQL + pgvector | 信号源信誉、Dev 数据库、代币模式、交易洞察 |
| **Agent 间消息** | Redis Pub/Sub | 信号事件、交易决策、通知推送 |

## 原因
1. **PostgreSQL** — 主力存储，JSONB 灵活存对话，关系型管理策略和用户
2. **Redis** — 毫秒级热数据访问 + Pub/Sub 解耦 Agent
3. **pgvector** — 向量检索内嵌在 PostgreSQL 中，不需要额外部署向量数据库
4. **三层合一** — pgvector 是 PG 扩展，运维成本低

## 知识库自动更新

```
每次交易完成后自动：

1. 记录交易结果（token, 金额, 盈亏, 信号源）
2. 更新信号源信誉评分（胜率、Rug 率）
3. 更新 Dev 数据库（该项目存活时间、是否 Rug）
4. 生成 Embedding（交易特征 → 向量）
5. 写入 pgvector（用于后续 RAG 检索）
```

## 后果

### 正面
- Agent 越用越聪明（信号源筛选、Rug 识别）
- 用户跨会话有上下文（不需要重复说明偏好）
- 单数据库运维（pgvector 无需额外服务）

### 负面
- 需要管理 3 个数据存储（PG + Redis + pgvector）
- pgvector 性能在大数据量下不如专用向量数据库

### 缓解措施
- MVP 阶段 pgvector 足够（< 10万条向量）
- 达到瓶颈时可迁移到 Qdrant / Pinecone
- 知识更新异步执行，不阻塞交易链路

## 替代方案
- **只用 PostgreSQL**：向量检索不够，放弃
- **Pinecone / Qdrant**：多了运维组件，MVP 过度设计，放弃
- **MongoDB**：交易场景需要事务，MongoDB 不如 PG，放弃
