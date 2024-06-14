package com.solana.rpc

import com.solana.publickey.SolanaPublicKey
import com.solana.serializers.BorshAsBase64JsonArraySerializer
import com.solana.serializers.SolanaResponseSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class AccountInfo<D>(
    val data: D?,
    val executable: Boolean,
    val lamports: ULong,
    val owner: SolanaPublicKey,
    val rentEpoch: ULong,
    val size: ULong? = null
)

fun <A> SolanaAccountSerializer(serializer: KSerializer<A>) =
    SolanaResponseSerializer(
        AccountInfo.serializer(
            BorshAsBase64JsonArraySerializer(serializer)
        )
    )