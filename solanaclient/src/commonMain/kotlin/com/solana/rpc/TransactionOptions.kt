package com.solana.rpc

import kotlinx.serialization.SerialName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class Encoding(private val enc: String) {
    base64("base64"),
    base58("base58"),
    jsonParsed("jsonParsed");
    fun getEncoding(): String {
        return enc
    }
}

enum class Commitment(val value: String) {
    @SerialName("processed")
    PROCESSED("processed"),

    @SerialName("confirmed")
    CONFIRMED("confirmed"),

    @SerialName("finalized")
    FINALIZED("finalized");

    override fun toString(): String {
        return value
    }
}

data class TransactionOptions(
    val commitment: Commitment = Commitment.FINALIZED,
    val encoding: Encoding = Encoding.base64,
    val skipPreflight: Boolean = false,
    val preflightCommitment: Commitment = commitment,
    val timeout: Duration = 30.seconds
)