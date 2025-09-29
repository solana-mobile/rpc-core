# RpcCore
Multiplatform JSON RPC Library using Kotlin Serialization

[![badge-latest-release]][url-latest-release]
[![badge-license]][url-license]
[![badge-kotlin]][url-kotlin]

## Usage

### Network Driver
RpcCore does not provide a multiplatform networking implementaion, you must provide your own by implementing the `HttpNetworkingDriver` interface: 
```kotlin
class MyNetworkDriver : HttpNetworkDriver {
    override suspend fun makeHttpRequest(request: HttpRequest): String {
        val url = request.url
        val httpMethod = request.method (should be "POST" for RPC 2.0 requests)
        val requestHeaders = request.properties
        val requestBody = reqest.body
        // networking implementation to submit the HTTP request to the url
        ...
        return response string
    }
}
```

### RPC 2.0 Requests
```kotlin
// setup RPC driver
val rpcUrl = "https://api.endpoint.com"
val rpcDriver = Rpc20Driver(rpcUrl, MyNetworkDriver())

// build RPC request
val requestId = UUID.generate()
val requestMehtod = "getTheThing" // the RPC method you want to call
val requestParameters = buildJsonObject { // this will depend on the method you are calling
    put("param1", "Hello")
    put("param2", 1234)
}
val rpcRequest = JsonRpc20Request(requestId, requestParamaters, requestId)

// send the request and get response
// using JsonElement.serializer() will return the JSON RPC response. you can use your own serializer to get back a specific object
val rpcResponse = rpcDriver.makeRequest(rpcRequest, JsonElement.serializer())
```

<!-- TAG_VERSION -->
[badge-latest-release]: https://img.shields.io/badge/dynamic/json.svg?url=https://api.github.com/repos/solana-mobile/rpc-core/releases/latest&query=tag_name&label=release&color=blue
[badge-license]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

<!-- TAG_DEPENDENCIES -->
[badge-kotlin]: https://img.shields.io/badge/kotlin-2.2.20-blue.svg?logo=kotlin

[url-latest-release]: https://github.com/solana-mobile/web3-core/releases/latest
[url-license]: https://www.apache.org/licenses/LICENSE-2.0.txt
[url-kotlin]: https://kotlinlang.org


