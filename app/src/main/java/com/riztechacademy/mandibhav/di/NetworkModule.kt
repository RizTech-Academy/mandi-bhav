package com.riztechacademy.mandibhav.di

import com.riztechacademy.mandibhav.BuildConfig
import com.riztechacademy.mandibhav.data.network.PricesApi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** How Hilt builds the HTTP client and Retrofit: once each, for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Where the API lives; set per build type in app/build.gradle.kts. */
    @Provides
    fun provideBaseUrl(): HttpUrl = BuildConfig.PRICES_API_BASE_URL.toHttpUrl()

    /**
     * Unknown keys are skipped, so the server can add a field to version 1
     * of its JSON without breaking every copy of the app already installed.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    /**
     * One client for the app, shared by Retrofit and the image loader, so they
     * share one connection pool and one set of threads. [interceptors] is
     * empty in release; debug builds add a logger (src/debug).
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(interceptors: Set<@JvmSuppressWildcards Interceptor>): OkHttpClient =
        pricesHttpClient(interceptors)

    /**
     * The client is passed lazily and built on the first request, not here.
     * Building it sets up TLS, which StrictMode caught taking 270 ms on the
     * main thread when the prices screen's ViewModel was created. The first
     * request runs off the main thread, so that is where the cost moves.
     */
    @Provides
    @Singleton
    fun provideRetrofit(baseUrl: HttpUrl, client: Lazy<OkHttpClient>, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .callFactory { request -> client.get().newCall(request) }
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun providePricesApi(retrofit: Retrofit): PricesApi = retrofit.create(PricesApi::class.java)
}

/** Lets a build type add interceptors; the set may be empty. */
@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptorsModule {
    @Multibinds
    abstract fun interceptors(): Set<Interceptor>
}

/**
 * The client's limits, chosen for a phone on weak mobile data. OkHttp's
 * connect and read timeouts are 10 s by default, and the read timeout is per
 * read: a server that sends a byte every 9 s never trips it. The call
 * timeout is the whole request, start to finish, and is off by default.
 * Tests pass shorter limits.
 */
fun pricesHttpClient(
    interceptors: Set<Interceptor> = emptySet(),
    connectTimeoutMillis: Long = 10_000,
    readTimeoutMillis: Long = 10_000,
    callTimeoutMillis: Long = 20_000,
): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
    .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
    .callTimeout(callTimeoutMillis, TimeUnit.MILLISECONDS)
    .apply { interceptors.forEach { addInterceptor(it) } }
    .build()
