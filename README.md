# RpcCore
Multiplatform JSON RPC Library using Kotlin Serialization

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




