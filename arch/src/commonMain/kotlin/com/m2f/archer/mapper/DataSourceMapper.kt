package com.m2f.archer.mapper

import com.m2f.archer.crud.PutDataSource
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.datasource.DataSource
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.query.map
import kotlin.jvm.JvmName

/**
 * Transforms the output of a DataSource using the given [mapper] function.
 *
 * @param mapper A function to transform data
 * @return A new DataSource with its output transformed by the [mapper] function.
 */
inline fun <F, Q, A, B> DataSource<F, Q, A>.map(crossinline mapper: (A) -> B): DataSource<F, Q, B> =
    DataSource {
        invoke(it).map(mapper)
    }

/**
 * Transforms the output and input of a DataSource using the given [bijection].
 *
 * @param bijection A bijection that provides transformation functions between types A and B.
 * @return A new DataSource with its output and input transformed by the [bijection].
 */
@JvmName("mapPutDataSource")
fun <K, A, B> PutDataSource<K, A>.map(bijection: Bijection<A, B>): PutDataSource<K, B> =
    PutDataSource {
        invoke(it.map(bijection::to)).map(bijection::from)
    }

/**
 * Transforms the data stored in or retrieved from a StorageDataSource using the given [bijection].
 *
 * @param bijection A bijection that provides transformation functions between types A and B.
 * @return A new StorageDataSource with its data transformed by the [bijection].
 */
fun <K, A, B> StoreDataSource<K, A>.map(bijection: Bijection<A, B>) = StoreDataSource<K, B> {
    when (it) {
        is Get -> invoke(it).map(bijection::from)
        is Put -> invoke(it.map(bijection::to)).map(bijection::from)
    }
}
