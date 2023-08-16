package com.funkatronics.networking

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpNetworkDriver(val okHttpClient: OkHttpClient): HttpNetworkDriver {

    override suspend fun makeHttpRequest(request: HttpRequest): String {
        val okHttpRequest = Request.Builder().apply {
            url(request.url)
            request.properties.forEach { (key, value) ->
                addHeader(key, value)
            }
            when (request.method) {
                "POST" -> post((request.body ?: "").toRequestBody())
                "GET" -> get()
                else -> throw Error("Request Invalid: Unsupported http method: { ${request.method} }. This driver only supports GET and POST requests.")
            }
        }.build()

        val response = okHttpClient.newCall(okHttpRequest).execute()

        // Response.body returns a non-null value if this response was returned from Call.execute
        return response.body.string().also {
            response.close()
        }
    }
}