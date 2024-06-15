package com.solana.rpc

import com.solana.publickey.SolanaPublicKey
import com.solana.serialization.deserializer
import com.solana.serialization.wrappedSerializer
import com.solana.serializers.BorshAsAsEncodedDataArrayDeserializer
import com.solana.serializers.BorshAsBase64JsonArraySerializer
import com.solana.serializers.ByteArrayAsEncodedDataArrayDeserializer
import com.solana.serializers.SolanaResponseSerializer
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

fun <A> SolanaAccountDeserializer(deserializer: DeserializationStrategy<A>) =
    SolanaResponseSerializer(
        AccountInfo.serializer(
            BorshAsBase64JsonArraySerializer(deserializer.wrappedSerializer())
        )
    ).deserializer()

fun MultipleAccountsDeserializer() =
    SolanaResponseSerializer(
        ListSerializer(
            AccountInfo.serializer(
                ByteArrayAsEncodedDataArrayDeserializer.wrappedSerializer()
            ).nullable
        )
    )

fun <A> MultipleAccountsDeserializer(deserializer: DeserializationStrategy<A>) =
    SolanaResponseSerializer(
        ListSerializer(
            AccountInfo.serializer(
                BorshAsAsEncodedDataArrayDeserializer(deserializer).wrappedSerializer()
            ).nullable
        )
    ).deserializer()

fun ProgramAccountsDeserializer() =
    ListSerializer(
        AccountInfoWithPublicKey.serializer(
            ByteArrayAsEncodedDataArrayDeserializer.wrappedSerializer()
        )
    ).deserializer()

fun <A> ProgramAccountsDeserializer(deserializer: DeserializationStrategy<A>) =
    ListSerializer(
        AccountInfoWithPublicKey.serializer(
            BorshAsAsEncodedDataArrayDeserializer(deserializer).wrappedSerializer()
        )
    ).deserializer()