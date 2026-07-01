# Agent Wallet

AI 驱动的多用户加密交易 Agent 应用。Android 原生前端 + Ktor 后端，用户通过自然语言对话管理交易策略、发现信号、执行链上交易。

## 架构概览

```
┌──────────────────────┐      HTTPS/WS       ┌─────────────────────────────┐
│   Android App        │ ◄──────────────────► │   Ktor Backend              │
│   Jetpack Compose    │                      │                             │
│   Dark-only theme    │                      │   OrchestratorAgent         │
│   4 tabs (Chat/      │                      │   ├── ChatAgent             │
│   Portfolio/Strategy/│                      │   ├── SignalAgent           │
│   Signal)            │                      │   ├── AnalysisAgent         │
└──────────────────────┘                      │   ├── ExecutionAgent        │
                                              │   └── KnowledgeAgent        │
                                              │                             │
                                              │   ┌─────────────────────┐   │
                                              │   │ Coinbase Agentic   │   │
                                              │   │ Wallet (MPC+Sign)  │   │
                                              │   └─────────────────────┘   │
                                              │   ┌─────────────────────┐   │
                                              │   │ OKX DEX + Signal   │   │
                                              │   │ APIs                │   │
                                              │   └─────────────────────┘   │
                                              └─────────────────────────────┘
```

## 技术栈

| 层 | 技术 |
|----|------|
| Android UI | Jetpack Compose + Material3 |
| 后端 | Ktor 3.x + Kotlin 2.1.x |
| 钱包 | Coinbase Agentic Wallet (MPC + Session Key) |
| 交易路由 | OKX DEX API (500+ DEX 聚合) |
| 信号发现 | OKX Signal / Trenches / Social API |
| 数据库 | PostgreSQL + Redis + pgvector |
| 模型 | Claude (推理) + Gemini Flash (信号初筛) |

## 关键设计决策

详见 `docs/decisions/` 目录：
- [001](docs/decisions/001-coinbase-wallet.md) — 为什么 Coinbase 管钱包
- [002](docs/decisions/002-okx-dex-only.md) — 为什么 OKX 只做 DEX
- [003](docs/decisions/003-multi-agent.md) — 多 Agent 架构
- [004](docs/decisions/004-knowledge-base.md) — 知识库设计
- [005](docs/decisions/005-ui-design.md) — UI 设计哲学

## 项目结构

```
agent-wallet/
├── CLAUDE.md                    ← 本文件
├── README.md
├── docs/decisions/              ← ADR 架构决策记录
├── android/                     ← Android 前端
│   ├── app/src/main/java/com/agentwallet/
│   │   ├── ui/theme/            ← 色彩/字体/形状/间距
│   │   ├── ui/components/       ← 可复用 Compose 组件
│   │   ├── ui/screens/          ← 四大页面
│   │   └── navigation/          ← 导航
│   └── build.gradle.kts
└── backend/                     ← Ktor 后端
    └── src/main/kotlin/com/agentwallet/
        ├── agent/               ← Agent 层（Orchestrator/Chat/Analysis/Execution/Knowledge）
        ├── api/                 ← REST 路由
        ├── auth/                ← JWT 认证
        ├── config/              ← 配置
        ├── models/              ← 数据模型
        ├── plugins/             ← Ktor 插件
        ├── risk/                ← 风控引擎
        └── services/            ← Coinbase/OKX API 客户端
```

## 当前状态

- [x] UI 设计系统（颜色/字体/间距/组件）
- [x] 11 个 Compose 组件
- [x] 4 个页面 + 导航
- [x] Ktor 后端骨架
- [x] Agent 层（Orchestrator + 4 个子 Agent）
- [x] OKX + Coinbase 服务桩
- [x] 风控引擎
- [x] ADR 文档
- [x] LLM 接入（Anthropic Claude function calling）
- [x] OKX API 真实对接（HMAC 签名 + REST）
- [x] Coinbase CDP 服务桩
- [x] 数据库迁移（Exposed 表 + PostgreSQL + Redis）
- [x] 用户认证完善（PostgreSQL 持久化）
- [x] 端到端测试（RiskEngine + OkxAuth + ChatRoutes）
- [x] Android 网络层（ApiClient + Auth + ChatViewModel）
- [ ] OKX WebSocket 实时信号（SignalAgent signalStream）
- [ ] Coinbase CDP REST API 真实集成
- [ ] CI/CD
