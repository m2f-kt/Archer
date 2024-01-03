package com.m2f.archer.crud.cache.memcache

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.memcache.deps.queriesRepo
import com.m2f.archer.crud.get
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
import com.m2f.archer.query.Put
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant

class MemoizedExpirationCache(private val databaseName: String = DATABASE_NAME) :
    CacheDataSource<CacheMetaInformation, Instant> {

    private companion object {
        const val DATABASE_NAME = "expiration_registry"
    }

    override suspend fun invoke(q: KeyQuery<CacheMetaInformation, out Instant>): Either<Failure, Instant> = either {

        val queries = queriesRepo.get(databaseName).bind()
        queries.transactionWithResult {
            when (q) {
                is Get -> queries.getInstant(
                    key = q.key.key, hash = q.key.hashCode().toLong()
                ).executeAsOneOrNull()?.instant?.toInstant() ?: raise(
                    DataNotFound
                )

                is Put -> {
                    val instant = q.value ?: raise(DataEmpty)
                    val now = Clock.System.now()
                    queries.insertInstant(
                        key = q.key.key,
                        hash = q.key.hashCode().toLong(),
                        name = q.key.classIdentifier,
                        fullName = q.key.classFullIdentifier,
                        instant = instant.toString(),
                        whenCreated = now.toString()
                    )
                    instant
                }
            }
        }
    }

    override suspend fun delete(q: Delete<CacheMetaInformation>): Either<Failure, Unit> = either {
        val queries = queriesRepo.get(DATABASE_NAME).bind()
        queries.transaction { queries.deleteInstant(key = q.key.key, hash = q.key.hashCode().toLong()) }
        Unit.right()
    }

}