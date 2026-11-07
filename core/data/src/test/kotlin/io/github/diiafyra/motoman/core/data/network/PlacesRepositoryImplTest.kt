package io.github.diiafyra.motoman.core.data.network

import io.github.diiafyra.motoman.core.data.di.createOkHttpClient
import io.github.diiafyra.motoman.core.data.di.createOverpassApi
import io.github.diiafyra.motoman.core.data.repository.PlacesRepositoryImpl
import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.PlaceKind
import io.github.diiafyra.motoman.core.domain.result.AppResult
import io.github.diiafyra.motoman.core.domain.result.DataError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class PlacesRepositoryImplTest {
    private val server = MockWebServer()
    private val delays = mutableListOf<Long>()
    private val center = GeoPoint(21.0285, 105.8542)

    @Before
    fun start() = server.start()

    @After
    fun stop() = server.close()

    private fun repository(
        client: OkHttpClient =
            createOkHttpClient(
                debug = false,
                retry =
                    RetryInterceptor(sleep = {
                        delays +=
                            it
                    }),
            ),
    ) = PlacesRepositoryImpl(createOverpassApi(server.url("/").toString(), client, Json { ignoreUnknownKeys = true }))

    private fun ok(body: String) =
        MockResponse
            .Builder()
            .code(200)
            .body(body)
            .build()

    private fun code(code: Int) =
        MockResponse
            .Builder()
            .code(code)
            .body("busy")
            .build()

    @Test
    fun parsesNodesAndWaysNearestFirstAndSkipsWhatIsNotAPlace() =
        runTest {
            server.enqueue(
                ok(
                    """
                    {"version":0.6,"elements":[
                      {"type":"node","id":1,"lat":21.0400,"lon":105.8542,"tags":{"amenity":"fuel","name":"Far","brand":"Petrolimex"}},
                      {"type":"way","id":2,"center":{"lat":21.0290,"lon":105.8542},"tags":{"shop":"motorcycle_repair","name":"Near","name:vi":"Sửa xe Gần"}},
                      {"type":"node","id":3,"lat":21.03,"lon":105.85,"tags":{"amenity":"cafe"}},
                      {"type":"relation","id":4,"tags":{"amenity":"fuel"}},
                      {"type":"node","id":5,"lat":21.0300,"lon":105.8542,"tags":{"shop":"car_repair","opening_hours":"Mo-Su 07:00-19:00"},"extra":true}
                    ]}
                    """.trimIndent(),
                ),
            )

            val result = repository().nearby(center, radiusMeters = 3_000)

            val places = (result as AppResult.Success).value
            assertEquals(listOf("way/2", "node/5", "node/1"), places.map { it.id })
            assertEquals("Sửa xe Gần", places[0].name)
            assertEquals(PlaceKind.MOTORBIKE_REPAIR, places[0].kind)
            assertEquals(PlaceKind.CAR_REPAIR, places[1].kind)
            assertEquals("Mo-Su 07:00-19:00", places[1].openingHours)
            assertEquals("Petrolimex", places[2].brand)
        }

    @Test
    fun sendsOneGetWithTheQueryAndAUserAgent() =
        runTest {
            server.enqueue(ok("""{"elements":[]}"""))

            repository().nearby(center, radiusMeters = 3_000)

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            val query = requireNotNull(request.url.queryParameter("data"))
            assertTrue(query, query.contains("around:3000,21.028500,105.854200"))
            assertTrue(query.contains("\"amenity\"=\"fuel\""))
            assertTrue(requireNotNull(request.headers["User-Agent"]).startsWith("SoXe-Android"))
        }

    @Test
    fun aBusyServerIsRetriedThenSucceeds() =
        runTest {
            server.enqueue(code(503))
            server.enqueue(code(429))
            server.enqueue(ok("""{"elements":[]}"""))

            val result = repository().nearby(center, 3_000)

            assertTrue(result is AppResult.Success)
            assertEquals(3, server.requestCount)
            assertEquals(listOf(1_000L, 2_000L), delays)
        }

    @Test
    fun aServerStillBusyAfterTwoRetriesIsAServerError() =
        runTest {
            repeat(3) { server.enqueue(code(503)) }

            assertEquals(AppResult.Failure(DataError.Server(503)), repository().nearby(center, 3_000))
            assertEquals(3, server.requestCount)
        }

    @Test
    fun retryAfterIsHonouredUpToTheCap() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(429)
                    .addHeader("Retry-After", "2")
                    .build(),
            )
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(429)
                    .addHeader("Retry-After", "120")
                    .build(),
            )
            server.enqueue(ok("""{"elements":[]}"""))

            repository().nearby(center, 3_000)

            assertEquals(listOf(2_000L, 5_000L), delays)
        }

    @Test
    fun aClientErrorIsNotRetried() =
        runTest {
            server.enqueue(code(400))

            assertEquals(AppResult.Failure(DataError.Server(400)), repository().nearby(center, 3_000))
            assertEquals(1, server.requestCount)
            assertTrue(delays.isEmpty())
        }

    @Test
    fun aBodyThatIsNotTheExpectedJsonIsInvalidData() =
        runTest {
            server.enqueue(ok("<html>Overpass is down</html>"))

            assertEquals(AppResult.Failure(DataError.InvalidData), repository().nearby(center, 3_000))
        }

    @Test
    fun aSlowServerIsATimeout() =
        runTest {
            repeat(3) {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(
                            200,
                        ).body("""{"elements":[]}""")
                        .headersDelay(2, TimeUnit.SECONDS)
                        .build(),
                )
            }
            val impatient =
                createOkHttpClient(debug = false, retry = RetryInterceptor(sleep = { delays += it }))
                    .newBuilder()
                    .readTimeout(100, TimeUnit.MILLISECONDS)
                    .build()

            assertEquals(AppResult.Failure(DataError.Timeout), repository(impatient).nearby(center, 3_000))
        }

    @Test
    fun noServerIsNoConnection() =
        runTest {
            val repository = repository()
            server.close()

            assertEquals(AppResult.Failure(DataError.NoConnection), repository.nearby(center, 3_000))
        }
}
