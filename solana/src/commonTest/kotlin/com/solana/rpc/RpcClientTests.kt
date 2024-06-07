package com.solana.rpc

import com.solana.config.TestConfig
import com.solana.networking.KtorNetworkDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
}