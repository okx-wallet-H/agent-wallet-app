# ADR 002: OKX 仅用于 DEX 层和信号发现

## 状态
✅ 已决定

## 背景
需要最优的 DEX 交易路由和多链流动性，以及加密货币市场特有的信号发现能力。

## 决策
**OKX Onchain OS 仅用于 DEX 交易路由和信号发现。不使用 OKX 的钱包功能。**

### OKX 负责的 API
| API | 用途 |
|-----|------|
| DEX Swap Quote | 询价 + 最优路由 |
| DEX Signal | 聪明钱/KOL/巨鲸实时信号 |
| DEX Trenches | 新盘扫描、开发者画像、捆绑检测 |
| DEX Token | 代币安全元数据、持有者分析 |
| DEX Social | 市场情绪、KOL 讨论热度 |
| DEX Market | K线、价格、行情 |

### OKX 不负责的
- ❌ 钱包创建/管理 → Coinbase CDP
- ❌ 交易签名 → Coinbase MPC
- ❌ 私钥存储 → Coinbase TEE

## 原因
1. OKX DEX 聚合 500+ DEX，支持 20+ 链，是市面上覆盖最广的聚合器之一
2. OKX 的信号/trends/social API 是开箱即用的加密市场数据源，不需要自己爬链
3. OKX DEX 返回标准 calldata（to, data, value），任何 EVM/Solana 钱包都能签名
4. OKX 钱包的多用户方案不明确，但 DEX API 是标准的 REST/WS，与钱包无关

## 后果

### 正面
- 最优的 DEX 路由（500+ 聚合，<100ms 延迟）
- 内置信号发现，减少自建数据管道
- DEX 层与钱包层解耦，各自选最优方案

### 负面
- 依赖两个平台，需要管理两套 API Key
- OKX API 签名认证（HMAC）需要正确实现

### 缓解措施
- OKX API 客户端封装为独立模块
- 签名逻辑集中处理，便于调试

## 替代方案
- **自建 DEX 聚合**：工作量大（维护 20+ 链的路由），放弃
- **只用 Coinbase**：缺乏 DEX 聚合和信号数据，不适合交易场景
