package com.solana.serializers

import com.funkatronics.kborsh.Borsh
import com.solana.rpc.Encoding
import com.solana.rpc.SolanaResponse
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class SolanaResponseDeserializer<R>(dataSerializer: DeserializationStrategy<R>)
    : DeserializationStrategy<R?> {
    private val serializer = SolanaResponse.serializer(dataSerializer.asSerializer())
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): R? =
        decoder.decodeSerializableValue(serializer).value
}

internal class ByteArrayAsEncodedDataArraySerializer(val encoding: Encoding) : SerializationStrategy<ByteArray> {
    private val delegateSerializer = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: ByteArray) =
        encoder.encodeSerializableValue(
            delegateSerializer, listOf(encoding.encode(value), encoding.serialName())
        )
}

internal object ByteArrayAsEncodedDataArrayDeserializer: DeserializationStrategy<ByteArray> {
    private val delegateSerializer = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun deserialize(decoder: Decoder): ByteArray {
        decoder.decodeSerializableValue(delegateSerializer).apply {
            Encoding.entries.forEach { enc ->
                if (contains(enc.serialName()))
                    return enc.decode(first { it != enc.serialName() })
            }
            throw(SerializationException("Unknown encoding: ${this.toTypedArray().contentToString()}"))
        }
    }
}

internal class BorshAsAsEncodedDataArraySerializationStrategy<T>(
    private val dataSerializer: SerializationStrategy<T>,
    encoding: Encoding,
    private val borsh: Borsh = Borsh
) : SerializationStrategy<T?> {
    private val delegateSerializer = ByteArrayAsEncodedDataArraySerializer(encoding)

    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun serialize(encoder: Encoder, value: T?) =
        encoder.encodeSerializableValue(delegateSerializer,
            value?.let {
                borsh.encodeToByteArray(dataSerializer, value)
            } ?: byteArrayOf()
        )
}

internal class BorshAsAsEncodedDataArrayDeserializer<T>(private val dataSerializer: DeserializationStrategy<T>,
                                                        private val borsh: Borsh = Borsh): DeserializationStrategy<T?> {
    private val delegateDeserializer = ByteArrayAsEncodedDataArrayDeserializer

    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun deserialize(decoder: Decoder): T? =
        decoder.decodeSerializableValue(delegateDeserializer).run {
            if (this.isEmpty()) return null
            borsh.decodeFromByteArray(dataSerializer, this)
        }
}

internal class BorshAsBase64JsonArraySerializer<T>(dataSerializer: KSerializer<T>): KSerializer<T?> {
    private val borsh = Borsh
    private val delegateSerializer = BorshAsAsEncodedDataArraySerializationStrategy(dataSerializer, Encoding.BASE64, borsh)
    private val delegateDeserializer = BorshAsAsEncodedDataArrayDeserializer(dataSerializer, borsh)

    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun serialize(encoder: Encoder, value: T?) =
        encoder.encodeSerializableValue(delegateSerializer, value)

    override fun deserialize(decoder: Decoder): T? =
        decoder.decodeSerializableValue(delegateDeserializer)
}