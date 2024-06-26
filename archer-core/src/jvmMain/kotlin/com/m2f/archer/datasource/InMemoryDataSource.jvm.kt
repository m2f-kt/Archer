package com.m2f.archer.datasource

import arrow.fx.stm.TMap
import arrow.fx.stm.atomically
import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.failure.DataNotFound
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
actual class InMemoryDataSource<K, A> actual constructor(initialValues: Map<K, A>) :
    CacheDataSource<K, A> {
    private val values: TMap<K, A> = runBlocking {
        TMap.new<K, A>()
            .apply {
                atomically {
                    initialValues.forEach { (key, value) -> insert(key, value) }
                }
            }
    }

    actual override suspend fun ArcherRaise.invoke(q: KeyQuery<K, out A>): A =
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

    actual override suspend fun ArcherRaise.delete(q: Delete<K>) {
        atomically { values.remove(q.key) }
    }
}
