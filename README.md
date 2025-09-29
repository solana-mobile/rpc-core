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

### Example: Solana Account Request

Using the `rpc-solana` and `rpc-ktordriver` modules along with [web3-solana](https://github.com/solana-mobile/web3-core), we can query the blockchain and deserialze encoded account data all in one step. 

```kotlin
val accountPublicKey = SolanaPublicKey(/* public key of the account we wish to query */)
val rpcUrl = "https://api.mainnet-beta.solana.com"
val rpc = SolanaRpcClient(rpcUrl, KtorNetworkDriver()) // using prebuilt ktor driver from `rpc-ktordriver`

// Get basic AccountInfo with serialized account data as a ByteArray
val accountInfoResponse = rpc.getAccountInfo(accountPublicKey, commitment = Commitment.CONFIRMED)

accountInfoResponse.result?.let { accountInfo ->
    // check account info: owner, space, lamports etc.
    val owner = accountInfo.owner
}
```

By supplying out own deserialization strategy to the RPC client, we can receive an fully deserialized struct based on our on chain data. In this example, we will read anm SPL token mint account using a custom serializer from `kotlinx-serialization`.

```kotlin
// declare account data struct representing the data we want to deserialize from the blockchain
@Serializable
data class MintAccountInfo(
    val mintAuthorityOption: Int,
    val mintAuthority: SolanaPublicKey,
    val supply: Long,
    val decimals: Byte,
    val isInitialized: Boolean,
    val freezeAuthorityOption: Int,
    val freezeAuthority: SolanaPublicKey
)

// now we query the RPC as we did before
val mintAccountPublicKey = SolanaPublicKey(/* public key of the mint we wish to query */)
val rpcUrl = "https://api.mainnet-beta.solana.com"
val rpc = SolanaRpcClient(rpcUrl, KtorNetworkDriver()) // using prebuilt ktor driver from `rpc-ktordriver`

// Get AccountInfo with deserialized account data as MintAccountInfo
val accountInfoResponse = rpc.getAccountInfo(MintAccountInfo.serializer(), 
    mintPublicKey, 
    commitment = Commitment.CONFIRMED
)

accountInfoResponse.result?.data?.let { mintAccountInfo ->
    // check mint account details
    val mintAuthority = mintAccountInfo.authority
    val mintSupply = mintAccountInfo.supply
    val mintDecimals = mintAccountInfo.decimals
}
```

This approach works for any on chain data that is stored on the chain with standard Borsh encoding as long as the provided serialization strategy matches the account data layout. 

<!-- TAG_VERSION -->
[badge-latest-release]: https://img.shields.io/badge/dynamic/json.svg?url=https://api.github.com/repos/solana-mobile/rpc-core/releases/latest&query=tag_name&label=release&color=blue
[badge-license]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

<!-- TAG_DEPENDENCIES -->
[badge-kotlin]: https://img.shields.io/badge/kotlin-2.2.20-blue.svg?logo=kotlin

[url-latest-release]: https://github.com/solana-mobile/rpc-core/releases/latest
[url-license]: https://www.apache.org/licenses/LICENSE-2.0.txt
[url-kotlin]: https://kotlinlang.org


