package com.m2f.archer.crud

import arrow.core.Either
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.repository.Repository

typealias CRUDRepository<Q, A> = Repository<Failure, Q, A & Any>
typealias GetRepository<K, A> = CRUDRepository<Get<K>, A & Any>
typealias DeleteRepository<K> = CRUDRepository<Delete<K>, Unit>
typealias PutRepository<K, A> = CRUDRepository<Put<K, A & Any>, A & Any>

suspend fun <K, A> GetRepository<K, A & Any>.get(param: K): Either<Failure, A & Any> =
    invoke(Get(param))

suspend fun <A> GetRepository<Unit, A & Any>.get(): Either<Failure, A & Any> =
    invoke(Get(Unit))

suspend fun <K, A> PutRepository<K, A & Any>.put(
    param: K,
    value: A & Any,
): Either<Failure, A> = invoke(
    Put(param, value),
)

suspend fun <K> PutRepository<K, Unit>.put(
    value: K,
): Either<Failure, Unit> = invoke(
    Put(value, Unit),
)

suspend fun <K> DeleteRepository<K>.delete(param: K): Either<Failure, Unit> =
    invoke(Delete(param))
