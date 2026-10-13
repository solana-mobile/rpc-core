package com.solana.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
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

@Serializable
data class TransactionDetails(
    val blockTime: Long,
    val meta: TransactionMetadata,
    val slot: ULong,
    val transaction: JsonParsedTransaction,
    val version: JsonElement? = null
)

@Serializable
data class SimulationResult(
    val accounts: List<AccountInfo<List<String>>?>? = null,
    val err: JsonElement? = null,
    val innerInstructions: JsonObject? = null,
    val loadedAccountsDataSize: UInt? = null,
    val logs: List<String>? = null,
    val replacementBlockhash: BlockhashResponse? = null,
    val returnData: JsonObject? = null,
    val unitsConsumed: ULong? = null,
)