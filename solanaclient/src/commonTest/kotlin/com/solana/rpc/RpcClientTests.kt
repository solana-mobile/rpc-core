package com.solana.rpc

import com.funkatronics.encoders.Base64
import com.funkatronics.kborsh.Borsh
import com.solana.config.TestConfig
import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.serializers.ByteArrayAsEncodedDataArrayDeserializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import diglol.crypto.Ed25519
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlin.test.*

class RpcClientTests {

    @Test
    fun `getAccountInfo returns AccountInfo object`() = runTest {
        // given
        val expectedAccountData = "system_program"
        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val response = rpcClient.getAccountInfo(
            ByteArrayAsEncodedDataArrayDeserializer,
            SolanaPublicKey.from("11111111111111111111111111111111")
        )

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(expectedAccountData, response.result!!.data!!.decodeToString())
    }

    @Test
    fun `getAccountInfo with data slicing returns AccountInfo object with sliced data`() = runTest {
        // given
        val dataSlice = AccountRequest.DataSlice(8, 2)
        val expectedAccountData = "stem_pro"
        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val response = rpcClient.getAccountInfo(
            ByteArrayAsEncodedDataArrayDeserializer,
            SolanaPublicKey.from("11111111111111111111111111111111"),
            dataSlice = dataSlice
        )

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(expectedAccountData, response.result!!.data!!.decodeToString())
    }

    @Test
    fun `getAccountInfo deserializes account data struct`() = runTest {
        // given
        @Serializable
        data class TestAccountData(val name: String, val number: Int, val bool: Boolean)
        val testData = TestAccountData("accountInfoTest", 123456789, false)
        val testDataBorsh = Borsh.encodeToByteArray(testData)
        val ownerPubkey = SolanaPublicKey.from("11111111111111111111111111111111")
        val mockedResponse = """
            {
                "jsonrpc":"2.0",
                "result":{
                    "context":{"apiVersion":"apiVer","slot":123456789},
                    "value":{
                        "data":[
                            "${Base64.encodeToString(testDataBorsh)}",
                            "base64"
                        ]
                        "executable":true,
                        "lamports":1,
                        "owner":"11111111111111111111111111111111",
                        "rentEpoch":1234567890,
                        "space":${testDataBorsh.size}
                    }
                },
                "id":"requestId"
            }
        """.trimIndent()

        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver(
            HttpClient(MockEngine {
                respond(mockedResponse)
            })
        ))

        // when
        val response = rpcClient.getAccountInfo<TestAccountData>(ownerPubkey)

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(response.result!!.owner, ownerPubkey)
        assertEquals(testData, response.result!!.data)
    }

    @Test
    fun `getMultipleAccounts returns AccountInfo object`() = runTest {
        // given
        val expectedAccountData = "system_program"
        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val response = rpcClient.getMultipleAccountsRaw(
            listOf(
                SolanaPublicKey.from("11111111111111111111111111111111"),
                SolanaPublicKey.from("11111111111111111111111111111111"),
            ),
        )

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(2, response.result!!.size)
        assertEquals(expectedAccountData, response.result!!.first()?.data!!.decodeToString())
        assertEquals(expectedAccountData, response.result!![1]?.data!!.decodeToString())
    }

    @Test
    fun `getMultipleAccounts returns null in list`() = runTest {
        // given
        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val response = rpcClient.getMultipleAccountsRaw(
            listOf(
                SolanaPublicKey.from("NativeLoader1111111111111111111111111111111"),
            ),
        )

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertNull(response.result!!.first())
    }

    @Test
    fun `getMultipleAccounts with data slicing returns AccountInfo object with sliced data`() = runTest {
        // given
        val dataSlice = AccountRequest.DataSlice(8, 2)
        val expectedAccountData = "stem_pro"
        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val response = rpcClient.getMultipleAccountsRaw(
            listOf(
                SolanaPublicKey.from("11111111111111111111111111111111"),
                SolanaPublicKey.from("11111111111111111111111111111111"),
            ),
            dataSlice = dataSlice
        )

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(2, response.result!!.size)
        assertEquals(expectedAccountData, response.result!!.first()?.data!!.decodeToString())
        assertEquals(expectedAccountData, response.result!![1]?.data!!.decodeToString())
    }

    @Test
    fun `getMultipleAccounts deserializes account data struct`() = runTest {
        // given
        @Serializable
        data class TestAccountData(val name: String, val number: Int, val bool: Boolean)
        val testData = TestAccountData("accountInfoTest", 123456789, false)
        val testDataBorsh = Borsh.encodeToByteArray(testData)
        val accountPubkey = SolanaPublicKey.from("11111111111111111111111111111111")
        val mockedResponse = """
            {
                "jsonrpc":"2.0",
                "result":{
                    "context":{"apiVersion":"apiVer","slot":123456789},
                    "value":[{
                        "data":[
                            "${Base64.encodeToString(testDataBorsh)}",
                            "base64"
                        ]
                        "executable":true,
                        "lamports":1,
                        "owner":"11111111111111111111111111111111",
                        "rentEpoch":1234567890,
                        "space":${testDataBorsh.size}
                    }]
                },
                "id":"requestId"
            }
        """.trimIndent()

        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver(
            HttpClient(MockEngine {
                respond(mockedResponse)
            })
        ))

        // when
        val response = rpcClient.getMultipleAccounts<TestAccountData>(listOf(accountPubkey))

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(response.result!!.first()!!.owner, accountPubkey)
        assertEquals(testData, response.result!!.first()!!.data)
    }

    @Test
    fun `getProgramAccounts returns list of AccountInfoWithPublicKey objects`() = runTest {
        // given
        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val response = rpcClient.getProgramAccountsRaw(
            SolanaPublicKey.from("NativeLoader1111111111111111111111111111111")
        )

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertTrue { response.result!!.size > 1 }
    }

    @Test
    fun `getProgramAccounts returns empty list`() = runTest {
        // given
        val randomPublicKey = Ed25519.generateKeyPair().publicKey
        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val response = rpcClient.getProgramAccountsRaw(
            SolanaPublicKey(randomPublicKey),
        )

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(0, response.result!!.size)
    }

    @Test
    fun `getProgramAccounts deserializes account data struct`() = runTest {
        // given
        @Serializable
        data class TestAccountData(val name: String, val number: Int, val bool: Boolean)
        val testData = TestAccountData("accountInfoTest", 123456789, false)
        val testDataBorsh = Borsh.encodeToByteArray(testData)
        val programId = SolanaPublicKey.from("11111111111111111111111111111111")
        val mockedResponse = """
            {
                "jsonrpc":"2.0",
                "result":{
                    "context":{"apiVersion":"apiVer","slot":123456789},
                    "value":[{
                        "account": {
                            "data":[
                                "${Base64.encodeToString(testDataBorsh)}",
                                "base64"
                            ]
                            "executable":true,
                            "lamports":1,
                            "owner":"11111111111111111111111111111111",
                            "rentEpoch":1234567890,
                            "space":${testDataBorsh.size}
                        },
                        "pubkey": "11111111111111111111111111111111"
                    }]
                }
                "id":"requestId"
            }
        """.trimIndent()

        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver(
            HttpClient(MockEngine {
                respond(mockedResponse)
            })
        ))

        // when
        val response = rpcClient.getProgramAccounts<TestAccountData>(programId)

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(response.result!!.first()!!.account.owner, programId)
        assertEquals(testData, response.result!!.first()!!.account.data)
    }

    @Test
    fun `getLatestBlockhash returns valid blockhash response`() = runTest {
        // given
        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val response = rpcClient.getLatestBlockhash()

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertTrue { response.result!!.blockhash.isNotEmpty() }
    }

    @Test
    fun `getMinBalanceForRentExemption returns rent exempt balance`() = runTest {
        // given
        val rpcClient = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val size = 0L

        // when
        val response = rpcClient.getMinBalanceForRentExemption(size)

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertTrue { response.result!! > 0 }
    }

    @Test
    fun `getSignatureStatus returns valid blockhash response`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.1f)
        val response = rpc.getSignatureStatuses(listOf(airdropResponse.result!!))

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertTrue { response.result!!.size == 1 }
    }

    @Test
    fun `getBalance returns account balance`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver(),
            TransactionOptions(commitment = Commitment.CONFIRMED))
        val balance = 10000000L

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.01f)

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.confirmTransaction(airdropResponse.result!!,
                TransactionOptions(commitment = Commitment.CONFIRMED))
        }

        val response = rpc.getBalance(pubkey)

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(balance, response.result)
    }

    @Test
    fun `sendTransaction with base58 encoding returns transaction signature`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val message = "hello solana!"

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.1f)
        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(buildMemoTransaction(pubkey, message))
            .build().run {
                val sig = Ed25519.sign(keyPair, serialize())
                Transaction(listOf(sig), this)
            }

        val response = rpc.sendTransaction(transaction,
            TransactionOptions(
                commitment = Commitment.CONFIRMED,
                encoding = Encoding.BASE58,
                skipPreflight = true
            )
        )

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
        assertTrue { response.result!!.isNotEmpty() }
    }

    @Test
    fun `sendTransaction with base64 encoding returns transaction signature`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val message = "hello solana!"

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.1f)
        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(buildMemoTransaction(pubkey, message))
            .build().run {
                val sig = Ed25519.sign(keyPair, serialize())
                Transaction(listOf(sig), this)
            }

        val response = rpc.sendTransaction(transaction,
            TransactionOptions(
                commitment = Commitment.CONFIRMED,
                encoding = Encoding.BASE64,
                skipPreflight = true
            )
        )

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
        assertTrue { response.result!!.isNotEmpty() }
    }

    private fun buildMemoTransaction(address: SolanaPublicKey, memo: String) =
        TransactionInstruction(SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"),
            listOf(AccountMeta(address, true, true)),
            memo.encodeToByteArray()
        )
}