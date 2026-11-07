package io.github.diiafyra.motoman.core.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Retries a request that failed for a reason that may pass: a dropped connection, a timeout, or the
 * server saying it is busy (429, 502, 503, 504). Only GET and HEAD are retried, because they are
 * idempotent: repeating them cannot do anything twice on the server. Anything else is sent once.
 *
 * Waits [baseDelayMillis], then twice that, between attempts, or what the server's Retry-After asks
 * for, capped at [maxDelayMillis]. OkHttp's call timeout still bounds the whole thing.
 */
internal class RetryInterceptor(
    private val maxRetries: Int = 2,
    private val baseDelayMillis: Long = 1_000,
    private val maxDelayMillis: Long = 5_000,
    private val sleep: (Long) -> Unit = Thread::sleep,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET" && request.method != "HEAD") return chain.proceed(request)

        var attempt = 0
        while (true) {
            val response =
                try {
                    chain.proceed(request)
                } catch (e: IOException) {
                    if (attempt >= maxRetries || chain.call().isCanceled()) throw e
                    sleep(delayFor(attempt, retryAfterSeconds = null))
                    attempt++
                    continue
                }
            if (response.code !in RETRYABLE_CODES || attempt >= maxRetries) return response
            val retryAfter = response.header("Retry-After")?.trim()?.toLongOrNull()
            response.close()
            sleep(delayFor(attempt, retryAfter))
            attempt++
        }
    }

    private fun delayFor(
        attempt: Int,
        retryAfterSeconds: Long?,
    ): Long {
        val wanted = retryAfterSeconds?.times(1_000) ?: (baseDelayMillis shl attempt)
        return wanted.coerceIn(0, maxDelayMillis)
    }

    private companion object {
        val RETRYABLE_CODES = setOf(429, 502, 503, 504)
    }
}
