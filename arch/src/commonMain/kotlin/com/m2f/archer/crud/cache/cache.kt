package com.m2f.archer.crud.cache

import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.raise.either
import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepositoryStrategy
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.StrategyBuilder
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cacheStrategy
import com.m2f.archer.crud.get
import com.m2f.archer.crud.put
import com.m2f.archer.crud.validate.validate
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

inline fun <reified K, reified A> GetDataSource<K, A>.cache(
    storage: StoreDataSource<K, A> = InMemoryDataSource(),
    expiration: CacheExpiration = Never
): GetRepositoryStrategy<K, A> = cacheStrategy(this, storage.expires(expiration))

infix fun <K, A> GetDataSource<K, A>.cache(storage: StoreDataSource<K, A>): StrategyBuilder<K, A> =
    StrategyBuilder(this, storage)

inline infix fun <reified K, A> StrategyBuilder<K, A>.expires(expiration: CacheExpiration): GetRepositoryStrategy<K, A> =
    StrategyBuilder(mainDataSource, storeDataSource.expires(expiration)).build()

inline infix fun <reified K, A> StrategyBuilder<K, A>.expiresIn(duration: Duration): GetRepositoryStrategy<K, A> =
    StrategyBuilder(mainDataSource, storeDataSource.expires(After(duration))).build()

inline fun <reified K, A> StoreDataSource<K, A>.expires(
    expiration: CacheExpiration,
    expirationStore: StoreDataSource<K, Instant> = InMemoryDataSource()
): StoreDataSource<K, A> = when (expiration) {
    Never -> this
    CacheExpiration.Always -> {
        StoreDataSource { query ->
            either {
                when (query) {
                    is Put -> invoke(query).bind()
                    is Get -> validate { false }.invoke(query).bind()
                }
            }
        }
    }

    is CacheExpiration.After -> StoreDataSource { query ->
        either {
            when (query) {
                is Put -> {
                    val now = Clock.System.now()
                    val expirationDate = (now + expiration.time)
                    expirationStore.put(query.key, expirationDate)
                        .flatMap { invoke(query) }
                        .bind()
                }

                is Get -> {
                    val now = Clock.System.now()
                    val expired = expirationStore.get(query.key)
                        .map { now - it }
                        .map { it.isPositive() }
                        .getOrElse { false }
                    validate { !expired }
                        .invoke(query)
                        .bind()
                }
            }
        }
    }
}
