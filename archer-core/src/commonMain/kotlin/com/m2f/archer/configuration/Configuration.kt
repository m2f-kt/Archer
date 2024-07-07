package com.m2f.archer.configuration

import com.m2f.archer.crud.GetRepositoryStrategy
import com.m2f.archer.crud.StrategyBuilder
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.CacheExpiration
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.build
import com.m2f.archer.crud.cache.expires
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid
import com.m2f.archer.failure.NetworkFailure.NoConnection
import com.m2f.archer.failure.NetworkFailure.Redirect
import com.m2f.archer.failure.NetworkFailure.ServerFailure
import com.m2f.archer.failure.NetworkFailure.UnhandledNetworkFailure
import kotlinx.datetime.Instant
import kotlin.time.Duration

abstract class Configuration {
    abstract val mainFallbacks: (Failure) -> Boolean
    abstract val storageFallbacks: (Failure) -> Boolean

    abstract val cache: CacheDataSource<CacheMetaInformation, Instant>

    inline infix fun <K, reified A> StrategyBuilder<K, A>.expiresIn(duration: Duration):
            GetRepositoryStrategy<K, A> =
        expires(After(duration))

    inline infix fun <K, reified A> StrategyBuilder<K, A>.expires(
        expiration: CacheExpiration
    ): GetRepositoryStrategy<K, A> =
        StrategyBuilder(
            mainDataSource,
            storeDataSource.expires(
                configuration = this@Configuration,
                expiration = expiration
            )
        ).build()

}

object DefaultConfiguration : Configuration() {
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

    override val cache: CacheDataSource<CacheMetaInformation, Instant> = InMemoryDataSource()//MemoizedExpirationCache()
}