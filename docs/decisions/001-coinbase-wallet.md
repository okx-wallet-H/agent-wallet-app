# ADR 001: 钱包层使用 Coinbase Agentic Wallet

## 状态
✅ 已决定

## 背景
Agent Wallet 需要为多用户提供钱包服务，核心需求：
- 每个终端用户拥有独立钱包
- Agent 能自主签名交易（无需每笔用户手动确认）
- 私钥安全管理（不能暴露给 LLM 或 Agent 代码）
- 支持多链（EVM + Solana）

## 决策
**选择 Coinbase Agentic Wallet 管理钱包层。**

## 原因
1. **原生多用户支持** — CDP 有 `end-users` API，专为平台方管理多用户设计
2. **MPC 密钥管理** — 私钥分片，无单点泄露，符合安全要求
3. **Session Key / Delegated Signing** — Agent 可持有限权密钥自主签名，用户设定限额
4. **Policy Engine** — 原生支持单笔限额、日限额、代币白名单等策略
5. **ERC-4337 智能账户** — Gas 代付，用户无需持有各链原生代币
6. **Kotlin SDK** — 与 Android + Ktor 后端语言一致

### 对比 OKX Agentic Wallet

| | Coinbase | OKX |
|--|---------|-----|
| 多用户 API | ✅ end-users API | ❓ 文档未明确 |
| MPC 密钥 | ✅ | ✅ TEE |
| Session Key | ✅ | ✅ |
| Policy Engine | ✅ 原生 | ⚠️ 依赖确认流程 |
| Kotlin SDK | ✅ | ❌ |

## 后果

### 正面
- 多用户钱包管理开箱即用
- MPC + Session Key 安全模型成熟
- Policy Engine 减少自建风控工作量

### 负面
- 依赖两个平台（Coinbase 管钱包 + OKX 管交易）
- CDP 主要面向 Base 链，跨链 Solana 支持待验证
- Coinbase 的费用结构需要评估

### 缓解措施
- 钱包层抽象为接口，可替换实现
- Solana 交易通过 OKX DEX 路由，签名层由 Coinbase 处理

## 替代方案
- **纯 OKX Agentic Wallet**：多用户路径不清晰，放弃
- **自建 MPC**：工作量巨大（3-6 个月），安全审计成本高，放弃
- **Turnkey + Safe**：安全但延迟高，不适合高频交易场景，放弃
