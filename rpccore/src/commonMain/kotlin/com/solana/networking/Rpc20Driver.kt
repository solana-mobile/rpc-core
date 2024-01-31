package com.solana.networking

import com.solana.rpccore.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class Rpc20Driver(private val url: String,
                  private val httpDriver: HttpNetworkDriver,
                  private val json: Json) : JsonRpcDriver {

    constructor(url: String, httpDriver: HttpNetworkDriver) : this(url, httpDriver, Json {
        ignoreUnknownKeys = true
    })

    override suspend fun <R> makeRequest(request: RpcRequest, resultSerializer: KSerializer<R>): Rpc20Response<R> {
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
                json.decodeFromString(Rpc20Response.serializer(resultSerializer), this)
            } catch (e: Exception) {
                Rpc20Response(error = RpcError(-1, e.message ?: this))
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
}