package com.agentwallet.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.jsonb

/**
 * Exposed table definitions for all persistent entities.
 */
object UsersTable : UUIDTable("users") {
    val email          = varchar("email", 255).uniqueIndex()
    val passwordHash   = varchar("password_hash", 255)
    val coinbaseWalletId = varchar("coinbase_wallet_id", 255).nullable()
    val createdAt      = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

object StrategiesTable : UUIDTable("strategies") {
    val userId    = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val name      = varchar("name", 255)
    val type      = varchar("type", 50)       // SNIPER, DCA, GRID, LIMIT_ORDER
    val chain     = varchar("chain", 50)
    val params    = jsonb("params")           // StrategyParams as JSONB
    val status    = varchar("status", 20)     // RUNNING, PAUSED, STOPPED
    val trades    = integer("trades").default(0)
    val pnl       = double("pnl").default(0.0)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

object ConversationsTable : UUIDTable("conversations") {
    val userId    = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val messages  = jsonb("messages")         // List<ChatMessage> as JSONB
    val summary   = text("summary").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

object TradeHistoryTable : UUIDTable("trade_history") {
    val userId       = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val strategyId   = reference("strategy_id", StrategiesTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val token        = varchar("token", 255)
    val action       = varchar("action", 10)  // BUY, SELL
    val amount       = double("amount")
    val price        = double("price")
    val txHash       = varchar("tx_hash", 255).nullable()
    val pnl          = double("pnl").nullable()
    val signalSource = varchar("signal_source", 255).nullable()
    val executedAt   = timestamp("executed_at").defaultExpression(CurrentTimestamp)
}
