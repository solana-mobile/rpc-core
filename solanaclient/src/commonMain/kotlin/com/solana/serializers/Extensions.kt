package com.solana.serializers

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal fun <T> DeserializationStrategy<T>.asSerializer() = object : KSerializer<T> {
    override val descriptor = this@asSerializer.descriptor
    override fun deserialize(decoder: Decoder): T = decoder.decodeSerializableValue(this@asSerializer)
    override fun serialize(encoder: Encoder, value: T) = throw NotImplementedError("Serialize not implemented")
}

internal fun <T> KSerializer<T>.deserializer() = object : DeserializationStrategy<T> {
    override val descriptor = this@deserializer.descriptor
    override fun deserialize(decoder: Decoder): T = decoder.decodeSerializableValue(this@deserializer)
}

internal infix fun <T> SerializationStrategy<T>.with(deserializationStrategy: DeserializationStrategy<T>) =
    object : KSerializer<T> {
        override val descriptor: SerialDescriptor = this@with.descriptor

        override fun serialize(encoder: Encoder, value: T) =
            encoder.encodeSerializableValue(this@with, value)

        override fun deserialize(decoder: Decoder): T =
            decoder.decodeSerializableValue(deserializationStrategy)
    }