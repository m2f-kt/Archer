package com.m2f.archer.crud

import com.m2f.archer.query.Get
import com.m2f.archer.query.Put

/**
 * Combines an [GetDataSource] and an [PutDataSource] into a single [StoreDataSource].
 *
 * This function overloads the `+` operator to facilitate the merging of a data source responsible for "get" operations
 * with another responsible for "put" operations. The resulting [StoreDataSource] will delegate `Get` queries to
 * [GetDataSource] and `Put` queries to [PutDataSource].
 *
 * @param K The type of the key used to identify data.
 * @param A The type of the data being stored or retrieved.
 * @param put The [PutDataSource] instance responsible for handling "put" operations.
 * @receiver The [GetDataSource] instance responsible for handling "get" operations.
 *
 * @return A new [StoreDataSource] instance that delegates `Get` queries to the original [GetDataSource] and
 * `Put` queries to the provided [PutDataSource].
 *
 * @sample
 * ```kotlin
 * val getDataSource: GetDataSource<String, Int> = // ...
 * val putDataSource: PutDataSource<String, Int> = // ...
 * val storageDataSource = getDataSource + putDataSource
 * ```
 *
 * @see GetDataSource
 * @see PutDataSource
 * @see StoreDataSource
 */
operator fun <K, A> GetDataSource<K, A>.plus(put: PutDataSource<K, A>): StoreDataSource<K, A> =
    StoreDataSource { query ->
        when (query) {
            is Get -> invoke(query)
            is Put -> put.run { invoke(query) }
        }
    }
