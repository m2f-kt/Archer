package com.m2f.archer.crud

import arrow.core.Either
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.repository.Repository

typealias CRUDRepository<Q, A> = Repository<Failure, Q, A>
typealias GetRepository<K, A> = CRUDRepository<Get<K>, A>
typealias DeleteRepository<K> = CRUDRepository<Delete<K>, Unit>
typealias PutRepository<K, A> = CRUDRepository<Put<K, A>, A>

suspend inline fun <reified K, reified A> GetRepository<K, A>.get(param: K): Either<Failure, A> =
    invoke(Get(param))

suspend inline fun <reified A> GetRepository<Unit, A>.get(): Either<Failure, A> =
    invoke(Get(Unit))

suspend inline fun <reified K, reified A> PutRepository<K, A>.put(
    param: K,
    value: A,
): Either<Failure, A> = invoke(
    Put(param, value),
)

suspend inline fun <reified K> PutRepository<K, Unit>.put(
    value: K,
): Either<Failure, Unit> = invoke(
    Put(value, Unit),
)

suspend inline fun <reified K> DeleteRepository<K>.delete(param: K): Either<Failure, Unit> =
    invoke(Delete(param))
