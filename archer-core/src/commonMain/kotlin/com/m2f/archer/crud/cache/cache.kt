package com.m2f.archer.crud.cache

import com.m2f.archer.configuration.DefaultConfiguration.cacheStrategy
import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepositoryStrategy
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.StrategyBuilder
import com.m2f.archer.crud.archerRecover
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Always
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.Invalid
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import kotlinx.datetime.Clock.System

interface CacheDataSource<K, A> : StoreDataSource<K, A>, DeleteDataSource<K>

infix fun <K, A> GetDataSource<K, A>.cacheWith(storage: StoreDataSource<K, A>): StrategyBuilder<K, A> =
    StrategyBuilder(this, storage)

fun <K, A> StrategyBuilder<K, A>.build(): GetRepositoryStrategy<K, A> =
    cacheStrategy(
        mainDataSource,
        storeDataSource,
    )

inline fun <K, reified A> GetDataSource<K, A>.cache(
    storage: StoreDataSource<K, A> = InMemoryDataSource(),
    expiration: CacheExpiration = Never
): GetRepositoryStrategy<K, A> = cacheStrategy(this@cache, storage.expires(expiration))

inline fun <K, reified A> StoreDataSource<K, A>.expires(
    expiration: CacheExpiration,
): StoreDataSource<K, A> =
    when (expiration) {
        Never -> this
        Always ->
            StoreDataSource { q ->
                when (q) {
                    is Put -> {
                        this@expires.put(q.key, q.value ?: raise(com.m2f.archer.failure.DataEmpty))
                    }

                    is Get -> {
                        raise(Invalid)
                    }
                }
            }

        is After ->
            StoreDataSource { q ->
                val info = CacheMetaInformation(
                    key = q.key.toString(),
                    classIdentifier = A::class.simpleName.toString()
                )
                when (q) {
                    is Put -> {
                        val now = System.now()
                        val expirationDate = (now + expiration.time)
                        cache.put(info, expirationDate)
                        this@expires.run { invoke(q) }
                    }

                    is Get -> {
                        val now = System.now()
                        val isValid: Boolean = if (ignoreCache) {
                            true
                        } else {
                            archerRecover(block = { cache.get(info).let { now - it }.isNegative() }) {
                                false
                            }
                        }
                        archerRecover(
                            block = {
                                this@expires.get(q.key).takeIf { isValid } ?: raise(Invalid)
                            },
                        ) { failure ->
                            cache.run { delete(Delete(info)) }
                            raise(failure)
                        }
                    }
                }
            }
    }
