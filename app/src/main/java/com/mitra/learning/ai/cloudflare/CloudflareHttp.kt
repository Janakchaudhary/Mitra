package com.mitra.learning.ai.cloudflare

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudflareHttp(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun postJson(url: String, apiToken: String, body: JsonObject): String = requestWithRetry {
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiToken")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private suspend fun requestWithRetry(buildRequest: () -> Request): String {
        var lastFailure: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    client.newCall(buildRequest()).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (response.isSuccessful) return@use body
                        val retryable = response.code == 429 || response.code in 500..599
                        val message = CloudflareResponseParser.errorMessage(body)
                            ?: "Cloudflare AI request failed (${response.code})"
                        if (retryable) throw RetryableHttpException(message)
                        throw IOException(message)
                    }
                }
            } catch (t: Throwable) {
                lastFailure = t
                val retryable = t is RetryableHttpException || t is IOException
                if (!retryable || attempt == MAX_ATTEMPTS - 1) throw t
                delay(700L * (attempt + 1))
            }
        }
        throw lastFailure ?: IOException("Cloudflare AI request failed")
    }

    private class RetryableHttpException(message: String) : IOException(message)

    companion object {
        private const val MAX_ATTEMPTS = 3
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
