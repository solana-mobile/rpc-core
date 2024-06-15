package com.solana.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun <T> DeserializationStrategy<T>.wrappedSerializer() = object : KSerializer<T> {
    override val descriptor = this@wrappedSerializer.descriptor
    override fun deserialize(decoder: Decoder): T = decoder.decodeSerializableValue(this@wrappedSerializer)
    override fun serialize(encoder: Encoder, value: T) = throw NotImplementedError("Serialize not implemented")
}

fun <T> KSerializer<T>.deserializer() = object : DeserializationStrategy<T> {
    override val descriptor = this@deserializer.descriptor
    override fun deserialize(decoder: Decoder): T = decoder.decodeSerializableValue(this@deserializer)
}

infix fun <T> SerializationStrategy<T>.with(deserializationStrategy: DeserializationStrategy<T>) =
    object : KSerializer<T> {
        override val descriptor: SerialDescriptor = this@with.descriptor

        override fun serialize(encoder: Encoder, value: T) =
            encoder.encodeSerializableValue(this@with, value)

        override fun deserialize(decoder: Decoder): T =
            decoder.decodeSerializableValue(deserializationStrategy)
    }