package com.m2f.archer.crud

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
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
    suspend fun delete(q: Delete<K>): Either<Failure, Unit>
}

suspend fun <K, A> GetDataSource<K, A>.get(queryKey: K): Either<Failure, A> =
    invoke(Get(queryKey))

suspend fun <K, A> PutDataSource<K, A>.post(param: K): Either<Failure, A> =
    invoke(Put(param, null))

suspend fun <K, A> PutDataSource<K, A>.put(param: K, value: A): Either<Failure, A> =
    invoke(Put(param, value))

suspend fun <K> PutDataSource<K, Unit>.put(param: K): Either<Failure, Unit> =
    invoke(Put(param, Unit))

inline fun <K, T> getDataSource(crossinline block: suspend Raise<Failure>.(K) -> T): GetDataSource<K, T> =
    GetDataSource { query ->
        either {
            catch(block = {
                block(query.key)
            }) {
                raise(Unhandled(it))
            }
        }
    }

inline fun <K, T> putDataSource(
    crossinline block: suspend Raise<Failure>.(K, T) -> T
): PutDataSource<K, T> =
    PutDataSource { query ->
        either {
            val value = query.value ?: raise(DataEmpty)
            block(query.key, value)
        }
    }

inline fun <K, T> postDataSource(crossinline block: suspend Raise<Failure>.(K) -> T): PutDataSource<K, T> =
    PutDataSource { query ->
        either {
            if (query.value != null) raise(Invalid)
            block(query.key)
        }
    }

inline fun <K> deleteDataSource(crossinline block: suspend Raise<Failure>.(K) -> Unit): DeleteDataSource<K> =
    DeleteDataSource { query ->
        either {
            block(query.key)
        }
    }
