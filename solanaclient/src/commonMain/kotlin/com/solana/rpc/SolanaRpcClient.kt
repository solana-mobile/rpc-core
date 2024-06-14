package com.solana.rpc

import com.solana.networking.HttpNetworkDriver
import com.solana.networking.Rpc20Driver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpccore.RpcRequest
import com.solana.serializers.SolanaResponseSerializer
import com.solana.transaction.Transaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import kotlin.math.pow

class SolanaRpcClient(
    private val rpcDriver: Rpc20Driver,
    private val defaultTransactionOptions: TransactionOptions = TransactionOptions()
) {

    constructor(
        url: String, networkDriver: HttpNetworkDriver,
        defaultTransactionOptions: TransactionOptions = TransactionOptions()
    ) : this(Rpc20Driver(url, networkDriver), defaultTransactionOptions)

    suspend fun requestAirdrop(address: SolanaPublicKey, amountSol: Float, requestId: String? = null) =
        makeRequest(
            AirdropRequest(address, (amountSol * 10f.pow(9)).toLong(), requestId),
            String.serializer()
        )

    suspend fun <D> getAccountInfo(
        serializer: KSerializer<D>,
        publicKey: SolanaPublicKey,
        commitment: Commitment? = null,
        minContextSlot: Long? = null,
        dataSlice: AccountInfoRequest.DataSlice? = null,
        requestId: String? = null
    ) = makeRequest(
        AccountInfoRequest(publicKey, commitment, minContextSlot, dataSlice, requestId),
        SolanaAccountSerializer(serializer)
    )

    suspend inline fun <reified D> getAccountInfo(
        publicKey: SolanaPublicKey,
        commitment: Commitment? = null,
        minContextSlot: Long? = null,
        dataSlice: AccountInfoRequest.DataSlice? = null,
        requestId: String? = null
    ) = getAccountInfo<D>(serializer(), publicKey, commitment, minContextSlot, dataSlice, requestId)

    suspend fun getBalance(
        address: SolanaPublicKey,
        commitment: Commitment = Commitment.CONFIRMED,
        requestId: String? = null
    ) = makeRequest(
        BalanceRequest(address, commitment, requestId),
        SolanaResponseSerializer(Long.serializer())
    )

    suspend fun getLatestBlockhash(
        commitment: Commitment? = null,
        minContextSlot: Long? = null,
        requestId: String? = null
    ) = makeRequest(
        LatestBlockhashRequest(commitment, minContextSlot, requestId),
        SolanaResponseSerializer(BlockhashResponse.serializer())
    )

    suspend fun getMinBalanceForRentExemption(
        size: Long,
        commitment: Commitment? = null,
        requestId: String? = null
    ) = makeRequest(RentExemptBalanceRequest(size, commitment, requestId), Long.serializer())

    suspend fun <D> getMultipleAccounts(
        serializer: KSerializer<D>,
        publicKeys: List<SolanaPublicKey>,
        commitment: Commitment? = null,
        minContextSlot: Long? = null,
        dataSlice: AccountInfoRequest.DataSlice? = null,
        requestId: String? = null
    ) = makeRequest(
        MultipleAccountsInfoRequest(publicKeys, commitment, minContextSlot, dataSlice, requestId),
        MultipleAccountsSerializer(serializer)
    )

    suspend inline fun <reified D> getMultipleAccounts(
        publicKeys: List<SolanaPublicKey>,
        commitment: Commitment? = null,
        minContextSlot: Long? = null,
        dataSlice: AccountInfoRequest.DataSlice? = null,
        requestId: String? = null
    ) = getMultipleAccounts<D>(serializer(), publicKeys, commitment, minContextSlot, dataSlice, requestId)

    suspend fun getSignatureStatuses(
        signatures: List<String>,
        searchTransactionHistory: Boolean = false,
        requestId: String? = null
    ) = makeRequest(
        SignatureStatusesRequest(signatures, searchTransactionHistory, requestId),
        SolanaResponseSerializer(ListSerializer(SignatureStatus.serializer().nullable))
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

    suspend fun confirmTransaction(
        transactionSignature: String,
        options: TransactionOptions = defaultTransactionOptions
    ): Result<Boolean> = withTimeout(options.timeout) {
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

    private suspend inline fun <T> makeRequest(request: RpcRequest, serializer: KSerializer<T>) =
        rpcDriver.makeRequest(request, serializer)

    private suspend inline fun <reified T> makeRequest(request: RpcRequest) =
        rpcDriver.makeRequest<T>(request, serializer())
}