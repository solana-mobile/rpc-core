package com.funkatronics.util

import com.funkatronics.networking.HttpNetworkDriver
import com.funkatronics.networking.HttpRequest

class MockHttpDriver(val response: String) : HttpNetworkDriver {
    override suspend fun makeHttpRequest(request: HttpRequest): String = response
}