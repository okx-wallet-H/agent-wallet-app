# Agent Wallet

> 用自然语言管理你的加密交易。AI Agent 7×24 监控信号、自动执行策略、越用越聪明。

## 功能

- **💬 对话驱动交易** — 跟 Agent 聊天就能设置策略、执行交易、查看盈亏
- **🤖 多 Agent 协作** — 信号监控 / 安全分析 / 交易执行 独立运作，互不阻塞
- **🔐 安全自主签名** — Coinbase MPC 钱包 + Session Key，Agent 限额内自主操作
- **📡 实时信号发现** — OKX 聪明钱 / KOL / 巨鲸动态 + 新币狙击
- **🛡️ 多层风控** — 单笔限额 / 日限额 / 代币白名单 / 滑点保护
- **🧠 持续学习** — 知识库记录交易结果，越交易越聪明

## 技术栈

![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack_Compose-35-4285F4?logo=jetpackcompose)
![Ktor](https://img.shields.io/badge/Ktor-3.1-7F52FF?logo=kotlin)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis)

## 快速开始

### 前置条件

- JDK 17+
- Android Studio Hedgehog+
- PostgreSQL 17
- Redis 7
- Coinbase CDP API Key
- OKX Onchain OS API Key

### 后端

```bash
cd backend

# 设置环境变量
export JWT_SECRET="your-secret"
export DB_URL="jdbc:postgresql://localhost:5432/agentwallet"
export OKX_API_KEY="..."
export COINBASE_API_KEY="..."

# 启动
./gradlew run
```

### Android

```bash
cd android

# 在 Android Studio 中打开，或命令行构建
./gradlew assembleDebug

# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 架构决策

所有重要技术决策记录在 `docs/decisions/` 目录。每个 ADR 包含背景、决策、理由和替代方案。

## 项目状态

MVP 骨架已完成。详见 [CLAUDE.md](CLAUDE.md)。
