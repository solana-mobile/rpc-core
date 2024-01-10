package com.solana.util

import com.solana.networking.HttpNetworkDriver
import com.solana.networking.HttpRequest

class MockHttpDriver(val response: String) : HttpNetworkDriver {
    override suspend fun makeHttpRequest(request: HttpRequest): String = response
}