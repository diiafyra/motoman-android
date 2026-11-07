# 0004. Network errors as values, retry only for idempotent requests

Status: accepted

## Context

The only network call in v1.0 is a public Overpass query for nearby fuel stations and garages; v1.1
adds an authenticated REST API. Retrofit suspend functions throw on every failure, and a
`catch (e: Exception)` around one also swallows `CancellationException`, which breaks structured
concurrency: a screen that is left keeps working. Public services such as Overpass also answer
"too many requests" (429) or "busy" (503) from time to time, and a request repeated blindly can do
harm when it is not idempotent.

## Decision

- Repositories return `AppResult<T>`: `Success` or `Failure(DataError)`. `DataError` has four cases,
  chosen by what the user can do: `NoConnection` (check the connection), `Timeout` (try again),
  `Server(code)` (try later) and `InvalidData` (nothing; a broken response). ViewModels turn them into
  messages through one shared mapper.
- A single `safeCall` wrapper maps exceptions to `DataError`. It rethrows `CancellationException`.
- A `RetryInterceptor` retries `GET` and `HEAD` only, at most twice, after a dropped connection, a
  timeout or a 429, 502, 503 or 504. It waits 1 s then 2 s, or the server's `Retry-After`, capped at
  5 s. Other methods are sent once, because repeating them could act twice on the server.
- The call timeout (75 s) bounds a request including its retries, so no screen waits longer. It is
  long on purpose: the shared Overpass server answered in 26 to 43 seconds for central Hanoi when
  measured, so the screen says it may take a while instead of failing early.
- Logging is `BASIC` in debug builds (request line and status) and off in release builds. Bodies are
  never logged.

## Alternatives

- Throwing exceptions with a shared catch helper. Rejected: the compiler cannot check that a call
  site handles errors, and the helper is easy to forget.
- OkHttp's built-in `retryOnConnectionFailure`. Kept for connection setup, but it cannot honour
  `Retry-After` or retry on status codes.
- A third-party result library. Rejected for one small sealed interface.

## Consequences

- Every network call site must handle `Failure`; `map` keeps that short.
- v1.1 reuses this layer. Authenticated calls add an interceptor for the token and a refresh step that
  runs once for many concurrent 401s (ADR 5 covers sync).
- A failed query is never cached; the nearby screen shows a retry button instead of stale data.
