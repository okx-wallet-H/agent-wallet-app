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
 * Database plugin — auto-detects PostgreSQL vs H2, Redis vs in-memory.
 * Local dev: zero dependencies (H2 + in-memory map).
 * Production: PostgreSQL + Redis.
 */
object DatabaseFactory {

    private var hikari: HikariDataSource? = null
    private var jedisPool: JedisPool? = null
    var isProduction = false
        private set

    fun init(config: AppConfig) {
        isProduction = config.isProduction()

        if (isProduction) {
            initPostgres(config)
            initRedis(config)
        } else {
            initH2()
        }

        // Auto-create tables
        transaction {
            SchemaUtils.create(
                UsersTable,
                StrategiesTable,
                ConversationsTable,
                TradeHistoryTable
            )
            // Ensure anonymous user exists (for unauthenticated requests)
            val anonExists = com.agentwallet.models.UserRepository.findByEmail("anonymous@local") != null
            if (!anonExists) {
                com.agentwallet.models.UserRepository.create("anonymous@local", "nopass")
            }
        }
    }

    private fun initPostgres(config: AppConfig) {
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
    }

    private fun initH2() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:agentwallet;DB_CLOSE_DELAY=-1"
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 5
        }
        hikari = HikariDataSource(hikariConfig)
        Database.connect(hikari!!)
    }

    private fun initRedis(config: AppConfig) {
        try {
            val redisConfig = JedisPoolConfig().apply {
                maxTotal = 10
                maxIdle = 5
                minIdle = 2
            }
            jedisPool = JedisPool(redisConfig, config.redisHost, config.redisPort)
            jedisPool!!.resource.use { it.ping() } // test connection
        } catch (e: Exception) {
            jedisPool = null // fall back to in-memory
        }
    }

    fun redis(): JedisPool? = jedisPool

    fun shutdown() {
        hikari?.close()
        jedisPool?.close()
    }
}

/**
 * Usage tracker — Redis when available, in-memory fallback.
 */
object RiskUsageTracker {
    private val memoryStore = mutableMapOf<String, Double>()

    fun get(userId: String): Double {
        val redis = DatabaseFactory.redis()
        return if (redis != null) {
            try {
                redis.resource.use { it.get("risk:daily:$userId")?.toDoubleOrNull() ?: 0.0 }
            } catch (e: Exception) {
                memoryStore[userId] ?: 0.0
            }
        } else {
            memoryStore[userId] ?: 0.0
        }
    }

    fun increment(userId: String, amount: Double) {
        val redis = DatabaseFactory.redis()
        if (redis != null) {
            try {
                redis.resource.use { j ->
                    j.incrByFloat("risk:daily:$userId", amount)
                    val now = java.time.LocalTime.now()
                    val secondsToMidnight = java.time.LocalTime.MAX.toSecondOfDay() - now.toSecondOfDay() + 1
                    j.expire("risk:daily:$userId", secondsToMidnight.toLong())
                }
            } catch (e: Exception) {
                memoryStore[userId] = (memoryStore[userId] ?: 0.0) + amount
            }
        } else {
            memoryStore[userId] = (memoryStore[userId] ?: 0.0) + amount
        }
    }

    fun reset(userId: String) {
        val redis = DatabaseFactory.redis()
        if (redis != null) {
            try { redis.resource.use { it.del("risk:daily:$userId") } }
            catch (e: Exception) { memoryStore.remove(userId) }
        } else {
            memoryStore.remove(userId)
        }
    }
}
