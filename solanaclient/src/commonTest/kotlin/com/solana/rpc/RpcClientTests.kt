package com.solana.rpc

import com.solana.config.TestConfig
import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import diglol.crypto.Ed25519
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

class RpcClientTests {

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
                encoding = Encoding.base58,
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
                encoding = Encoding.base64,
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