package com.solana.networking

import com.solana.rpccore.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

class Rpc20Driver(private val url: String,
                  private val httpDriver: HttpNetworkDriver,
                  private val json: Json) : JsonRpcDriver {

    constructor(url: String, httpDriver: HttpNetworkDriver) : this(url, httpDriver, Json {
        ignoreUnknownKeys = true
    })

    override suspend fun <R> makeRequest(request: RpcRequest,
                                         resultSerializer: DeserializationStrategy<R>): Rpc20Response<R> {
        require(request.jsonrpc == "2.0") { "Request is not a JSON RPC 2.0 request (${request.jsonrpc})"}
        return httpDriver.makeHttpRequest(
            HttpPostRequest(
                url = url,
                properties = mapOf("Content-Type" to "application/json; charset=utf-8"),
                body = json.encodeToString(
                    JsonRpc20Request.serializer(), JsonRpc20Request(
                        request.method,
                        request.params,
                        request.id
                    )
                )
            )
        ).run {
            try {
                json.decodeFromString(Deserializer(resultSerializer), this)
            } catch (e: Exception) {
                Rpc20Response(error = RpcError(-1, e.message ?: e.toString()))
            }
        }
    }

    internal class HttpPostRequest(
        override val url: String,
        override val properties: Map<String, String>,
        override val body: String? = null
    ) : HttpRequest {
        override val method = "POST"
    }

    private class Deserializer<R>(deserializer: DeserializationStrategy<R>): DeserializationStrategy<Rpc20Response<R>> {
        private val deserializer = Rpc20Response.serializer(object : KSerializer<R> {
            override val descriptor = deserializer.descriptor
            override fun serialize(encoder: Encoder, value: R) {}
            override fun deserialize(decoder: Decoder): R = decoder.decodeSerializableValue(deserializer)
        })
        override val descriptor = deserializer.descriptor
        override fun deserialize(decoder: Decoder): Rpc20Response<R> =
            decoder.decodeSerializableValue(deserializer)
    }
}