@file:OptIn(ExperimentalSerializationApi::class)

package com.solana.rpc

import com.funkatronics.encoders.Base58
import com.funkatronics.encoders.Base64
import com.funkatronics.hash.Sha256
import com.solana.publickey.SolanaPublicKey
import com.solana.rpccore.JsonRpc20Request
import com.solana.transaction.Transaction
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.jvm.JvmStatic
import kotlin.random.Random

sealed class SolanaRpcRequest(
    method: String,
    params: List<JsonElement>? = null,
    configuration: Map<String, JsonElement?>? = null,
    id: String? = null
) : JsonRpc20Request(
    method,
    buildJsonArray {
        if (!params.isNullOrEmpty()) addAll(params)
        configuration?.filterValues { it != null && it !is JsonNull }?.let { config ->
            if (config.isNotEmpty()) addJsonObject {
                config.forEach { (k, v) -> put(k, v!!) }
            }
        }
    }.also {
        println(Json.encodeToString(JsonArray.serializer(), it))
    },
    id ?: generateRequestId(method)
) {
    companion object {
        @JvmStatic
        private fun generateRequestId(method: String) =
            Base64.encodeToString(Sha256.hash(
                "$method-${Random.nextInt(100000000, 999999999)}"
                    .encodeToByteArray()
            ))
    }
}

class AccountInfoRequest(
    publicKey: SolanaPublicKey,
    commitment: Commitment? = null,
    minContextSlot: Long? = null,
    dataSlice: DataSlice? = null,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getAccountInfo",
    params = listOf(JsonPrimitive(publicKey.base58())),
    configuration = mapOf(
        "encoding" to JsonPrimitive(Encoding.base64.getEncoding()),
        "commitment" to JsonPrimitive(commitment?.value),
        "minContextSlot" to JsonPrimitive(minContextSlot),
        "dataSlice" to Json.encodeToJsonElement(dataSlice),
    ),
    requestId
) {
    @Serializable
    data class DataSlice(val length: Long, val offset: Long)
}

class AirdropRequest(
    address: SolanaPublicKey,
    lamports: Long,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "requestAirdrop",
    params = listOf(
        JsonPrimitive(address.base58()),
        JsonPrimitive(lamports)
    ),
    id = requestId
)

class BalanceRequest(
    address: SolanaPublicKey,
    commitment: Commitment = Commitment.CONFIRMED,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getBalance",
    params = listOf(JsonPrimitive(address.base58())),
    configuration = mapOf(
        "commitment" to JsonPrimitive(commitment.value),
    ),
    requestId
)

class LatestBlockhashRequest(
    commitment: Commitment? = null,
    minContextSlot: Long? = null,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getLatestBlockhash",
    params = null,
    configuration = mapOf(
        "commitment" to JsonPrimitive(commitment?.value),
        "minContextSlot" to JsonPrimitive(minContextSlot),
    ),
    requestId
)

class SendTransactionRequest(
    transaction: Transaction,
    options: TransactionOptions,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "sendTransaction",
    params = listOf(JsonPrimitive(when (options.encoding) {
        Encoding.base58 -> Base58.encodeToString(transaction.serialize())
        Encoding.base64 -> Base64.encodeToString(transaction.serialize())
    })),
    configuration = mapOf(
        "encoding" to JsonPrimitive(options.encoding.getEncoding()),
        "skipPreflight" to JsonPrimitive(options.skipPreflight),
        "preflightCommitment" to JsonPrimitive(options.preflightCommitment.toString())
    ),
    requestId
)

class SignatureStatusesRequest(
    transactionIds: List<String>,
    searchTransactionHistory: Boolean = false,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getSignatureStatuses",
    params = listOf(JsonArray(transactionIds.map { JsonPrimitive(it) })),
    configuration = mapOf("searchTransactionHistory" to JsonPrimitive(searchTransactionHistory)),
    requestId
)

class RentExemptBalanceRequest(
    size: Long,
    commitment: Commitment? = null,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getMinimumBalanceForRentExemption",
    params = listOf(JsonPrimitive(size)),
    configuration = mapOf("commitment" to JsonPrimitive(commitment?.value)),
    requestId
)