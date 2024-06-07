package com.solana.rpc

import com.funkatronics.encoders.Base58
import com.solana.networking.HttpNetworkDriver
import com.solana.networking.Rpc20Driver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpccore.JsonRpc20Request
import com.solana.serializers.SolanaResponseSerializer
import com.solana.transaction.Transaction
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class SolanaRpcClient(val rpcDriver: Rpc20Driver) {

    constructor(url: String, networkDriver: HttpNetworkDriver): this(Rpc20Driver(url, networkDriver))

    suspend fun requestAirdrop(address: SolanaPublicKey, amountSol: Float, requestId: String? = null) =
        rpcDriver.makeRequest(
            AirdropRequest(address, (amountSol*10f.pow(9)).toLong(), requestId),
            String.serializer()
        )

    suspend fun getBalance(address: SolanaPublicKey, commitment: String = "confirmed", requestId: String? = null) =
        rpcDriver.makeRequest(BalanceRequest(address, commitment, requestId), SolanaResponseSerializer(Long.serializer()))

    suspend fun getMinBalanceForRentExemption(size: Long, commitment: String? = null, requestId: String? = null) =
        rpcDriver.makeRequest(RentExemptBalanceRequest(size, commitment, requestId), Long.serializer())

    suspend fun getLatestBlockhash(commitment: String? = null, minContextSlot: Long? = null, requestId: String? = null) =
        rpcDriver.makeRequest(LatestBlockhashRequest(commitment, minContextSlot, requestId), SolanaResponseSerializer(BlockhashResponse.serializer()))

    suspend fun sendTransaction(transaction: Transaction, skipPreflight: Boolean = false, requestId: String? = null) =
        rpcDriver.makeRequest(SendTransactionRequest(transaction, skipPreflight, requestId), String.serializer())

    suspend fun sendAndConfirmTransaction(transaction: Transaction) =
        sendTransaction(transaction).apply {
            result?.let { confirmTransaction(it) }
        }

    suspend fun getSignatureStatuses(signatures: List<String>, searchTransactionHistory: Boolean = false, requestId: String? = null) =
        rpcDriver.makeRequest(SignatureStatusesRequest(signatures, searchTransactionHistory, requestId),
            SolanaResponseSerializer(ListSerializer(SignatureStatus.serializer().nullable))
        )

    suspend fun confirmTransaction(
        signature: String,
        commitment: String = "confirmed",
        timeout: Long = 15000
    ): Result<String> = withTimeout(timeout) {
        suspend fun getStatus() =
            getSignatureStatuses(listOf(signature))
                .result?.first()

        val timeSource = TimeSource.Monotonic

        // wait for desired transaction status
        while(getStatus()?.confirmationStatus != commitment) {

            // wait a bit before retrying
            val mark = timeSource.markNow()
            var inc = 0
            while(mark.elapsedNow() < 0.3.seconds && isActive) { inc++ }

            if (!isActive) break // breakout after timeout
        }

        Result.success(signature)
    }

    //region Requests
    sealed class SolanaRpcRequest(method: String, params: JsonElement?, id: String? = null)
        : JsonRpc20Request(method, params, id ?: "$method-${Random.nextInt(100000000, 999999999)}")

    class AirdropRequest(address: SolanaPublicKey, lamports: Long, requestId: String? = null)
        : SolanaRpcRequest(
        method = "requestAirdrop",
        params = buildJsonArray {
            add(address.base58())
            add(lamports)
        },
        id = requestId
    )

    class BalanceRequest(address: SolanaPublicKey, commitment: String = "confirmed", requestId: String? = null)
        : SolanaRpcRequest(
        method = "getBalance",
        params = buildJsonArray {
            add(address.base58())
            addJsonObject {
                put("commitment", commitment)
            }
        },
        requestId
    )

    class LatestBlockhashRequest(commitment: String? = null, minContextSlot: Long? = null, requestId: String? = null)
        : SolanaRpcRequest(
        method = "getLatestBlockhash",
        params = buildJsonArray {
            if (commitment != null || minContextSlot!= null) {
                addJsonObject {
                    commitment?.let { put("commitment", commitment) }
                    minContextSlot?.let { put("minContextSlot", minContextSlot) }
                }
            }
        },
        requestId
    )

    class SendTransactionRequest(transaction: Transaction, skipPreflight: Boolean = false, requestId: String? = null)
        : SolanaRpcRequest(
        method = "sendTransaction",
        params = buildJsonArray {
            add(Base58.encodeToString(transaction.serialize()))
            addJsonObject {
                put("skipPreflight", skipPreflight)
            }
        },
        requestId
    )

    class SignatureStatusesRequest(transactionIds: List<String>, searchTransactionHistory: Boolean = false, requestId: String? = null)
        : SolanaRpcRequest(
        method = "getSignatureStatuses",
        params = buildJsonArray {
            addJsonArray { transactionIds.forEach { add(it) } }
            addJsonObject {
                put("searchTransactionHistory", searchTransactionHistory)
            }
        },
        requestId
    )

    class RentExemptBalanceRequest(size: Long, commitment: String? = null, requestId: String? = null)
        : SolanaRpcRequest(
        method = "getMinimumBalanceForRentExemption",
        params = buildJsonArray {
            add(size)
            commitment?.let {
                addJsonObject {
                    put("commitment", commitment)
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
        var confirmationStatus: String?
    )
    //endregion
}