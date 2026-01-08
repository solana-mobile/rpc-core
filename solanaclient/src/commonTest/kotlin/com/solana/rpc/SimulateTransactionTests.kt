package com.solana.rpc

import com.solana.config.TestConfig
import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import com.solana.transaction.toUnsignedTransaction
import diglol.crypto.Ed25519
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SimulateTransactionTests {

    @Test
    fun `simulateTransaction with basic options returns simulation result`() = runTest {
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
            .build().toUnsignedTransaction()

        // delay to allow commitment
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            delay(2000)
        }

        val response = rpc.simulateTransaction(transaction,
            commitment = Commitment.CONFIRMED,
            encoding = Encoding.BASE64,
            replaceRecentBlockhash = false,
            sigVerify = false,
            minContextSlot = null,
            innerInstructions = null,
            accounts = null,
            attemptJsonParseAccounts = false
        )

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
        assertNotNull(response.result!!.logs)
        assertNull(response.result!!.err)
    }

    @Test
    fun `simulateTransaction with replacement blockhash returns replaced blockhash`() = runTest {
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
            .build().toUnsignedTransaction()

        // delay to allow commitment
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            delay(2000)
        }

        val response = rpc.simulateTransaction(transaction,
            commitment = Commitment.CONFIRMED,
            encoding = Encoding.BASE64,
            replaceRecentBlockhash = true,
            sigVerify = false,
            minContextSlot = null,
            innerInstructions = null,
            accounts = null,
            attemptJsonParseAccounts = false
        )

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
        assertNotNull(response.result!!.logs)
        assertNull(response.result!!.err)
        assertNotNull(response.result!!.replacementBlockhash)
    }

    @Test
    fun `simulateTransaction with sigverify verifies transaction signature`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val message = "hello solana!"

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.1f)

        // delay to allow commitment
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            delay(2000)
        }

        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(buildMemoTransaction(pubkey, message))
            .build().run {
                val sig = Ed25519.sign(keyPair, serialize())
                Transaction(listOf(sig), this)
            }

        val response = rpc.simulateTransaction(transaction,
            commitment = Commitment.PROCESSED,
            encoding = Encoding.BASE64,
            replaceRecentBlockhash = false,
            sigVerify = true,
            minContextSlot = null,
            innerInstructions = null,
            accounts = null,
            attemptJsonParseAccounts = false
        )

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
        assertNotNull(response.result!!.logs)
        assertNull(response.result!!.err)
    }

    @Test
    fun `simulateTransaction with accounts returns transaction accounts`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val message = "hello solana!"

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.1f)

        // delay to allow commitment
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            delay(2000)
        }

        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(buildMemoTransaction(pubkey, message))
            .build().toUnsignedTransaction()

        val response = rpc.simulateTransaction(transaction,
            commitment = Commitment.PROCESSED,
            encoding = Encoding.BASE64,
            replaceRecentBlockhash = false,
            sigVerify = false,
            minContextSlot = null,
            innerInstructions = null,
            accounts = listOf(pubkey),
            attemptJsonParseAccounts = false
        )

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
        assertNotNull(response.result!!.logs)
        assertNull(response.result!!.err)
    }

    private fun buildMemoTransaction(address: SolanaPublicKey, memo: String) =
        TransactionInstruction(SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"),
            listOf(AccountMeta(address, true, true)),
            memo.encodeToByteArray()
        )
}