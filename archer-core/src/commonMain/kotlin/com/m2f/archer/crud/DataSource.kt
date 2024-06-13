package com.m2f.archer.crud

import arrow.core.raise.catch
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.m2f.archer.datasource.DataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid
import com.m2f.archer.failure.Unhandled
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
import com.m2f.archer.query.Put

typealias CRUDDataSource<Q, A> = DataSource<Failure, Q, A>
typealias GetDataSource<K, A> = CRUDDataSource<Get<K>, A>
typealias PutDataSource<K, A> = CRUDDataSource<Put<K, out A>, A>
typealias StoreDataSource<K, A> = CRUDDataSource<KeyQuery<K, out A>, A>

fun interface DeleteDataSource<K> {
    suspend fun ArcherRaise.delete(q: Delete<K>)
}

inline fun <K, T> getDataSource(crossinline block: suspend ArcherRaise.(K) -> T): GetDataSource<K, T> =
    GetDataSource { query ->
        catch(block = {
            block(query.key)
        }) {
            raise(Unhandled(it))
        }
    }

inline fun <K, T> putDataSource(
    crossinline block: suspend ArcherRaise.(K, T) -> T
): PutDataSource<K, T> =
    PutDataSource { (key, value) ->
        ensureNotNull(value) { raise(DataEmpty) }
        block(key, value)
    }

inline fun <K, T> postDataSource(crossinline block: suspend ArcherRaise.(K) -> T): PutDataSource<K, T> =
    PutDataSource { query ->
        ensure(query.value == null) { Invalid }
        block(query.key)
    }

inline fun <K> deleteDataSource(crossinline block: suspend ArcherRaise.(K) -> Unit): DeleteDataSource<K> =
    DeleteDataSource { query ->
        block(query.key)
    }
