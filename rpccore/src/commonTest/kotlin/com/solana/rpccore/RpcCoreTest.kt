package com.solana.rpccore

import com.solana.networking.HttpNetworkDriver
import com.solana.networking.HttpRequest
import com.solana.networking.Rpc20Driver
import com.solana.util.MockHttpDriver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.test.*

class RpcDriverTest {

    @Test
    fun testJsonRpcDriverGetReturnsResultFailure() = runTest {
        // given
        val rpcUrl = "https://api.invalid.solana.com"
        val rpcRequest = JsonRpc20Request("getLatestBlockhash", id = "1234")
        val rpcDriver = Rpc20Driver(rpcUrl, MockHttpDriver(""))

        // when
        val rpcResponse = rpcDriver.get(rpcRequest, JsonElement.serializer())

        // then
        assertTrue(rpcResponse.isFailure)
        assertNotNull(rpcResponse.exceptionOrNull())
    }

    @Test
    fun testJsonRpcDriverGetReturnsRpcError() = runTest {
        // given
        val rpcUrl = "https://api.invalid.solana.com"
        val rpcRequest = JsonRpc20Request("getInvalidMethod", id = "1234")
        val rpcDriver = Rpc20Driver(rpcUrl, MockHttpDriver(""))

        // when
        val rpcResponse = rpcDriver.makeRequest(rpcRequest, JsonElement.serializer())

        // then
        assertNotNull(rpcResponse.error)
        assertNull(rpcResponse.result)
    }

    @Test
    fun testRpc20DriverReturnsResult() = runTest {
        // given
        val rpcUrl = "https://api.mocked.solana.com"
        val rpcRequest = JsonRpc20Request("getLatestBlockhash", id = "1234")
        val mockedResponse = """
            {
                "jsonrpc":"2.0",
                "result":{
                    "context":{
                        "slot":197247830
                    },
                    "value":{
                        "blockhash": "EkSnNWid2cvwEVnVx9aBqawnmiCNiDgp3gUdkDPTKN1N",
                        "lastValidBlockHeight": 3090
                    }
                },
                "id":"${rpcRequest.id}"
            }
        """.trimIndent()

        val rpcDriver = Rpc20Driver(rpcUrl, MockHttpDriver(mockedResponse))

        // when
        val rpcResponse = rpcDriver.makeRequest(rpcRequest, JsonElement.serializer())

        // then
        assertNull(rpcResponse.error)
        assertNotNull(rpcResponse.result)
    }

    @Test
    fun testRpc20DriverReturnsNetworkError() = runTest {
        // given
        val rpcUrl = "https://api.invalid.solana.com"
        val rpcRequest = JsonRpc20Request("getInvalidMethod", id = "1234")
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

        val rpcDriver = Rpc20Driver(rpcUrl, MockHttpDriver(mockedResponse))

        // when
        val rpcResponse = rpcDriver.makeRequest(rpcRequest, JsonElement.serializer())

        // then
        assertNotNull(rpcResponse.error)
        assertNull(rpcResponse.result)
    }

    @Test
    fun testRpc20DriverCorrectlySerializesRequest() = runTest {
        val rpcUrl = "https://api.invalid.solana.com"
        val rpcRequest = JsonRpc20Request("getSomething", id = "1234")
        val serializedRequest = buildJsonObject {
            put("method", rpcRequest.method)
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