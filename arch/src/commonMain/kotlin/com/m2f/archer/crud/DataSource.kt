package com.m2f.archer.crud

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.airalo.babel.architecture.datasource.DataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
import com.m2f.archer.query.Put

typealias CRUDDataSource<Q, A> = DataSource<Failure, Q, A>
typealias GetDataSource<K, A> = CRUDDataSource<Get<K>, A>
typealias PutDataSource<K, A> = CRUDDataSource<Put<K, out A>, A>
typealias StoreDataSource<K, A> = CRUDDataSource<KeyQuery<K, out A>, A>

@Suppress("FUN_INTERFACE_WITH_SUSPEND_FUNCTION")
fun interface DeleteDataSource<K> {
    suspend fun delete(q: Delete<K>): Either<Failure, Unit>
}

suspend inline fun <reified K, reified A> GetDataSource<K, A>.get(param: K): Either<Failure, A> =
    invoke(Get(param))

suspend inline fun <reified K, reified A> PutDataSource<K, A>.put(
    param: K,
    value: A,
): Either<Failure, A> = invoke(
    Put(param, value),
)

suspend inline fun <reified K, reified A> PutDataSource<K, A>.post(
    param: K
): Either<Failure, A> = invoke(
    Put(param, null),
)

suspend inline fun <reified K> PutDataSource<K, Unit>.put(param: K): Either<Failure, Unit> =
    invoke(
        Put(param, Unit),
    )

inline fun <K, T> getDataSource(crossinline block: suspend Raise<Failure>.(K) -> T): GetDataSource<K, T> =
    GetDataSource { query ->
        either {
            block(query.key)
        }
    }

inline fun <K, T> putDataSource(crossinline block: suspend Raise<Failure>.(K, T) -> T): PutDataSource<K, T> =
    PutDataSource { query ->
        either {
            val value = query.value ?: raise(DataEmpty)
            block(query.key, value)
        }
    }

inline fun <K, T> postDataSource(crossinline block: suspend Raise<Failure>.(K) -> T): PutDataSource<K, T> =
    PutDataSource { query ->
        either {
            if(query.value != null) raise(Invalid)
            block(query.key)
        }
    }

inline fun <K> deleteDataSource(crossinline block: suspend Raise<Failure>.(K) -> Unit): DeleteDataSource<K> =
    DeleteDataSource { query ->
        either {
            block(query.key)
        }
    }




