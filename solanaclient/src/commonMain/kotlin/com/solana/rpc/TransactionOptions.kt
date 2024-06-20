package com.solana.rpc

import com.funkatronics.encoders.Base58
import com.funkatronics.encoders.Base64
import kotlinx.serialization.SerialName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class Encoding(private val enc: String) {
    @SerialName("base64")
    BASE64("base64"),
    @SerialName("base58")
    BASE58("base58");

    fun decode(encoded: String) = when (this) {
        BASE64 -> Base64.decode(encoded)
        BASE58 -> Base58.decode(encoded)
    }

    fun encode(bytes: ByteArray) = when (this) {
        BASE64 -> Base64.encodeToString(bytes)
        BASE58 -> Base58.encodeToString(bytes)
    }

    fun serialName() = toString()
    override fun toString() = enc
}

enum class Commitment(private val value: String) {
    @SerialName("processed")
    PROCESSED("processed"),

    @SerialName("confirmed")
    CONFIRMED("confirmed"),

    @SerialName("finalized")
    FINALIZED("finalized");

    fun serialName() = toString()
    override fun toString() = value
}

data class TransactionOptions(
    val commitment: Commitment = Commitment.FINALIZED,
    val encoding: Encoding = Encoding.BASE64,
    val skipPreflight: Boolean = false,
    val preflightCommitment: Commitment = commitment,
    val timeout: Duration = 30.seconds
)