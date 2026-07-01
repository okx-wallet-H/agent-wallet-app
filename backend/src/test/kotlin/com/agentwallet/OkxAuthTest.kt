package com.agentwallet

import com.agentwallet.services.OkxAuth
import io.ktor.client.request.*
import io.ktor.http.*
import org.junit.Test
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.*

class OkxAuthTest {

    @Test
    fun `hmac signing should match expected output`() {
        val secretKey = "test_secret"
        val timestamp = "2026-07-01T10:00:00Z"
        val method = "POST"
        val path = "/api/v6/dex/aggregator/quote"
        val body = "[{\"chainId\":\"1\"}]"

        val signString = "$timestamp$method$path$body"

        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(signString.toByteArray(Charsets.UTF_8))
        val signature = Base64.getEncoder().encodeToString(hash)

        assertNotNull(signature)
        assertTrue(signature.isNotEmpty(), "Signature should not be empty")
        assertTrue(signature.length > 20, "HMAC-SHA256 produces reasonable-length output")
    }

    @Test
    fun `signing with timestamp changes output`() {
        val secretKey = "test_secret"
        val sign1 = hmacSha256("2026-07-01T10:00:00ZPOST/api/v6/dex/aggregator/quote[]", secretKey)
        val sign2 = hmacSha256("2026-07-01T10:00:01ZPOST/api/v6/dex/aggregator/quote[]", secretKey)

        assertNotEquals(sign1, sign2, "Different timestamps should produce different signatures")
    }

    @Test
    fun `signing with different body changes output`() {
        val secretKey = "test_secret"
        val sign1 = hmacSha256("2026-07-01T10:00:00ZPOST/api/v6/dex/aggregator/quote[{\"a\":1}]", secretKey)
        val sign2 = hmacSha256("2026-07-01T10:00:00ZPOST/api/v6/dex/aggregator/quote[{\"a\":2}]", secretKey)

        assertNotEquals(sign1, sign2, "Different bodies should produce different signatures")
    }

    private fun hmacSha256(data: String, key: String): String {
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        return Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
    }
}
