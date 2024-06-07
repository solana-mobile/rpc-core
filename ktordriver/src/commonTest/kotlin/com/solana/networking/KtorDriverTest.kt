package com.solana.networking

import com.solana.rpccore.JsonRpc20Request
import com.solana.rpccore.JsonRpcRequest
import com.solana.rpccore.makeRequest
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.test.*

class KtorDriverTest {

    @Test
    fun testKtorNetworkDriverPostRequest() = runTest {
        // given
        val ktorClient = buildMockJsonHttpClient("{}")
        val driver = KtorNetworkDriver(ktorClient)
        val request = object : HttpRequest {
            override val url = "https:///someendpoint.com/post"
            override val method = "POST"
            override val body = "Hello World!"
            override val properties: Map<String, String> = mapOf()
        }

        // when
        val result = runCatching {
            driver.makeHttpRequest(request)
        }

        // then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun testKtorNetworkDriverGetRequest() = runTest {
        // given
        val ktorClient = buildMockJsonHttpClient("{}")
        val driver = KtorNetworkDriver(ktorClient)
        val request = object : HttpRequest {
            override val url = "https:///someendpoint.com/get"
            override val method = "GET"
            override val body = "Hello World!"
            override val properties: Map<String, String> = mapOf()
        }

        // when
        val result = runCatching {
            driver.makeHttpRequest(request)
        }

        // then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun testKtorNetworkDriverWithRpcDriver() = runTest {
        // given
        val rpcUrl = "https://api.somerpc.com"
        val rpcRequest = JsonRpc20Request("callMethod", id = "1234")
        val mockedResponse = """
            {
                "jsonrpc":"2.0",
                "result": {
                    "number": 1234,
                    "value":"a value"
                },
                "id":"1234"
            }
        """.trimIndent()

        val ktorClient = buildMockJsonHttpClient(mockedResponse)
        val httpDriver = KtorNetworkDriver(ktorClient)
        val rpcDriver = Rpc20Driver(rpcUrl, httpDriver)

        data class Result(val number: Int, val value: String)
        val serializer = object : KSerializer<Result> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Result")
            override fun serialize(encoder: Encoder, value: Result) { TODO("Not yet implemented") }
            override fun deserialize(decoder: Decoder): Result =
                decoder.decodeSerializableValue(JsonObject.serializer()).let { json ->
                    Result(json["number"]!!.jsonPrimitive.int, json["value"]!!.jsonPrimitive.content)
                }
        }

        // when
        val result = rpcDriver.makeRequest(rpcRequest, serializer)

        // then
        assertNull(result.error)
        assertNotNull(result.result)
        assertEquals(1234, result.result?.number)
        assertEquals("a value", result.result?.value)
    }

    @Test
    fun testKtorNetworkDriverWithRpcDriverReturnsRpcError() = runTest {
        // given
        val rpcUrl = "https://api.mocked.solana.com"
        val rpcRequest = JsonRpc20Request("invalidMethod", id = "1234")
        val mockedResponse = """
            {
                "jsonrpc":"2.0",
                "error":{
                    "code":-32601
                    "message":"Method not found"
                },
                "id":"${rpcRequest.id}"
            }
        """.trimIndent()

        val ktorClient = buildMockJsonHttpClient(mockedResponse)
        val httpDriver = KtorNetworkDriver(ktorClient)
        val rpcDriver = Rpc20Driver(rpcUrl, httpDriver)

        // when
        val rpcResponse = rpcDriver.makeRequest(rpcRequest)

        // then
        assertNotNull(rpcResponse.error)
        assertNull(rpcResponse.result)
    }

    private fun buildMockJsonHttpClient(responseJson: String) =
        HttpClient(MockEngine { respond(responseJson) })

    @Test
    fun testRpc20DriverCorrectlySerializesNonRpc20Request() = runTest {
        val rpcUrl = "https://api.invalid.solana.com"
        val rpcRequest = JsonRpcRequest("getSomething", buildJsonObject { put("param", "value") },"1234", "2.0")
        val serializedRequest = buildJsonObject {
            put("method", rpcRequest.method)
            put("params", rpcRequest.params!!)
            put("id", rpcRequest.id)
            put("jsonrpc", rpcRequest.jsonrpc)
        }.toString()

        val rpcDriver = Rpc20Driver(rpcUrl, object : HttpNetworkDriver {
            override suspend fun makeHttpRequest(request: HttpRequest): String {
                assertEquals(serializedRequest, request.body)
                return ""
            }
        })

        rpcDriver.makeRequest(rpcRequest)
    }

    @Test
    fun testRpc20DriverCorrectlySerializesCustomRequest() = runTest {
        @Serializable
        class MyRpcRequest : JsonRpc20Request("getSomething", id = "1234",
            params = buildJsonArray {
                add("a string")
                addJsonObject {
                    put("param", "value")
                }
            })

        val rpcUrl = "https://api.invalid.solana.com"
        val rpcRequest = MyRpcRequest()
        val serializedRequest = buildJsonObject {
            put("method", rpcRequest.method)
            put("params", rpcRequest.params!!)
            put("id", rpcRequest.id)
            put("jsonrpc", rpcRequest.jsonrpc)
        }.toString()

        val rpcDriver = Rpc20Driver(rpcUrl, object : HttpNetworkDriver {
            override suspend fun makeHttpRequest(request: HttpRequest): String {
                assertEquals(serializedRequest, request.body)
                return ""
            }
        })

        rpcDriver.makeRequest(rpcRequest)
    }
}
