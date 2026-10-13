package com.solana.rpc

import com.funkatronics.encoders.Base58
import com.funkatronics.encoders.Base64
import com.funkatronics.hash.Sha256
import com.solana.publickey.SolanaPublicKey
import com.solana.rpccore.JsonRpc20Request
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
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
                it != JsonNull &&
                        !(it is JsonObject && it.jsonObject.filterValues { it != JsonNull }.isEmpty()) &&
                        !(it is JsonArray && it.jsonArray.isEmpty() && it.jsonArray.none { it != JsonNull })
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

sealed class AccountRequest(
    method: String,
    params: (JsonArrayBuilder.() -> Unit)? = null,
    configuration: (JsonObjectBuilder.() -> Unit)? = null,
    id: String? = null
) : SolanaRpcRequest(
    method,
    params,
    configuration = {
        put("encoding", Encoding.BASE64.serialName())
        configuration?.invoke(this)
    },
    id
) {
    @Serializable
    data class DataSlice(val length: Long, val offset: Long)
}

class AccountInfoRequest(
    publicKey: SolanaPublicKey,
    commitment: Commitment? = null,
    minContextSlot: Long? = null,
    dataSlice: DataSlice? = null,
    requestId: String? = null
) : AccountRequest(
    method = "getAccountInfo",
    params = { add(publicKey.base58()) },
    configuration = {
        put("commitment", commitment?.serialName())
        put("minContextSlot", minContextSlot)
        put("dataSlice", buildJsonObject {
            put("length", dataSlice?.length)
            put("offset", dataSlice?.offset)
        })
    },
    requestId
)

class MultipleAccountsInfoRequest(
    publicKeys: List<SolanaPublicKey>,
    commitment: Commitment? = null,
    minContextSlot: Long? = null,
    dataSlice: DataSlice? = null,
    requestId: String? = null
) : AccountRequest(
    method = "getMultipleAccounts",
    params = { addJsonArray {
        publicKeys.forEach { add(it.base58()) }
    }},
    configuration = {
        put("commitment", commitment?.serialName())
        put("minContextSlot", minContextSlot)
        put("dataSlice", buildJsonObject {
            put("length", dataSlice?.length)
            put("offset", dataSlice?.offset)
        })
    },
    requestId
)

class ProgramAccountsRequest(
    program: SolanaPublicKey,
    commitment: Commitment? = null,
    minContextSlot: Long? = null,
    dataSlice: DataSlice? = null,
    filters: List<Filter>? = null,
    requestId: String? = null
) : AccountRequest(
    method = "getProgramAccounts",
    params = { add(program.base58()) },
    configuration = {
        put("withContext", true)
        put("commitment", commitment?.serialName())
        put("minContextSlot", minContextSlot)
        put("dataSlice", buildJsonObject {
            put("length", dataSlice?.length)
            put("offset", dataSlice?.offset)
        })
        filters?.let {
            put("filters", JsonArray(filters.map { it.toJsonObject() }))
        }
    },
    requestId
) {
    init {
        require((filters?.size ?: 0) < 4) { "Too many filters, maximum is 4" }
    }

    class DataSize(val dataSize: Long) : Filter
    class MemCompare(val offset: Long, val bytes: String) : Filter
    sealed interface Filter {
        fun toJsonObject() = buildJsonObject {
            when (this@Filter) {
                is DataSize -> put("dataSize", dataSize)
                is MemCompare -> {
                    put("offset", offset)
                    put("bytes", bytes)
                    put("encoding", Encoding.BASE64.serialName())
                }
            }
        }
    }
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
        put("commitment", commitment.serialName())
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
        put("commitment", commitment?.serialName())
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
        Encoding.BASE58 -> Base58.encodeToString(transaction.serialize())
        Encoding.BASE64 -> Base64.encodeToString(transaction.serialize())
    })},
    configuration = {
        put("encoding", options.encoding.serialName())
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
    configuration = { put("commitment", commitment?.serialName()) },
    requestId
)

class GetTransactionRequest(
    signature: String,
    commitment: Commitment = Commitment.FINALIZED,
    maxSupportedTransactionVersion: Long = 0,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getTransaction",
    params = { add(signature) },
    configuration = {
        put("commitment", commitment.serialName())
        put("maxSupportedTransactionVersion", maxSupportedTransactionVersion)
        put("encoding", "jsonParsed")
    },
    requestId
)

class GetFeeForMessageRequest(
    message: Message,
    commitment: Commitment = Commitment.PROCESSED,
    minContextSlot: Long? = null,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "getFeeForMessage",
    params = { add(message.serialize().run { Base64.encodeToString(this) }) },
    configuration = {
        put("commitment", commitment.serialName())
        put("minContextSlot", minContextSlot)
    },
    requestId
)

class SimulateTransactionRequest(
    transaction: Transaction,
    commitment: Commitment? = null,
    encoding: Encoding = Encoding.BASE64,
    replaceRecentBlockhash: Boolean? = null,
    sigVerify: Boolean? = null,
    minContextSlot: Long? = null,
    innerInstructions: Boolean? = null,
    accounts: List<SolanaPublicKey>? = null,
    attemptJsonParseAccounts: Boolean = false,
    requestId: String? = null
) : SolanaRpcRequest(
    method = "simulateTransaction",
    params = { add(transaction.serialize().run {
        when(encoding) {
            Encoding.BASE64 -> Base64.encodeToString(this)
            Encoding.BASE58 -> Base58.encodeToString(this)
        }
    }) },
    configuration = {
        put("commitment", commitment?.serialName())
        put("encoding", encoding.serialName())
        put("replaceRecentBlockhash", replaceRecentBlockhash)
        put("sigVerify", sigVerify)
        put("minContextSlot", minContextSlot)
        put("innerInstructions", innerInstructions)
        accounts?.let {
            putJsonObject("accounts") {
                putJsonArray("addresses") {
                    accounts.forEach { add(it.address) }
                }
                put("encoding", if (attemptJsonParseAccounts) "jsonParsed" else "base64")
            }
        }
    },
    requestId
)