package com.solana.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class SolanaResponse<V>(val context: Context, val value: V?)

@Serializable
class Context(val apiVersion: String, val slot: ULong)

@Serializable
class BlockhashResponse(
    val blockhash: String,
    val lastValidBlockHeight: Long
)

@Serializable
data class SignatureStatus(
    val slot: Long,
    val confirmations: Long?,
    var err: JsonObject?,
    var confirmationStatus: Commitment?
)