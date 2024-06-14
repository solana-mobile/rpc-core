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
    params: (JsonArrayBuilder.() -> Unit)? = null,
    configuration: (JsonObjectBuilder.() -> Unit)? = null,
    id: String? = null
) : JsonRpc20Request(
    method,
    buildJsonArray {
        params?.invoke(this)
        configuration?.let {
            buildJsonObject(configuration).filterValues {
                it != JsonNull && !(it is JsonObject && it.jsonObject.filterValues { it != JsonNull }.isEmpty())
            }
        }?.let {
            if (it.isNotEmpty()) add(JsonObject(it))
        }
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
    params = { add(publicKey.base58()) },
    configuration = {
        put("encoding", Encoding.base64.getEncoding())
        put("commitment", commitment?.value)
        put("minContextSlot", minContextSlot)
        put("dataSlice", buildJsonObject {
            put("length", dataSlice?.length)
            put("offset", dataSlice?.offset)
        })
    },
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
    params = {
        add(address.base58())
        add(lamports)
    },
    id = requestId
)

class BalanceRequest(
    address: SolanaPublicKey,
    commitment: Commitment = Commitment.CONFIRMED,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getBalance",
    params = { add(address.base58()) },
    configuration = {
        put("commitment", commitment.value)
    },
    requestId
)

class LatestBlockhashRequest(
    commitment: Commitment? = null,
    minContextSlot: Long? = null,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getLatestBlockhash",
    params = null,
    configuration = {
        put("commitment", commitment?.value)
        put("minContextSlot", minContextSlot)
    },
    requestId
)

class SendTransactionRequest(
    transaction: Transaction,
    options: TransactionOptions,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "sendTransaction",
    params = { add(when (options.encoding) {
        Encoding.base58 -> Base58.encodeToString(transaction.serialize())
        Encoding.base64 -> Base64.encodeToString(transaction.serialize())
    })},
    configuration = {
        put("encoding", options.encoding.getEncoding())
        put("skipPreflight", options.skipPreflight)
        put("preflightCommitment", options.preflightCommitment.toString())
    },
    requestId
)

class SignatureStatusesRequest(
    transactionIds: List<String>,
    searchTransactionHistory: Boolean = false,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getSignatureStatuses",
    params = { addJsonArray {
        transactionIds.forEach { add(it) }
    }},
    configuration = {
        put("searchTransactionHistory", searchTransactionHistory)
    },
    requestId
)

class RentExemptBalanceRequest(
    size: Long,
    commitment: Commitment? = null,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getMinimumBalanceForRentExemption",
    params = { add(size) },
    configuration = { put("commitment", commitment?.value) },
    requestId
)