package com.m2f.archer.crud.cache.configuration

import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.DefaultConfiguration
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.Failure
import kotlinx.datetime.Instant

val testConfiguration: () -> Configuration = {
    object : Configuration() {
        override val mainFallbacks: (Failure) -> Boolean = DefaultConfiguration.mainFallbacks
        override val storageFallbacks: (Failure) -> Boolean = DefaultConfiguration.storageFallbacks
        override val ignoreCache: Boolean = DefaultConfiguration.ignoreCache
        override val cache: CacheDataSource<CacheMetaInformation, Instant> = MemoizedExpirationCache(
            repo = fakeQueriesRepo
        )
    }
}

val inMemoryCacheConfiguration: () -> Configuration = {
    object : Configuration() {
        override val mainFallbacks: (Failure) -> Boolean = DefaultConfiguration.mainFallbacks
        override val storageFallbacks: (Failure) -> Boolean = DefaultConfiguration.storageFallbacks
        override val ignoreCache: Boolean = DefaultConfiguration.ignoreCache
        override val cache: CacheDataSource<CacheMetaInformation, Instant> = InMemoryDataSource()
    }
}
