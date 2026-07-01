package com.agentwallet

import com.agentwallet.models.TradeAction
import com.agentwallet.models.TradeIntent
import com.agentwallet.risk.RiskEngine
import org.junit.Test
import kotlin.test.*

class RiskEngineTest {

    private val engine = RiskEngine(
        perTxLimit = 500.0,
        dailyLimit = 2000.0,
        blacklistedTokens = setOf("0xSCAM")
    )

    @Test
    fun `should approve valid trade`() {
        val intent = TradeIntent(
            action = TradeAction.BUY,
            fromToken = "ETH",
            toToken = "USDC",
            amount = 100.0,
            chain = "1",
            slippage = 0.01
        )
        val result = engine.check("user_1", intent)
        assertTrue(result.approved, "Valid trade should be approved")
        assertTrue(result.checks.all { it.passed })
    }

    @Test
    fun `should reject trade exceeding per-tx limit`() {
        val intent = TradeIntent(
            action = TradeAction.BUY,
            fromToken = "ETH",
            toToken = "USDC",
            amount = 600.0,  // > 500
            chain = "1",
            slippage = 0.01
        )
        val result = engine.check("user_2", intent)
        assertFalse(result.approved, "Over-limit trade should be rejected")
        assertNotNull(result.reason)
        assertTrue(result.reason!!.contains("单笔限额"))
    }

    @Test
    fun `should reject blacklisted token`() {
        val intent = TradeIntent(
            action = TradeAction.BUY,
            fromToken = "ETH",
            toToken = "0xSCAM",
            amount = 50.0,
            chain = "1",
            slippage = 0.01
        )
        val result = engine.check("user_3", intent)
        assertFalse(result.approved, "Blacklisted token should be rejected")
    }

    @Test
    fun `should reject slippage out of bounds`() {
        val intent = TradeIntent(
            action = TradeAction.BUY,
            fromToken = "ETH",
            toToken = "USDC",
            amount = 100.0,
            chain = "1",
            slippage = 0.15  // > 10%
        )
        val result = engine.check("user_4", intent)
        assertFalse(result.approved, "Excessive slippage should be rejected")
    }

    @Test
    fun `should reject slippage too low`() {
        val intent = TradeIntent(
            action = TradeAction.BUY,
            fromToken = "ETH",
            toToken = "USDC",
            amount = 100.0,
            chain = "1",
            slippage = 0.0005  // < 0.1%
        )
        val result = engine.check("user_5", intent)
        assertFalse(result.approved, "Too-low slippage should be rejected")
    }

    @Test
    fun `should track daily usage`() {
        val userId = "user_daily_test"
        val intent1 = TradeIntent(TradeAction.BUY, "ETH", "USDC", 800.0, "1", 0.01)
        val intent2 = TradeIntent(TradeAction.BUY, "ETH", "USDC", 800.0, "1", 0.01)
        val intent3 = TradeIntent(TradeAction.BUY, "ETH", "USDC", 500.0, "1", 0.01)

        val result1 = engine.check(userId, intent1)
        assertTrue(result1.approved)

        val result2 = engine.check(userId, intent2)
        assertTrue(result2.approved)

        // Third trade should exceed daily limit (800+800+500 = 2100 > 2000)
        val result3 = engine.check(userId, intent3)
        assertFalse(result3.approved, "Should reject when daily limit exceeded")
        assertTrue(result3.reason!!.contains("日限额"))
    }
}
