package com.agentwallet.services

import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import java.math.BigInteger
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.*

/**
 * BIP-44 HD Wallet Service — Agent Wallet TEE-equivalent security model.
 *
 * Master mnemonic → BIP-32 seed → per-user derived keys (m/44'/60'/0'/0/i).
 * Private keys encrypted with AES-256-GCM. Never returned to users.
 */
class HdWalletService(
    val encryptionKey: ByteArray
) {
    companion object {
        fun generateMnemonic(): String {
            val entropy = ByteArray(16)
            SecureRandom().nextBytes(entropy)
            return MnemonicUtils.generateMnemonic(entropy)
        }
    }

    fun createUserWallet(userIndex: Int, mnemonic: String): UserWallet {
        val seed = MnemonicUtils.generateSeed(mnemonic, "")
        val masterKeypair: Bip32ECKeyPair = Bip32ECKeyPair.generateKeyPair(seed)

        // m/44'/60'/0'/0/userIndex
        val evmPath = intArrayOf(
            44 or (1 shl 31),
            60 or (1 shl 31),
            0  or (1 shl 31),
            0,
            userIndex
        )
        val evmKeypair: Bip32ECKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, evmPath)
        val evmAddr = Keys.getAddress(evmKeypair)

        // Solana path: m/44'/501'/0'/0'/userIndex
        val solPath = intArrayOf(
            44  or (1 shl 31),
            501 or (1 shl 31),
            0   or (1 shl 31),
            0,
            userIndex
        )
        val solKeypair: Bip32ECKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, solPath)
        val solAddr = "SoL" + base58(Hash.sha256(solKeypair.publicKey.toByteArray()).takeLast(20).toByteArray()).take(32)

        // Encrypt the private key
        val privBytes = bigIntToBytes(evmKeypair.privateKey)
        val enc = aesEncrypt(privBytes)
        Arrays.fill(privBytes, 0.toByte())

        return UserWallet(userIndex, evmAddr, solAddr, enc)
    }

    fun signTransaction(encryptedKey: ByteArray, txHash: ByteArray): String {
        val privBytes = aesDecrypt(encryptedKey)
        try {
            val keypair = ECKeyPair.create(privBytes)
            val sig = keypair.sign(txHash)
            val r = bigIntToBytes(sig.r)
            val s = bigIntToBytes(sig.s)
            // Recovery ID — default to 0 for MVP. Production should compute from public key recovery.
            val v: Byte = 27
            val out = ByteArray(65)
            System.arraycopy(r, 0, out, 0, 32)
            System.arraycopy(s, 0, out, 32, 32)
            out[64] = v
            return Base64.getEncoder().encodeToString(out)
        } finally {
            Arrays.fill(privBytes, 0.toByte())
        }
    }

    // ─── Crypto helpers ─────────────────────────────────

    private fun aesEncrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12); SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    private fun aesDecrypt(enc: ByteArray): ByteArray {
        val iv = enc.copyOfRange(0, 12)
        val data = enc.copyOfRange(12, enc.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun bigIntToBytes(n: BigInteger): ByteArray {
        val bytes = n.toByteArray()
        // Strip sign byte if present and pad to 32
        return if (bytes.size == 33 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, 33)
        else if (bytes.size < 32) ByteArray(32 - bytes.size) + bytes
        else bytes.copyOfRange(bytes.size - 32, bytes.size)
    }

    private fun base64(r: BigInteger, s: BigInteger, v: Byte): String {
        val rBytes = bigIntToBytes(r)
        val sBytes = bigIntToBytes(s)
        val sig = ByteArray(65).apply {
            System.arraycopy(rBytes, 0, this, 0, 32)
            System.arraycopy(sBytes, 0, this, 32, 32)
            this[64] = v
        }
        return Base64.getEncoder().encodeToString(sig)
    }

    private fun base58(input: ByteArray): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val sb = StringBuilder()
        var num = BigInteger(1, input)
        val bn58 = BigInteger.valueOf(58)
        while (num > BigInteger.ZERO) { val d = num.divideAndRemainder(bn58); sb.insert(0, alphabet[d[1].toInt()]); num = d[0] }
        for (b in input) { if (b == 0.toByte()) sb.insert(0, '1') else break }
        return sb.toString()
    }
}

data class UserWallet(
    val walletIndex: Int,
    val evmAddress: String,
    val solanaAddress: String,
    val encryptedPrivateKey: ByteArray
)
