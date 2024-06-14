package com.solana.rpc

import com.solana.publickey.SolanaPublicKey
import com.solana.serializers.BorshAsBase64JsonArraySerializer
import com.solana.serializers.SolanaResponseSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable

@Serializable
data class AccountInfo<D>(
    val data: D?,
    val executable: Boolean,
    val lamports: ULong,
    val owner: SolanaPublicKey,
    val rentEpoch: ULong,
    val size: ULong? = null
)

@Serializable
data class AccountInfoWithPublicKey<P>(
    val account: AccountInfo<P>,
    @SerialName("pubkey") val publicKey: String
)

fun <A> SolanaAccountSerializer(serializer: KSerializer<A>) =
    SolanaResponseSerializer(
        AccountInfo.serializer(
            BorshAsBase64JsonArraySerializer(serializer)
        )
    )

fun <A> MultipleAccountsSerializer(serializer: KSerializer<A>) =
    SolanaResponseSerializer(
        ListSerializer(
            AccountInfo.serializer(
                BorshAsBase64JsonArraySerializer(serializer)
            ).nullable
        )
    )

fun <A> ProgramAccountsSerializer(serializer: KSerializer<A>) =
    ListSerializer(
        AccountInfoWithPublicKey.serializer(
            BorshAsBase64JsonArraySerializer(serializer)
        ).nullable
    )