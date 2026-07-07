package com.agentwallet.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

fun safeUuid(id: String): UUID = try { UUID.fromString(id) } catch (e: Exception) { UUID(0, 0) }
val NIL_UUID = UUID(0, 0)

object UserRepository {

    fun create(email: String): User = transaction {
        val id = UsersTable.insertAndGetId {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = ""
        }
        User(id = id.value.toString(), email = email, passwordHash = "")
    }

    fun findByEmail(email: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.email eq email }
            .singleOrNull()?.let { rowToUser(it) }
    }

    fun findById(id: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq safeUuid(id) }
            .singleOrNull()?.let { rowToUser(it) }
    }

    fun markVerified(userId: String) = transaction {
        UsersTable.update({ UsersTable.id eq safeUuid(userId) }) {
            it[onchainosVerified] = true
        }
    }

    fun updateWalletCache(userId: String, evmAddr: String, solAddr: String, balance: Double) = transaction {
        UsersTable.update({ UsersTable.id eq safeUuid(userId) }) {
            it[cachedEvmAddress] = evmAddr
            it[cachedSolAddress] = solAddr
            it[cachedBalance] = balance
            it[cachedAt] = java.time.Instant.now()
        }
    }

    private fun rowToUser(row: ResultRow): User = User(
        id = row[UsersTable.id].value.toString(),
        email = row[UsersTable.email],
        passwordHash = row[UsersTable.passwordHash],
        onchainosVerified = row[UsersTable.onchainosVerified],
        cachedEvmAddress = row[UsersTable.cachedEvmAddress],
        cachedSolAddress = row[UsersTable.cachedSolAddress],
        cachedBalance = row[UsersTable.cachedBalance],
        createdAt = row[UsersTable.createdAt].toEpochMilli()
    )
}

object StrategyRepository {

    fun create(strategy: Strategy): Strategy = transaction {
        val paramsJson = json.encodeToString(strategy.params)
        val id = StrategiesTable.insertAndGetId {
            it[userId] = safeUuid(strategy.userId)
            it[name] = strategy.name
            it[type] = strategy.type.name
            it[chain] = strategy.chain
            it[StrategiesTable.params] = paramsJson
            it[status] = strategy.status.name
        }
        strategy.copy(id = id.value.toString())
    }

    fun findByUserId(userId: String): List<Strategy> = transaction {
        StrategiesTable.selectAll()
            .where { StrategiesTable.userId eq safeUuid(userId) }
            .orderBy(StrategiesTable.createdAt, SortOrder.DESC)
            .map { rowToStrategy(it) }
    }

    fun findById(id: String): Strategy? = transaction {
        StrategiesTable.selectAll()
            .where { StrategiesTable.id eq safeUuid(id) }
            .singleOrNull()?.let { rowToStrategy(it) }
    }

    fun update(id: String, strategy: Strategy): Strategy = transaction {
        val paramsJson = json.encodeToString(strategy.params)
        StrategiesTable.update({ StrategiesTable.id eq safeUuid(id) }) {
            it[name] = strategy.name
            it[status] = strategy.status.name
            it[StrategiesTable.params] = paramsJson
            it[updatedAt] = java.time.Instant.now()
        }
        findById(id)!!
    }

    fun delete(id: String) = transaction {
        StrategiesTable.deleteWhere { StrategiesTable.id eq safeUuid(id) }
    }

    private fun rowToStrategy(row: ResultRow): Strategy {
        val paramsJson = row[StrategiesTable.params]
        val params = try { json.decodeFromString<StrategyParams>(paramsJson) }
            catch (e: Exception) { StrategyParams(maxPerTx = 100.0, dailyLimit = 500.0) }
        return Strategy(
            id = row[StrategiesTable.id].value.toString(),
            userId = row[StrategiesTable.userId].value.toString(),
            name = row[StrategiesTable.name],
            type = try { StrategyType.valueOf(row[StrategiesTable.type]) } catch (e: Exception) { StrategyType.SNIPER },
            chain = row[StrategiesTable.chain],
            params = params,
            status = try { StrategyStatus.valueOf(row[StrategiesTable.status]) } catch (e: Exception) { StrategyStatus.RUNNING },
            trades = row[StrategiesTable.trades],
            pnl = row[StrategiesTable.pnl],
            createdAt = row[StrategiesTable.createdAt].toEpochMilli(),
            updatedAt = row[StrategiesTable.updatedAt].toEpochMilli()
        )
    }
}

object TradeHistoryRepository {
    fun create(
        userId: String, strategyId: String?, token: String, action: String,
        amount: Double, price: Double, txHash: String?, pnl: Double?, signalSource: String?
    ) = transaction {
        TradeHistoryTable.insert {
            it[TradeHistoryTable.userId] = safeUuid(userId)
            it[TradeHistoryTable.strategyId] = strategyId?.let { safeUuid(it) }
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
            .where { TradeHistoryTable.userId eq safeUuid(userId) }
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

object ConversationRepository {
    fun upsert(conversationId: String, userId: String, messages: List<ChatMessage>) = transaction {
        val cid = safeUuid(conversationId)
        val existing = ConversationsTable.selectAll().where { ConversationsTable.id eq cid }.singleOrNull()
        if (existing != null) {
            val existingJson = existing[ConversationsTable.messages]
            val existingMessages = try { json.decodeFromString<List<ChatMessage>>(existingJson) } catch (e: Exception) { emptyList() }
            val updatedJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ChatMessage.serializer()), existingMessages + messages)
            ConversationsTable.update({ ConversationsTable.id eq cid }) {
                it[ConversationsTable.messages] = updatedJson
                it[updatedAt] = java.time.Instant.now()
            }
        } else {
            ConversationsTable.insert {
                it[id] = cid
                it[ConversationsTable.userId] = safeUuid(userId)
                it[ConversationsTable.messages] = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ChatMessage.serializer()), messages)
            }
        }
    }

    fun findByUserId(userId: String): List<Pair<String, List<ChatMessage>>> = transaction {
        ConversationsTable.selectAll()
            .where { ConversationsTable.userId eq safeUuid(userId) }
            .orderBy(ConversationsTable.updatedAt, SortOrder.DESC)
            .map { row ->
                val id = row[ConversationsTable.id].value.toString()
                val msgs = try { json.decodeFromString<List<ChatMessage>>(row[ConversationsTable.messages]) } catch (e: Exception) { emptyList() }
                id to msgs
            }
    }
}
