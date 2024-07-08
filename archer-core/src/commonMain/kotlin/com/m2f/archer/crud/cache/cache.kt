package com.m2f.archer.crud.cache

import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.DefaultConfiguration
import com.m2f.archer.configuration.DefaultConfiguration.expires
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
import com.m2f.archer.crud.cacheStrategy
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.Invalid
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import kotlinx.datetime.Clock.System
import kotlin.time.Duration

interface CacheDataSource<K, A> : StoreDataSource<K, A>, DeleteDataSource<K>

infix fun <K, A> GetDataSource<K, A>.cacheWith(storage: StoreDataSource<K, A>): StrategyBuilder<K, A> =
    StrategyBuilder(this, storage)

fun <K, A> StrategyBuilder<K, A>.build(configuration: Configuration = DefaultConfiguration): GetRepositoryStrategy<K, A> =
    cacheStrategy(
        configuration,
        mainDataSource,
        storeDataSource,
    )

inline infix fun <K, reified A> StrategyBuilder<K, A>.expiresIn(duration: Duration):
        GetRepositoryStrategy<K, A> =
    expires(After(duration))

inline fun <K, reified A> GetDataSource<K, A>.cache(
    configuration: Configuration = DefaultConfiguration,
    storage: StoreDataSource<K, A> = InMemoryDataSource(),
    expiration: CacheExpiration = Never
): GetRepositoryStrategy<K, A> = cacheStrategy(configuration, this@cache, storage.expires(configuration, expiration))

inline fun <K, reified A> StoreDataSource<K, A>.expires(
    configuration: Configuration = DefaultConfiguration,
    expiration: CacheExpiration,
): StoreDataSource<K, A> =
    when (expiration) {
        Never -> this
        Always -> StoreDataSource { q ->
            when (q) {
                is Put -> {
                    this@expires.put(q.key, q.value ?: raise(com.m2f.archer.failure.DataEmpty))
                }

                is Get -> {
                    raise(Invalid)
                }
            }
        }

        is After -> StoreDataSource { q ->
            val info = CacheMetaInformation(
                key = q.key.toString(),
                classIdentifier = A::class.simpleName.toString()
            )
            when (q) {
                is Put -> {
                    val now = System.now()
                    val expirationDate = (now + expiration.time)
                    configuration.cache.put(info, expirationDate)
                    this@expires.run { invoke(q) }
                }

                is Get -> {
                    val now = System.now()
                    val isValid: Boolean =
                        archerRecover({ configuration.cache.get(info).let { now - it }.isNegative() }) {
                            false
                        }
                    archerRecover(
                        block = {
                            this@expires.get(q.key).takeIf { isValid } ?: raise(Invalid)
                        },
                    ) { failure ->
                        configuration.cache.run { delete(Delete(info)) }
                        raise(failure)
                    }
                }
            }
        }
    }

