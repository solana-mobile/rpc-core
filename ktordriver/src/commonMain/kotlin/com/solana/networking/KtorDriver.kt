package com.solana.networking

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class KtorNetworkDriver(val httpClient: HttpClient = HttpClient()): HttpNetworkDriver {
    override suspend fun makeHttpRequest(request: HttpRequest): String =
        httpClient.request(request.url) {
            method = HttpMethod.parse(request.method)
            request.properties.forEach { (k, v) ->
                header(k, v)
            }
            setBody(request.body)
        }.bodyAsText()
}