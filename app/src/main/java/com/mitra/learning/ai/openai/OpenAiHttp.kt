package com.mitra.learning.ai.openai

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

class OpenAiHttp(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun postJson(url: String, apiKey: String, body: JsonObject): String = requestWithRetry {
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    suspend fun get(url: String, apiKey: String): String = requestWithRetry {
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .get()
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
                        val message = runCatching {
                            OpenAiResponseParser.errorMessage(OpenAiResponseParser.parseRoot(body))
                        }.getOrNull() ?: "AI request failed (${response.code})"
                        if (retryable) throw RetryableHttpException(message)
                        throw IOException(message)
                    }
                }
            } catch (t: Throwable) {
                lastFailure = t
                val retryable = t is RetryableHttpException || t is IOException
                if (!retryable || attempt == MAX_ATTEMPTS - 1) throw t
                delay(500L * (attempt + 1))
            }
        }
        throw lastFailure ?: IOException("AI request failed")
    }

    private class RetryableHttpException(message: String) : IOException(message)

    companion object {
        private const val MAX_ATTEMPTS = 3
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
