package com.agentwallet.models

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

// ─── User Repository ──────────────────────────────────

object UserRepository {

    fun create(email: String, passwordHash: String): User = transaction {
        val id = UsersTable.insertAndGetId {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
        }
        User(id = id.value.toString(), email = email, passwordHash = passwordHash)
    }

    fun findByEmail(email: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.email eq email }
            .singleOrNull()?.let { rowToUser(it) }
    }

    fun findById(id: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq UUID.fromString(id) }
            .singleOrNull()?.let { rowToUser(it) }
    }

    fun updateWalletId(userId: String, walletId: String) = transaction {
        UsersTable.update({ UsersTable.id eq UUID.fromString(userId) }) {
            it[coinbaseWalletId] = walletId
        }
    }

    private fun rowToUser(row: ResultRow): User = User(
        id = row[UsersTable.id].value.toString(),
        email = row[UsersTable.email],
        passwordHash = row[UsersTable.passwordHash],
        coinbaseWalletId = row[UsersTable.coinbaseWalletId],
        createdAt = row[UsersTable.createdAt].toInstant().toEpochMilli()
    )
}

// ─── Strategy Repository ──────────────────────────────

object StrategyRepository {

    fun create(strategy: Strategy): Strategy = transaction {
        val id = StrategiesTable.insertAndGetId {
            it[userId] = UUID.fromString(strategy.userId)
            it[name] = strategy.name
            it[type] = strategy.type.name
            it[chain] = strategy.chain
            it[params] = json.encodeToJsonElement(StrategyParams.serializer(), strategy.params)
            it[status] = strategy.status.name
        }
        strategy.copy(id = id.value.toString())
    }

    fun findByUserId(userId: String): List<Strategy> = transaction {
        StrategiesTable.selectAll()
            .where { StrategiesTable.userId eq UUID.fromString(userId) }
            .orderBy(StrategiesTable.createdAt, SortOrder.DESC)
            .map { rowToStrategy(it) }
    }

    fun findById(id: String): Strategy? = transaction {
        StrategiesTable.selectAll()
            .where { StrategiesTable.id eq UUID.fromString(id) }
            .singleOrNull()?.let { rowToStrategy(it) }
    }

    fun update(id: String, strategy: Strategy): Strategy = transaction {
        StrategiesTable.update({ StrategiesTable.id eq UUID.fromString(id) }) {
            it[name] = strategy.name
            it[status] = strategy.status.name
            it[params] = json.encodeToJsonElement(StrategyParams.serializer(), strategy.params)
            it[updatedAt] = java.time.Instant.now()
        }
        findById(id)!!
    }

    fun delete(id: String) = transaction {
        StrategiesTable.deleteWhere { StrategiesTable.id eq UUID.fromString(id) }
    }

    private fun rowToStrategy(row: ResultRow): Strategy = Strategy(
        id = row[StrategiesTable.id].value.toString(),
        userId = row[StrategiesTable.userId].value.toString(),
        name = row[StrategiesTable.name],
        type = StrategyType.valueOf(row[StrategiesTable.type]),
        chain = row[StrategiesTable.chain],
        params = json.decodeFromJsonElement(StrategyParams.serializer(), row[StrategiesTable.params]),
        status = StrategyStatus.valueOf(row[StrategiesTable.status]),
        trades = row[StrategiesTable.trades],
        pnl = row[StrategiesTable.pnl],
        createdAt = row[StrategiesTable.createdAt].toInstant().toEpochMilli(),
        updatedAt = row[StrategiesTable.updatedAt].toInstant().toEpochMilli()
    )
}

// ─── Trade History Repository ─────────────────────────

object TradeHistoryRepository {

    fun create(
        userId: String,
        strategyId: String?,
        token: String,
        action: String,
        amount: Double,
        price: Double,
        txHash: String?,
        pnl: Double?,
        signalSource: String?
    ) = transaction {
        TradeHistoryTable.insert {
            it[TradeHistoryTable.userId] = UUID.fromString(userId)
            it[TradeHistoryTable.strategyId] = strategyId?.let { UUID.fromString(it) }
            it[TradeHistoryTable.token] = token
            it[TradeHistoryTable.action] = action
            it[TradeHistoryTable.amount] = amount
            it[TradeHistoryTable.price] = price
            it[TradeHistoryTable.txHash] = txHash
            it[TradeHistoryTable.pnl] = pnl
            it[TradeHistoryTable.signalSource] = signalSource
        }
    }

    fun findByUserId(userId: String, limit: Int = 20): List<Map<String, Any?>> = transaction {
        TradeHistoryTable.selectAll()
            .where { TradeHistoryTable.userId eq UUID.fromString(userId) }
            .orderBy(TradeHistoryTable.executedAt, SortOrder.DESC)
            .limit(limit)
            .map { row ->
                mapOf(
                    "token" to row[TradeHistoryTable.token],
                    "action" to row[TradeHistoryTable.action],
                    "amount" to row[TradeHistoryTable.amount],
                    "price" to row[TradeHistoryTable.price],
                    "txHash" to row[TradeHistoryTable.txHash],
                    "pnl" to row[TradeHistoryTable.pnl],
                    "signalSource" to row[TradeHistoryTable.signalSource]
                )
            }
    }
}

// ─── Conversation Repository ──────────────────────────

object ConversationRepository {

    fun upsert(conversationId: String, userId: String, messages: List<ChatMessage>) = transaction {
        val existing = ConversationsTable.selectAll()
            .where { ConversationsTable.id eq UUID.fromString(conversationId) }
            .singleOrNull()

        if (existing != null) {
            // Append to existing conversation
            val existingMessages = json.decodeFromString<List<ChatMessage>>(
                existing[ConversationsTable.messages].toString()
            )
            ConversationsTable.update({ ConversationsTable.id eq UUID.fromString(conversationId) }) {
                it[ConversationsTable.messages] = json.encodeToJsonElement(existingMessages + messages)
                it[updatedAt] = java.time.Instant.now()
            }
        } else {
            ConversationsTable.insert {
                it[id] = UUID.fromString(conversationId)
                it[ConversationsTable.userId] = UUID.fromString(userId)
                it[messages] = json.encodeToJsonElement(messages)
            }
        }
    }

    fun findByUserId(userId: String): List<Pair<String, List<ChatMessage>>> = transaction {
        ConversationsTable.selectAll()
            .where { ConversationsTable.userId eq UUID.fromString(userId) }
            .orderBy(ConversationsTable.updatedAt, SortOrder.DESC)
            .map { row ->
                val id = row[ConversationsTable.id].value.toString()
                val msgs = json.decodeFromString<List<ChatMessage>>(
                    row[ConversationsTable.messages].toString()
                )
                id to msgs
            }
    }
}
