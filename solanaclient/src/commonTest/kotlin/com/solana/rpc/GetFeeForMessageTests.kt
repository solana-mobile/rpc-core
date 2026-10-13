package com.solana.rpc

import com.solana.config.TestConfig
import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import diglol.crypto.Ed25519
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetFeeForMessageTests {

    @Test
    fun `getFeeForMessage with basic options returns non-zero value`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        val blockhashResponse = rpc.getLatestBlockhash()

        val message = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(buildMemoTransaction(pubkey, "hello solana!"))
            .build()

        val response = rpc.getFeeForMessage(
            message,
            commitment = Commitment.CONFIRMED,
            minContextSlot = null,
        )

        // then
        assertNull(response.error)
        assertNotNull(response.result)
        assertTrue { response.result!! > 0u }
    }
}