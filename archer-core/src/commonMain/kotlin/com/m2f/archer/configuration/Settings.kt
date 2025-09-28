package com.m2f.archer.configuration

import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid
import com.m2f.archer.failure.NetworkFailure.NoConnection
import com.m2f.archer.failure.NetworkFailure.Redirect
import com.m2f.archer.failure.NetworkFailure.ServerFailure
import com.m2f.archer.failure.NetworkFailure.UnhandledNetworkFailure
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface Settings {
    val mainFallbacks: (Failure) -> Boolean
    val storageFallbacks: (Failure) -> Boolean
    val ignoreCache: Boolean

    @OptIn(ExperimentalTime::class)
    val cache: CacheDataSource<CacheMetaInformation, Instant>

    @OptIn(ExperimentalTime::class)
    fun getCurrentTime(): Instant

    companion object Default : Settings {
        override val mainFallbacks = { failure: Failure ->
            failure is DataNotFound ||
                failure is Invalid ||
                failure is NoConnection ||
                failure is ServerFailure ||
                failure is Redirect ||
                failure is UnhandledNetworkFailure
        }

        /**
         * Failures that will be used for storage calls to fallback into network
         */
        override val storageFallbacks = { failure: Failure ->
            failure is DataNotFound ||
                failure is Invalid
        }

        override val ignoreCache: Boolean = false

        @OptIn(ExperimentalTime::class)
        override val cache: CacheDataSource<CacheMetaInformation, Instant> by lazy { MemoizedExpirationCache() }

        @OptIn(ExperimentalTime::class)
        override fun getCurrentTime(): Instant =
            Clock.System.now()
    }
}

val Settings.configuration: Configuration
    get() = Configuration(settings = this)
