package com.m2f.archer.datasource

import arrow.core.Either
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Delete
import com.m2f.archer.query.KeyQuery

/**
 * A data source that stores data in memory.
 *
 * @param K The type of the key that the data source uses to store data.
 * @param A The type of the data that the data source stores.
 */
expect class InMemoryDataSource<K, A>(initialValues: Map<K, A> = emptyMap()) :
    CacheDataSource<K, A> {

    override suspend fun invoke(q: KeyQuery<K, out A>): Either<Failure, A>

    override suspend fun delete(q: Delete<K>): Either<Failure, Unit>
}
