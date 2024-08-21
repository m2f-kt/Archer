package com.m2f.archer.configuration

import arrow.core.Option
import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.GetRepositoryStrategy
import com.m2f.archer.crud.Ice
import com.m2f.archer.crud.Result
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.StrategyBuilder
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.CacheExpiration
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cache.build
import com.m2f.archer.crud.cache.expires
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache
import com.m2f.archer.crud.cacheStrategy
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid
import com.m2f.archer.failure.NetworkFailure.NoConnection
import com.m2f.archer.failure.NetworkFailure.Redirect
import com.m2f.archer.failure.NetworkFailure.ServerFailure
import com.m2f.archer.failure.NetworkFailure.UnhandledNetworkFailure
import kotlinx.datetime.Instant
import kotlin.experimental.ExperimentalTypeInference
import kotlin.time.Duration

abstract class Configuration {
    abstract val mainFallbacks: (Failure) -> Boolean
    abstract val storageFallbacks: (Failure) -> Boolean

    abstract val cache: CacheDataSource<CacheMetaInformation, Instant>

    infix fun <K, A> GetDataSource<K, A>.fallbackWith(
        store: StoreDataSource<K, A>
    ): GetRepository<K, A> =
        cacheStrategy(DefaultConfiguration, this, store).create(MainSync)

    inline fun <K, reified A> GetDataSource<K, A>.cache(
        storage: StoreDataSource<K, A> = InMemoryDataSource(),
        expiration: CacheExpiration = Never
    ): GetRepositoryStrategy<K, A> =
        cacheStrategy(this@Configuration, this@cache, storage.expires(this@Configuration, expiration))

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

    @OptIn(ExperimentalTypeInference::class)
    inline fun <A> ice(
        @BuilderInference block: ArcherRaise.() -> A
    ): Ice<A> =
        com.m2f.archer.crud.ice(this, block)

    @OptIn(ExperimentalTypeInference::class)
    inline fun <A> either(
        @BuilderInference block: ArcherRaise.() -> A
    ): Result<A> = com.m2f.archer.crud.either(configuration = this, block = block)


    @OptIn(ExperimentalTypeInference::class)
    inline fun <A> result(
        @BuilderInference block: ArcherRaise.() -> A
    ): Result<A> = com.m2f.archer.crud.result(block = block, configuration = this)

    @OptIn(ExperimentalTypeInference::class)
    inline fun <A> nullable(
        @BuilderInference block: ArcherRaise.() -> A
    ): A? = com.m2f.archer.crud.nullable(this, block)


    @OptIn(ExperimentalTypeInference::class)
    inline fun <A> nil(
        @BuilderInference block: ArcherRaise.() -> A
    ): A? = nullable(block)

    @OptIn(ExperimentalTypeInference::class)
    inline fun <A> option(
        @BuilderInference block: ArcherRaise.() -> A
    ): Option<A> = com.m2f.archer.crud.option(this, block)

    @OptIn(ExperimentalTypeInference::class)
    inline fun <A> bool(
        @BuilderInference block: ArcherRaise.() -> A
    ): Boolean = com.m2f.archer.crud.bool(this, block)

    @OptIn(ExperimentalTypeInference::class)
    inline fun <A> unit(
        @BuilderInference block: ArcherRaise.() -> A
    ) = com.m2f.archer.crud.unit(this, block)
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

    override val cache: CacheDataSource<CacheMetaInformation, Instant> = MemoizedExpirationCache()
}