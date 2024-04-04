package com.m2f.archer.crud.cache

import com.m2f.archer.crud.ArcherRaise
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
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache
import com.m2f.archer.crud.cacheStrategy
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.Invalid
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
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

inline infix fun <K, reified A> StrategyBuilder<K, A>.expires(expiration: CacheExpiration):
        GetRepositoryStrategy<K, A> =
    StrategyBuilder(mainDataSource, storeDataSource.expires(expiration)).build()

inline infix fun <K, reified A> StrategyBuilder<K, A & Any>.expiresIn(duration: Duration):
        GetRepositoryStrategy<K, A & Any> =
    expires(After(duration))

inline fun <K, reified A> StoreDataSource<K, A & Any>.expires(
    expiration: CacheExpiration,
    cache: CacheDataSource<CacheMetaInformation, Instant> = MemoizedExpirationCache()
): StoreDataSource<K, A & Any> =
    when (expiration) {
        Never -> this
        Always -> object : StoreDataSource<K, A & Any> {
            override suspend fun ArcherRaise.invoke(q: KeyQuery<K, out A & Any>): A & Any {
                return when (q) {
                    is Put -> {
                        this@expires.put(q.key, q.value ?: raise(DataEmpty))
                    }

                    is Get -> {
                        raise(Invalid)
                    }
                }
            }
        }

        is After -> object : StoreDataSource<K, A & Any> {
            override suspend fun ArcherRaise.invoke(q: KeyQuery<K, out A & Any>): A & Any {
                val info = CacheMetaInformation(
                    key = q.key.toString(),
                    classIdentifier = A::class.simpleName.toString()
                )
                return when (q) {
                    is Put -> {
                        val now = Clock.System.now()
                        val expirationDate = (now + expiration.time)
                        cache.put(info, expirationDate)
                        this@expires.run { invoke(q) }
                    }

                    is Get -> {
                        val now = Clock.System.now()
                        val isValid: Boolean = archerRecover({ cache.get(info).let { now - it }.isNegative() }) {
                            false
                        }
                        archerRecover(
                            block = { this@expires.get(q.key).takeIf { isValid } ?: raise(Invalid) },
                        ) { failure ->
                            cache.run { delete(Delete(info)) }
                            raise(failure)
                        }
                    }
                }
            }
        }
    }
