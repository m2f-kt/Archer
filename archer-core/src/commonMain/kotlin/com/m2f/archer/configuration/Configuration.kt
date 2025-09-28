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
import com.m2f.archer.crud.cache.CacheExpiration
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.build
import com.m2f.archer.crud.cache.expires
import com.m2f.archer.crud.operation.Main
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.repository.MainSyncRepository
import com.m2f.archer.repository.StoreSyncRepository
import com.m2f.archer.repository.toRepository
import kotlin.experimental.ExperimentalTypeInference
import kotlin.time.Duration

open class Configuration(private val settings: Settings) : Settings by settings {

    fun <K, A> cacheStrategy(
        mainDataSource: GetDataSource<K, A>,
        storeDataSource: StoreDataSource<K, A>,
    ): GetRepositoryStrategy<K, A> = GetRepositoryStrategy { operation ->
        when (operation) {
            is Main -> mainDataSource.toRepository()
            is Store -> storeDataSource.toRepository()
            is MainSync -> MainSyncRepository(mainDataSource, storeDataSource, mainFallbacks)
            is StoreSync -> StoreSyncRepository(
                storeDataSource,
                mainDataSource,
                storageFallbacks,
                mainFallbacks,
            )
        }
    }

    infix fun <K, A> GetDataSource<K, A>.fallbackWith(
        store: StoreDataSource<K, A>
    ): GetRepository<K, A> =
        cacheStrategy(this, store).create(MainSync)

    inline infix fun <K, reified A> StrategyBuilder<K, A>.expiresIn(duration: Duration): GetRepositoryStrategy<K, A> =
        expires(After(duration))

    inline infix fun <K, reified A> StrategyBuilder<K, A>.expires(
        expiration: CacheExpiration
    ): GetRepositoryStrategy<K, A> =
        StrategyBuilder(
            mainDataSource,
            storeDataSource.expires(
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
    ): Unit = com.m2f.archer.crud.unit(this, block)

    companion object {
        val Default: Configuration = Configuration(settings = Settings.Default)
        fun ignoreCache(settings: Settings = Settings.Default) = Configuration(
            settings = object : Settings by settings {
                override val ignoreCache: Boolean = true
            }
        )
    }
}
