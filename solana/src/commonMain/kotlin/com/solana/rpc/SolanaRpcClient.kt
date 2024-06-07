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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class SolanaRpcClient(val rpcDriver: Rpc20Driver) {

    constructor(url: String, networkDriver: HttpNetworkDriver): this(Rpc20Driver(url, networkDriver))

    suspend fun requestAirdrop(address: SolanaPublicKey, amountSol: Float) =
        rpcDriver.makeRequest(
            AirdropRequest(address, (amountSol*10f.pow(9)).toLong()),
            String.serializer()
        )

    suspend fun getBalance(address: SolanaPublicKey, commitment: String = "confirmed") =
        rpcDriver.makeRequest(BalanceRequest(address, commitment), SolanaResponseSerializer(Long.serializer()))

    suspend fun getMinBalanceForRentExemption(size: Long, commitment: String? = null) =
        rpcDriver.makeRequest(RentExemptBalanceRequest(size, commitment), Long.serializer())

    suspend fun getLatestBlockhash() =
        rpcDriver.makeRequest(LatestBlockhashRequest(), SolanaResponseSerializer(BlockhashResponse.serializer()))

    suspend fun sendTransaction(transaction: Transaction) =
        rpcDriver.makeRequest(SendTransactionRequest(transaction), String.serializer())

    suspend fun sendAndConfirmTransaction(transaction: Transaction) =
        sendTransaction(transaction).apply {
            result?.let { confirmTransaction(it) }
        }

    suspend fun getSignatureStatuses(signatures: List<String>) =
        rpcDriver.makeRequest(SignatureStatusesRequest(signatures),
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
            while(timeSource.markNow() - mark < 0.3.seconds && isActive) { inc++ }

            if (!isActive) break // breakout after timeout
        }

        Result.success(signature)
    }

    class AirdropRequest(address: SolanaPublicKey, lamports: Long, requestId: String = "1")
        : JsonRpc20Request(
        method = "requestAirdrop",
        params = buildJsonArray {
            add(address.base58())
            add(lamports)
        },
        id = requestId
    )

    class BalanceRequest(address: SolanaPublicKey, commitment: String = "confirmed", requestId: String = "1")
        : JsonRpc20Request(
        method = "getBalance",
        params = buildJsonArray {
            add(address.base58())
            addJsonObject {
                put("commitment", commitment)
            }
        },
        requestId
    )

    class LatestBlockhashRequest(commitment: String = "confirmed", requestId: String = "1")
        : JsonRpc20Request(
        method = "getLatestBlockhash",
        params = buildJsonArray {
            addJsonObject {
                put("commitment", commitment)
            }
        },
        requestId
    )

    @Serializable
    class BlockhashResponse(
        val blockhash: String,
        val lastValidBlockHeight: Long
    )

    class SendTransactionRequest(transaction: Transaction, skipPreflight: Boolean = true, requestId: String = "1")
        : JsonRpc20Request(
        method = "sendTransaction",
        params = buildJsonArray {
            add(Base58.encodeToString(transaction.serialize()))
            addJsonObject {
                put("skipPreflight", skipPreflight)
            }
        },
        requestId
    )

    class SignatureStatusesRequest(transactionIds: List<String>, searchTransactionHistory: Boolean = false, requestId: String = "1")
        : JsonRpc20Request(
        method = "getSignatureStatuses",
        params = buildJsonArray {
            addJsonArray { transactionIds.forEach { add(it) } }
            addJsonObject {
                put("searchTransactionHistory", searchTransactionHistory)
            }
        },
        requestId
    )

    @Serializable
    data class SignatureStatus(
        val slot: Long,
        val confirmations: Long?,
        var err: JsonObject?,
        var confirmationStatus: String?
    )

    class RentExemptBalanceRequest(size: Long, commitment: String? = null, requestId: String = "1")
        : JsonRpc20Request(
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
}