package com.solana.serializers

import com.funkatronics.encoders.Base64
import com.funkatronics.kborsh.BorshDecoder
import com.funkatronics.kborsh.BorshEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class SolanaResponseSerializer<R>(dataSerializer: KSerializer<R>)
    : KSerializer<R?> {
    private val serializer = WrappedValue.serializer(dataSerializer)
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: R?) =
        encoder.encodeSerializableValue(serializer, WrappedValue(value))

    override fun deserialize(decoder: Decoder): R? =
        decoder.decodeSerializableValue(serializer).value
}

@Serializable
private class WrappedValue<V>(val value: V?)

internal object ByteArrayAsBase64JsonArraySerializer: KSerializer<ByteArray> {
    private val delegateSerializer = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: ByteArray) =
        encoder.encodeSerializableValue(delegateSerializer, listOf(
            Base64.encodeToString(value), "base64"
        ))

    override fun deserialize(decoder: Decoder): ByteArray {
        decoder.decodeSerializableValue(delegateSerializer).apply {
            if (contains("base64")) first { it != "base64" }.apply {
                return Base64.decode(this)
            }
            else throw(SerializationException("Not Base64"))
        }
    }
}

internal class BorshAsBase64JsonArraySerializer<T>(private val dataSerializer: KSerializer<T>): KSerializer<T?> {
    private val delegateSerializer = ByteArrayAsBase64JsonArraySerializer
    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun serialize(encoder: Encoder, value: T?) =
        encoder.encodeSerializableValue(delegateSerializer,
            value?.let {
                BorshEncoder().apply {
                    encodeSerializableValue(dataSerializer, value)
                }.borshEncodedBytes
            } ?: byteArrayOf()
        )

    override fun deserialize(decoder: Decoder): T? =
        decoder.decodeSerializableValue(delegateSerializer).run {
            if (this.isEmpty()) return null
            BorshDecoder(this).decodeSerializableValue(dataSerializer)
        }
}