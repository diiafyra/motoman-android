package io.github.diiafyra.motoman.core.domain.result

/**
 * The outcome of anything that can fail for reasons outside the app (network, server). Errors are a
 * value the caller must handle, not an exception it might forget to catch.
 */
public sealed interface AppResult<out T> {
    public data class Success<out T>(
        val value: T,
    ) : AppResult<T>

    public data class Failure(
        val error: DataError,
    ) : AppResult<Nothing>
}

/**
 * Failures grouped by what the user can do about them, which is what the screen needs to choose a
 * message: check the connection, try again later, or nothing (a bug or a broken server response).
 */
public sealed interface DataError {
    /** No route to the server: offline, DNS failure, connection refused. */
    public data object NoConnection : DataError

    /** The server did not answer in time. */
    public data object Timeout : DataError

    /** The server answered with an error, including "too many requests". */
    public data class Server(
        val code: Int,
    ) : DataError

    /** The server answered, but not with what the app expects. */
    public data object InvalidData : DataError
}

public inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(value))
        is AppResult.Failure -> this
    }
