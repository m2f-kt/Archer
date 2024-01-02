package com.m2f.archer.crud.cache

import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.recover
import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepositoryStrategy
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.StrategyBuilder
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Always
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cacheStrategy
import com.m2f.archer.crud.get
import com.m2f.archer.crud.put
import com.m2f.archer.crud.validate.validate
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

interface CacheDataSource<K, A> : StoreDataSource<K, A>, DeleteDataSource<K>

fun <K, A> GetDataSource<K, A>.cache(
    storage: StoreDataSource<K, A> = InMemoryDataSource(),
    expiration: CacheExpiration = Never
): GetRepositoryStrategy<K, A> = cacheStrategy(this, storage.expires(expiration))

infix fun <K, A> GetDataSource<K, A>.cacheWith(storage: StoreDataSource<K, A>): StrategyBuilder<K, A> =
    StrategyBuilder(this, storage)

infix fun <K, A> StrategyBuilder<K, A>.expires(expiration: CacheExpiration): GetRepositoryStrategy<K, A> =
    StrategyBuilder(mainDataSource, storeDataSource.expires(expiration)).build()

infix fun <K, A> StrategyBuilder<K, A>.expiresIn(duration: Duration): GetRepositoryStrategy<K, A> =
    expires(After(duration))

fun <K, A> StoreDataSource<K, A>.expires(
    expiration: CacheExpiration,
    cache: CacheDataSource<K, Instant> = InMemoryDataSource()
): StoreDataSource<K, A> = when (expiration) {
    Never -> this
    Always -> {
        StoreDataSource { query ->
            either {
                when (query) {
                    is Put -> invoke(query).bind()
                    is Get -> validate { false }.invoke(query).bind()
                }
            }
        }
    }

    is After -> StoreDataSource { query ->
        either {
            when (query) {
                is Put -> {
                    val now = Clock.System.now()
                    val expirationDate = (now + expiration.time)
                    cache.put(query.key, expirationDate)
                        .flatMap { invoke(query) }
                        .bind()
                }

                is Get -> {
                    val now = Clock.System.now()
                    val expired = cache.get(query.key)
                        .map { now - it }
                        .map { it.isPositive() }
                        .getOrElse { true } // if there is no expiration date, it is expired

                    validate { !expired }
                        .invoke(query)
                        .recover {
                            //delete the expiration instant if there is no data
                            cache.delete(Delete(query.key)).bind()
                            raise(it)
                        }
                        .bind()
                }
            }
        }
    }
}
