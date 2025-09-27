@file:OptIn(ExperimentalTime::class)

package com.m2f.archer.crud.cache.memcache

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.m2f.archer.ExpirationRegistryQueries
import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.memcache.deps.queriesRepo
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
import com.m2f.archer.query.Put
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class MemoizedExpirationCache(
    private val repo: GetRepository<Unit, ExpirationRegistryQueries> = queriesRepo
) :
    CacheDataSource<CacheMetaInformation, Instant> {

    private val mutex: Mutex = Mutex()

    override suspend fun ArcherRaise.invoke(q: KeyQuery<CacheMetaInformation, out Instant>): Instant =
        mutex.withLock {
            val queries = repo.get(Unit)
            queries.transactionWithResult {
                when (q) {
                    is Get -> queries.getInstant(
                        key = q.key.key, hash = q.key.hashCode().toLong()
                    ).awaitAsOneOrNull()?.instant?.let { Instant.parse(it) } ?: raise(
                        DataNotFound
                    )

                    is Put -> {
                        val instant = q.value ?: raise(DataEmpty)
                        val now = Clock.System.now()
                        queries.insertInstant(
                            key = q.key.key,
                            hash = q.key.hashCode().toLong(),
                            name = q.key.classIdentifier,
                            instant = instant.toString(),
                            whenCreated = now.toString()
                        )
                        instant
                    }
                }
            }
        }

    override suspend fun ArcherRaise.delete(q: Delete<CacheMetaInformation>) =
        mutex.withLock {
            val queries = repo.get(Unit)
            queries.transaction { queries.deleteInstant(key = q.key.key, hash = q.key.hashCode().toLong()) }
        }
}
