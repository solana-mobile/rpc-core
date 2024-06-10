package com.solana.rpc

import com.funkatronics.encoders.Base58
import com.funkatronics.encoders.Base64
import com.solana.networking.HttpNetworkDriver
import com.solana.networking.Rpc20Driver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpccore.JsonRpc20Request
import com.solana.rpccore.RpcRequest
import com.solana.serializers.SolanaResponseSerializer
import com.solana.transaction.Transaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.math.pow
import kotlin.random.Random

class SolanaRpcClient(
    val rpcDriver: Rpc20Driver,
    private val defaultTransactionOptions: TransactionOptions = TransactionOptions()
) {

    constructor(
        url: String, networkDriver: HttpNetworkDriver,
        defaultTransactionOptions: TransactionOptions = TransactionOptions()
    ) : this(Rpc20Driver(url, networkDriver), defaultTransactionOptions)

    suspend inline fun <T> makeRequest(request: RpcRequest, serializer: KSerializer<T>) =
        rpcDriver.makeRequest(request, serializer)

    suspend inline fun <reified T> makeRequest(request: RpcRequest) =
        rpcDriver.makeRequest<T>(request, serializer())

    suspend fun requestAirdrop(address: SolanaPublicKey, amountSol: Float, requestId: String? = null) =
        makeRequest(
            AirdropRequest(address, (amountSol * 10f.pow(9)).toLong(), requestId),
            String.serializer()
        )

    suspend fun getBalance(
        address: SolanaPublicKey,
        commitment: Commitment = Commitment.CONFIRMED,
        requestId: String? = null
    ) = makeRequest(
        BalanceRequest(address, commitment, requestId),
        SolanaResponseSerializer(Long.serializer())
    )

    suspend fun getMinBalanceForRentExemption(
        size: Long,
        commitment: Commitment? = null,
        requestId: String? = null
    ) = makeRequest(RentExemptBalanceRequest(size, commitment, requestId), Long.serializer())

    suspend fun getLatestBlockhash(
        commitment: Commitment? = null,
        minContextSlot: Long? = null,
        requestId: String? = null
    ) = makeRequest(
        LatestBlockhashRequest(commitment, minContextSlot, requestId),
        SolanaResponseSerializer(BlockhashResponse.serializer())
    )

    suspend fun sendTransaction(
        transaction: Transaction,
        options: TransactionOptions = defaultTransactionOptions,
        requestId: String? = null
    ) = makeRequest(SendTransactionRequest(transaction, options, requestId), String.serializer())

    suspend fun sendAndConfirmTransaction(
        transaction: Transaction,
        options: TransactionOptions = defaultTransactionOptions
    ) = sendTransaction(transaction, options).apply {
        result?.let { confirmTransaction(it, options) }
    }

    suspend fun getSignatureStatuses(
        signatures: List<String>,
        searchTransactionHistory: Boolean = false,
        requestId: String? = null
    ) = makeRequest(
        SignatureStatusesRequest(signatures, searchTransactionHistory, requestId),
        SolanaResponseSerializer(ListSerializer(SignatureStatus.serializer().nullable))
    )

    suspend fun confirmTransaction(
        transactionSignature: String,
        options: TransactionOptions = defaultTransactionOptions
    ): Result<Boolean> =
        withTimeout(options.timeout) {
            val requiredCommitment = options.commitment.ordinal

            suspend fun confirmationStatus() =
                getSignatureStatuses(listOf(transactionSignature), false)
                    .result?.first()

            // wait for desired transaction status
            var inc = 1L
            while (true) {
                val confirmationOrdinal = confirmationStatus().also {
                    it?.err?.let { error ->
                        return@withTimeout Result.failure(Error(error.toString()))
                    }
                }?.confirmationStatus?.ordinal ?: -1

                if (confirmationOrdinal >= requiredCommitment) {
                    return@withTimeout Result.success(true)
                } else {
                    // Exponential delay before retrying.
                    delay(500 * inc)
                }
                // breakout after timeout
                if (!isActive) break
                inc++
            }

            return@withTimeout Result.success(isActive)
        }

    //region Requests
    sealed class SolanaRpcRequest(
        method: String,
        params: JsonElement?,
        id: String? = null
    ) : JsonRpc20Request(
        method,
        params,
        id ?: "$method-${Random.nextInt(100000000, 999999999)}"
    )

    class AirdropRequest(
        address: SolanaPublicKey,
        lamports: Long,
        requestId: String? = null
    ) : SolanaRpcRequest(
        method = "requestAirdrop",
        params = buildJsonArray {
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
        params = buildJsonArray {
            add(address.base58())
            addJsonObject {
                put("commitment", commitment.value)
            }
        },
        requestId
    )

    class LatestBlockhashRequest(
        commitment: Commitment? = null,
        minContextSlot: Long? = null,
        requestId: String? = null
    ) : SolanaRpcRequest(
        method = "getLatestBlockhash",
        params = buildJsonArray {
            if (commitment != null || minContextSlot != null) {
                addJsonObject {
                    commitment?.let { put("commitment", commitment.value) }
                    minContextSlot?.let { put("minContextSlot", minContextSlot) }
                }
            }
        },
        requestId
    )

    class SendTransactionRequest(
        transaction: Transaction,
        options: TransactionOptions,
        requestId: String? = null
    ) : SolanaRpcRequest(
            method = "sendTransaction",
            params = buildJsonArray {
                add(when (options.encoding) {
                    Encoding.base58 -> Base58.encodeToString(transaction.serialize())
                    Encoding.base64 -> Base64.encodeToString(transaction.serialize())
                })
                addJsonObject {
                    put("encoding", options.encoding.getEncoding())
                    put("skipPreflight", options.skipPreflight)
                    put("preflightCommitment", options.preflightCommitment.toString())
                }
            },
            requestId
        )

    class SignatureStatusesRequest(
        transactionIds: List<String>,
        searchTransactionHistory: Boolean = false,
        requestId: String? = null
    ) : SolanaRpcRequest(
        method = "getSignatureStatuses",
        params = buildJsonArray {
            addJsonArray { transactionIds.forEach { add(it) } }
            addJsonObject {
                put("searchTransactionHistory", searchTransactionHistory)
            }
        },
        requestId
    )

    class RentExemptBalanceRequest(
        size: Long,
        commitment: Commitment? = null,
        requestId: String? = null
    ) : SolanaRpcRequest(
        method = "getMinimumBalanceForRentExemption",
        params = buildJsonArray {
            add(size)
            commitment?.let {
                addJsonObject {
                    put("commitment", commitment.value)
                }
            }
        },
        requestId
    )
    //endregion

    //region Responses
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
    //endregion
}