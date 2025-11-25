package com.solana.rpc

import com.funkatronics.encoders.Base58
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Blockhash
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class TransactionMetadata(
    val err: Map<String, JsonArray>? = null,
    val fee: ULong,
    val computeUnitsConsumed: ULong,
    val preBalances: List<ULong>,
    val postBalances: List<ULong>,
    val innerInstructions: List<Instruction>,
    val preTokenBalances: List<TokenBalance>,
    val postTokenBalances: List<TokenBalance>,
    val logMessages: List<String>,
    val rewards: List<JsonObject>,
)

@Serializable
data class Instruction(
    val index: Int,
    val instructions: List<InnerInstruction>
)

@Serializable
data class InnerInstruction(
    val programIdIndex: Int,
    val accounts: List<Int>,
    @SerialName("data") val base58EncodedData: String
) {
    val rawData = Base58.decode(base58EncodedData)
}

@Serializable
data class TokenBalance(
    val accountIndex: Int,
    val mint: SolanaPublicKey,
    val owner: SolanaPublicKey? = null,
    val programId: SolanaPublicKey? = null,
    val uiTokenAmount: UiTokenAmount
)

@Serializable
data class UiTokenAmount(
    val amount: ULong,
    val decimals: Byte,
    @SerialName("uiAmountString") val uiAmount: String
)

@Serializable
data class JsonParsedTransaction(
    val message: JsonParsedMessage,
    val signatures: List<String>
)

@Serializable
data class JsonParsedMessage(
    val accountKeys: List<JsonParsedAccountMeta>,
    val instructions: List<JsonParsedInstruction>,
    val recentBlockhash: Blockhash
)

@Serializable
data class JsonParsedInstruction(
    val program: String? = null,
    val programId: SolanaPublicKey,
    val stackHeight: ULong? = null,
    val parsed: JsonElement? = null,
)

@Serializable
data class JsonParsedAccountMeta(
    @SerialName("pubkey") val publicKey: SolanaPublicKey,
    val source: String,
    @SerialName("signer") val isSigner: Boolean,
    @SerialName("writable") val isWritable: Boolean,
)