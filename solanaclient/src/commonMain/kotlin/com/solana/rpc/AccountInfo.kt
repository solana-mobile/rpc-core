package com.solana.rpc

import com.solana.publickey.SolanaPublicKey
import com.solana.serializers.*
import com.solana.serializers.BorshAsAsEncodedDataArrayDeserializer
import com.solana.serializers.ByteArrayAsEncodedDataArrayDeserializer
import kotlinx.serialization.DeserializationStrategy
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

fun SolanaAccountDeserializer() =
    SolanaResponseDeserializer(
        AccountInfo.serializer(
            ByteArrayAsEncodedDataArrayDeserializer.asSerializer()
        )
    )

fun <A> SolanaAccountDeserializer(deserializer: DeserializationStrategy<A>) =
    SolanaResponseDeserializer(
        AccountInfo.serializer(
            BorshAsAsEncodedDataArrayDeserializer(deserializer).asSerializer()
        )
    )

fun MultipleAccountsDeserializer() =
    SolanaResponseDeserializer(
        ListSerializer(
            AccountInfo.serializer(
                ByteArrayAsEncodedDataArrayDeserializer.asSerializer()
            ).nullable
        )
    )

fun <A> MultipleAccountsDeserializer(deserializer: DeserializationStrategy<A>) =
    SolanaResponseDeserializer(
        ListSerializer(
            AccountInfo.serializer(
                BorshAsAsEncodedDataArrayDeserializer(deserializer).asSerializer()
            ).nullable
        )
    )

fun ProgramAccountsDeserializer() =
    SolanaResponseDeserializer(
        ListSerializer(
            AccountInfoWithPublicKey.serializer(
                ByteArrayAsEncodedDataArrayDeserializer.asSerializer()
            )
        )
    )

fun <A> ProgramAccountsDeserializer(deserializer: DeserializationStrategy<A>) =
    SolanaResponseDeserializer(
        ListSerializer(
            AccountInfoWithPublicKey.serializer(
                BorshAsAsEncodedDataArrayDeserializer(deserializer).asSerializer()
            )
        )
    )