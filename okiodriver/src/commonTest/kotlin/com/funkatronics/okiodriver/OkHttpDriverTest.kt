package com.funkatronics.okiodriver

import com.funkatronics.networking.HttpRequest
import com.funkatronics.networking.OkHttpNetworkDriver
import com.funkatronics.networking.Rpc20Driver
import com.funkatronics.rpccore.JsonRpc20Request
import com.funkatronics.rpccore.makeRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OkHttpDriverTest {

    @Test
    fun testOkHttpNetworkDriverPostRequest() = runTest {
        // given
        val okHttpClient = buildMockJsonHttpClient("{}")
        val driver = OkHttpNetworkDriver(okHttpClient)
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
    fun testOkHttpNetworkDriverGetRequest() = runTest {
        // given
        val okHttpClient = buildMockJsonHttpClient("{}")
        val driver = OkHttpNetworkDriver(okHttpClient)
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
    fun testOkHttpNetworkDriverUnsupportedMethod() = runTest {
        // given
        val okHttpClient = OkHttpClient.Builder().build()
        val driver = OkHttpNetworkDriver(okHttpClient)
        val request = object : HttpRequest {
            override val url = "https:///someendpoint.com/delete"
            override val method = "DELETE"
            override val body = "Hello World!"
            override val properties: Map<String, String> = mapOf()
        }

        // when
        val result = runCatching {
            driver.makeHttpRequest(request)
        }

        // then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun testOkHttpNetworkDriverWithRpcDriver() = runTest {
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

        val okHttpClient = buildMockJsonHttpClient(mockedResponse)
        val httpDriver = OkHttpNetworkDriver(okHttpClient)
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
    fun testOkHttpNetworkDriverWithRpcDriverReturnsRpcError() = runTest {
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

        val okHttpClient = buildMockJsonHttpClient(mockedResponse)
        val httpDriver = OkHttpNetworkDriver(okHttpClient)
        val rpcDriver = Rpc20Driver(rpcUrl, httpDriver)

        // when
        val rpcResponse = rpcDriver.makeRequest(rpcRequest)

        // then
        assertNotNull(rpcResponse.error)
        assertNull(rpcResponse.result)
    }

    private fun buildMockJsonHttpClient(responseJson: String) = OkHttpClient.Builder()
        .addInterceptor {
            Response.Builder()
                .request(it.request())
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("")
                .body(responseJson.toResponseBody("application/json; charset=utf-8".toMediaType()))
                .build()
        }.build()
}