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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetTransactionTest {

    @Test
    fun `GetTransaction returns transaction details`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val expectedParsedInstruction = JsonParsedInstruction(
            program="spl-memo",
            programId = programId,
            stackHeight = 1u,
            parsed = JsonPrimitive("hello world")
        )

        // when
        rpc.requestAirdrop(pubkey, 0.1f)
        val blockhashResponse = rpc.getLatestBlockhash()
        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(TransactionInstruction(
                SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"),
                listOf(AccountMeta(pubkey, true, true)),
                "hello world".encodeToByteArray()
            ))
            .build().run {
                val sig = Ed25519.sign(keyPair, serialize())
                Transaction(listOf(sig), this)
            }

        val txResponse = withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(transaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            ))
        }
        val response = rpc.getTransaction(txResponse.result!!, Commitment.CONFIRMED)

        println(response.result)

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertNotNull(response.result!!.meta)
        assertNull(response.result!!.meta.err)
        assertEquals(expectedParsedInstruction, response.result!!.transaction.message.instructions.first())
    }

    @Test
    fun `GetTransaction returns transaction error`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val expectedParsedInstruction = JsonParsedInstruction(
            program="spl-memo",
            programId = programId,
            stackHeight = 1u,
            parsed = JsonPrimitive("hello world")
        )

        // when
        rpc.requestAirdrop(pubkey, 0.1f)
        val blockhashResponse = rpc.getLatestBlockhash()
        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(TransactionInstruction(
                SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"),
                listOf(
                    AccountMeta(pubkey, true, true),
                    AccountMeta(programId, false, false)
                ),
                "hello world".encodeToByteArray()
            ))
            .build().run {
                val sig = Ed25519.sign(keyPair, serialize())
                Transaction(listOf(sig), this)
            }

        val txResponse = withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(transaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            ))
        }
        val response = rpc.getTransaction(txResponse.result!!, Commitment.CONFIRMED)

        println(response.result)

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertNotNull(response.result!!.meta)
        assertNotNull(response.result!!.meta.err)
        assertEquals(expectedParsedInstruction, response.result!!.transaction.message.instructions.first())
    }
}