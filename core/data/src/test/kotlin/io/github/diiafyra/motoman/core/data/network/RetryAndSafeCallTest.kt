package io.github.diiafyra.motoman.core.data.network

import io.github.diiafyra.motoman.core.domain.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RetryAndSafeCallTest {
    private val server = MockWebServer()
    private val client = OkHttpClient.Builder().addInterceptor(RetryInterceptor(sleep = {})).build()

    @Before
    fun start() = server.start()

    @After
    fun stop() = server.close()

    @Test
    fun aPostIsNeverRetried() {
        // Repeating a POST could create the same thing twice on the server.
        server.enqueue(MockResponse.Builder().code(503).build())
        server.enqueue(MockResponse.Builder().code(200).build())

        val response =
            client
                .newCall(
                    Request
                        .Builder()
                        .url(
                            server.url("/"),
                        ).post("{}".toRequestBody("application/json".toMediaType()))
                        .build(),
                ).execute()

        assertEquals(503, response.code)
        assertEquals(1, server.requestCount)
        response.close()
    }

    @Test
    fun aGetIsRetriedAfterTheConnectionDropsButOnlyTwice() {
        var attempts = 0
        val delays = mutableListOf<Long>()
        // Sits inside the retry interceptor and fails like a dropped connection.
        val flaky =
            OkHttpClient
                .Builder()
                .addInterceptor(RetryInterceptor(sleep = { delays += it }))
                .addInterceptor { _ ->
                    attempts++
                    throw java.io.IOException("connection reset")
                }.build()

        assertThrows(java.io.IOException::class.java) {
            flaky.newCall(Request.Builder().url(server.url("/")).build()).execute()
        }

        assertEquals(3, attempts)
        assertEquals(listOf(1_000L, 2_000L), delays)
    }

    @Test
    fun aPostIsNotRetriedAfterAConnectionDropEither() {
        var attempts = 0
        val flaky =
            OkHttpClient
                .Builder()
                .addInterceptor(RetryInterceptor(sleep = {}))
                .addInterceptor { _ ->
                    attempts++
                    throw java.io.IOException("connection reset")
                }.build()
        val post =
            Request
                .Builder()
                .url(
                    server.url("/"),
                ).post("{}".toRequestBody("application/json".toMediaType()))
                .build()

        assertThrows(java.io.IOException::class.java) { flaky.newCall(post).execute() }

        assertEquals(1, attempts)
    }

    @Test
    fun cancellationIsRethrownNotTurnedIntoAnError() =
        runTest {
            assertThrows(CancellationException::class.java) {
                kotlinx.coroutines.runBlocking { safeCall<Unit> { throw CancellationException("left the screen") } }
            }
            assertEquals(AppResult.Success(1), safeCall { 1 })
        }
}
