package io.github.diiafyra.motoman.core.data.network

import io.github.diiafyra.motoman.core.domain.result.AppResult
import io.github.diiafyra.motoman.core.domain.result.DataError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Runs a network call and turns every failure into a [DataError], so callers handle errors as values.
 * Cancellation is never turned into an error: it is rethrown, so structured concurrency keeps working
 * when the user leaves the screen.
 */
internal suspend fun <T> safeCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        AppResult.Failure(DataError.Server(e.code()))
    } catch (e: SerializationException) {
        AppResult.Failure(DataError.InvalidData)
    } catch (e: InterruptedIOException) {
        // SocketTimeoutException, and OkHttp's call timeout.
        AppResult.Failure(DataError.Timeout)
    } catch (e: IOException) {
        AppResult.Failure(DataError.NoConnection)
    }
