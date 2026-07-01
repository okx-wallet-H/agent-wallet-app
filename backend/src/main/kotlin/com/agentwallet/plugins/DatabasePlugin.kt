package com.agentwallet.plugins

import com.agentwallet.config.AppConfig
import com.agentwallet.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

/**
 * Database plugin — PostgreSQL (via Exposed + HikariCP) + Redis.
 * Auto-creates tables on init (MVP migration strategy).
 */
object DatabaseFactory {

    private var hikari: HikariDataSource? = null
    private var jedisPool: JedisPool? = null

    fun init(config: AppConfig) {
        // PostgreSQL
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.dbUrl
            username = config.dbUser
            password = config.dbPassword
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 30000
            connectionTimeout = 10000
        }
        hikari = HikariDataSource(hikariConfig)
        Database.connect(hikari!!)

        // Auto-create tables (MVP migration)
        transaction {
            SchemaUtils.create(
                UsersTable,
                StrategiesTable,
                ConversationsTable,
                TradeHistoryTable
            )
        }

        // Redis
        val redisConfig = JedisPoolConfig().apply {
            maxTotal = 10
            maxIdle = 5
            minIdle = 2
        }
        jedisPool = JedisPool(redisConfig, config.redisHost, config.redisPort)
    }

    fun redis(): JedisPool = jedisPool
        ?: throw IllegalStateException("Database not initialized. Call DatabaseFactory.init() first.")

    fun shutdown() {
        hikari?.close()
        jedisPool?.close()
    }
}

/**
 * Redis-backed daily usage tracker for RiskEngine.
 */
object RiskUsageTracker {
    private val jedis: redis.clients.jedis.Jedis
        get() = DatabaseFactory.redis().resource

    private fun key(userId: String): String = "risk:daily:$userId"

    fun get(userId: String): Double {
        return jedis.use { it.get(key(userId))?.toDoubleOrNull() ?: 0.0 }
    }

    fun increment(userId: String, amount: Double) {
        jedis.use { j ->
            j.incrByFloat(key(userId), amount)
            // TTL to midnight
            val now = java.time.LocalTime.now()
            val midnight = java.time.LocalTime.MAX  // 23:59:59.999
            val secondsToMidnight = midnight.toSecondOfDay() - now.toSecondOfDay() + 1
            j.expire(key(userId), secondsToMidnight.toLong())
        }
    }

    fun reset(userId: String) {
        jedis.use { it.del(key(userId)) }
    }
}
