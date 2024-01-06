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
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache
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

inline fun <K, reified A> GetDataSource<K, A & Any>.cache(
    storage: StoreDataSource<K, A & Any> = InMemoryDataSource(),
    expiration: CacheExpiration = Never
): GetRepositoryStrategy<K, A & Any> = cacheStrategy(this, storage.expires(expiration))

infix fun <K, A> GetDataSource<K, A & Any>.cacheWith(storage: StoreDataSource<K, A & Any>): StrategyBuilder<K, A> =
    StrategyBuilder(this, storage)

inline infix fun <K, reified A> StrategyBuilder<K, A>.expires(expiration: CacheExpiration): GetRepositoryStrategy<K, A> =
    StrategyBuilder(mainDataSource, storeDataSource.expires(expiration)).build()

inline infix fun <K, reified A> StrategyBuilder<K, A & Any>.expiresIn(duration: Duration): GetRepositoryStrategy<K, A & Any> =
    expires(After(duration))

inline fun <K, reified A> StoreDataSource<K, A & Any>.expires(
    expiration: CacheExpiration,
    cache: CacheDataSource<CacheMetaInformation, Instant> = MemoizedExpirationCache()
): StoreDataSource<K, A & Any> = when (expiration) {
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
            val info = CacheMetaInformation(
                key = query.key.toString(),
                classIdentifier = A::class.simpleName.toString()
            )
            when (query) {

                is Put -> {
                    val now = Clock.System.now()
                    val expirationDate = (now + expiration.time)
                    cache.put(info, expirationDate)
                        .flatMap { invoke(query) }
                        .bind()
                }

                is Get -> {
                    validate {
                        val now = Clock.System.now()
                        val expired = cache.get(info)
                            .map { now - it }
                            .map { it.isPositive() }
                            .getOrElse { true } // if there is no expiration date, it is expired
                        !expired
                    }
                        .invoke(query)
                        .recover {
                            //delete the expiration instant if there is no data
                            cache.delete(Delete(info)).bind()
                            raise(it)
                        }
                        .bind()
                }
            }
        }
    }
}
