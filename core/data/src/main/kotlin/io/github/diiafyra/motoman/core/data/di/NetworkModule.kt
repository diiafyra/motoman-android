package io.github.diiafyra.motoman.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.diiafyra.motoman.core.data.BuildConfig
import io.github.diiafyra.motoman.core.data.network.OverpassApi
import io.github.diiafyra.motoman.core.data.network.RetryInterceptor
import io.github.diiafyra.motoman.core.data.repository.PlacesRepositoryImpl
import io.github.diiafyra.motoman.core.domain.repository.PlacesRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {
    @Binds
    abstract fun placesRepository(impl: PlacesRepositoryImpl): PlacesRepository

    companion object {
        @Provides
        @Singleton
        fun json(): Json = Json { ignoreUnknownKeys = true }

        @Provides
        @Singleton
        fun okHttpClient(): OkHttpClient = createOkHttpClient(debug = BuildConfig.DEBUG)

        @Provides
        @Singleton
        fun overpassApi(
            client: OkHttpClient,
            json: Json,
        ): OverpassApi = createOverpassApi(OverpassApi.BASE_URL, client, json)
    }
}

internal fun createOverpassApi(
    baseUrl: String,
    client: OkHttpClient,
    json: Json,
): OverpassApi =
    Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OverpassApi::class.java)

/**
 * One client for the app. Debug builds log the request line and status only; release builds log
 * nothing. Bodies are never logged.
 */
internal fun createOkHttpClient(
    debug: Boolean,
    retry: RetryInterceptor = RetryInterceptor(),
): OkHttpClient =
    OkHttpClient
        .Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Bounds everything, retries included, so a screen never waits more than this.
        .callTimeout(45, TimeUnit.SECONDS)
        // Overpass asks clients to identify themselves.
        .addInterceptor { chain ->
            chain.proceed(
                chain
                    .request()
                    .newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build(),
            )
        }.addInterceptor(retry)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (debug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            },
        ).build()

private const val USER_AGENT = "SoXe-Android (+https://github.com/diiafyra/motoman-android)"
