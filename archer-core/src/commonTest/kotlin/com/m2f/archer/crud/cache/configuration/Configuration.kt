@file:OptIn(ExperimentalTime::class)

package com.m2f.archer.crud.cache.configuration

import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.DefaultConfiguration
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.Failure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val testConfiguration: (scheduler: TestCoroutineScheduler) -> Configuration = { scheduler ->
    object : Configuration() {
        override val mainFallbacks: (Failure) -> Boolean = DefaultConfiguration.mainFallbacks
        override val storageFallbacks: (Failure) -> Boolean = DefaultConfiguration.storageFallbacks
        override val ignoreCache: Boolean = DefaultConfiguration.ignoreCache
        override val cache: CacheDataSource<CacheMetaInformation, Instant> = MemoizedExpirationCache(
            repo = fakeQueriesRepo
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun getCurrentTime(): Instant = Instant.fromEpochMilliseconds(scheduler.currentTime)
    }
}

val inMemoryCacheConfiguration: (scheduler: TestCoroutineScheduler) -> Configuration = { scheduler ->
    object : Configuration() {
        override val mainFallbacks: (Failure) -> Boolean = DefaultConfiguration.mainFallbacks
        override val storageFallbacks: (Failure) -> Boolean = DefaultConfiguration.storageFallbacks
        override val ignoreCache: Boolean = DefaultConfiguration.ignoreCache
        override val cache: CacheDataSource<CacheMetaInformation, Instant> = InMemoryDataSource()

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun getCurrentTime(): Instant = Instant.fromEpochMilliseconds(scheduler.currentTime)
    }
}
