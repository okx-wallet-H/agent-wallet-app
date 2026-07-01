package com.agentwallet

import com.agentwallet.models.TradeAction
import com.agentwallet.models.TradeIntent
import com.agentwallet.plugins.RiskUsageTracker
import com.agentwallet.risk.RiskEngine
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class RiskEngineTest {

    private val engine = RiskEngine(
        perTxLimit = 500.0,
        dailyLimit = 2000.0,
        blacklistedTokens = setOf("0xSCAM")
    )

    @Before
    fun resetState() {
        RiskUsageTracker.reset("user_1")
        RiskUsageTracker.reset("user_2")
        RiskUsageTracker.reset("user_3")
        RiskUsageTracker.reset("user_4")
        RiskUsageTracker.reset("user_5")
        RiskUsageTracker.reset("user_daily_test")
    }

    @Test
    fun `should approve valid trade`() {
        val intent = TradeIntent(TradeAction.BUY, "ETH", "USDC", 100.0, "1", 0.01)
        val result = engine.check("user_1", intent)
        assertTrue(result.approved, "Valid trade should be approved")
        assertTrue(result.checks.all { it.passed })
    }

    @Test
    fun `should reject trade exceeding per-tx limit`() {
        val intent = TradeIntent(TradeAction.BUY, "ETH", "USDC", 600.0, "1", 0.01)
        val result = engine.check("user_2", intent)
        assertFalse(result.approved, "Over-limit trade should be rejected")
        assertNotNull(result.reason)
        assertTrue(result.reason!!.contains("单笔限额"))
    }

    @Test
    fun `should reject blacklisted token`() {
        val intent = TradeIntent(TradeAction.BUY, "ETH", "0xSCAM", 50.0, "1", 0.01)
        val result = engine.check("user_3", intent)
        assertFalse(result.approved, "Blacklisted token should be rejected")
    }

    @Test
    fun `should reject slippage out of bounds`() {
        val intent = TradeIntent(TradeAction.BUY, "ETH", "USDC", 100.0, "1", 0.15)
        val result = engine.check("user_4", intent)
        assertFalse(result.approved, "Excessive slippage should be rejected")
    }

    @Test
    fun `should reject slippage too low`() {
        val intent = TradeIntent(TradeAction.BUY, "ETH", "USDC", 100.0, "1", 0.0005)
        val result = engine.check("user_5", intent)
        assertFalse(result.approved, "Too-low slippage should be rejected")
    }

    @Test
    fun `should track daily usage`() {
        val userId = "user_daily_test"
        // per-tx limit is 500, so each tx must be <= 500
        val i1 = TradeIntent(TradeAction.BUY, "ETH", "USDC", 500.0, "1", 0.01)
        val i2 = TradeIntent(TradeAction.BUY, "ETH", "USDC", 500.0, "1", 0.01)
        val i3 = TradeIntent(TradeAction.BUY, "ETH", "USDC", 500.0, "1", 0.01)
        val i4 = TradeIntent(TradeAction.BUY, "ETH", "USDC", 500.0, "1", 0.01)
        val i5 = TradeIntent(TradeAction.BUY, "ETH", "USDC", 500.0, "1", 0.01)

        assertTrue(engine.check(userId, i1).approved)
        assertTrue(engine.check(userId, i2).approved)
        assertTrue(engine.check(userId, i3).approved)
        assertTrue(engine.check(userId, i4).approved)
        // 5th trade: 500*5 = 2500 > 2000 daily limit
        assertFalse(engine.check(userId, i5).approved, "Should exceed daily limit")
    }
}
