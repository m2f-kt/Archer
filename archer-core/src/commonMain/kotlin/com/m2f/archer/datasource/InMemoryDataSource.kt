package com.m2f.archer.datasource

import arrow.core.Either
import arrow.core.raise.either
import arrow.fx.stm.TMap
import arrow.fx.stm.atomically
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
import com.m2f.archer.query.Put
import kotlinx.coroutines.runBlocking

/**
 * A data source that stores data in memory.
 *
 * @param K The type of the key that the data source uses to store data.
 * @param A The type of the data that the data source stores.
 */
class InMemoryDataSource<K, A>(initialValues: Map<K, A> = emptyMap()) :
    CacheDataSource<K, A> {

    private val values: TMap<K, A> = runBlocking {
        TMap.new<K, A>()
            .apply {
                atomically {
                    initialValues.forEach { (key, value) -> insert(key, value) }
                }
            }
    }

    override suspend fun invoke(q: KeyQuery<K, out A>): Either<Failure, A> = either {
        atomically {
            when (q) {
                is Put -> {
                    q.value?.also { values[q.key] = it } ?: raise(DataNotFound)
                }

                is Get -> {
                    values[q.key] ?: raise(DataNotFound)
                }
            }
        }
    }

    override suspend fun delete(q: Delete<K>): Either<Failure, Unit> = either {
        atomically { values.remove(q.key) }
    }
}
