package com.m2f.archer.crud

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.None
import arrow.core.Option
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.recover
import arrow.core.some
import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.Settings
import com.m2f.archer.configuration.configuration
import com.m2f.archer.crud.Ice.Content
import com.m2f.archer.crud.Ice.Error
import com.m2f.archer.crud.cache.invalidateCache
import com.m2f.archer.crud.operation.Main
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Operation
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.datasource.DataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Idle
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.repository.Repository
import kotlin.experimental.ExperimentalTypeInference

typealias Result<T> = Either<Failure, T>

class ArcherRaise(raise: Raise<Failure>, settings: Settings) :
    Raise<Failure> by raise, Configuration(settings) {

    fun <A> Ice<A>.bind(): A = fold(
        ifIdle = { raise(Idle) },
        ifContent = { it },
        ifError = { raise(it) },
    )

    fun <A> A?.bind(): A = this ?: raise(DataNotFound)

    suspend fun <K, A> Repository<K, A>.execute(param: K): A =
        invoke(param)

    suspend fun <K, A> DataSource<K, A>.execute(param: K): A =
        invoke(param)

    suspend fun <A> Repository<Unit, A>.execute(): A =
        invoke(Unit)

    suspend fun <A> DataSource<Unit, A>.execute(): A =
        invoke(Unit)

    suspend fun <K, A> GetRepository<K, A>.get(param: K): A =
        invoke(Get(param))

    suspend fun <K, A> PutRepository<K, A>.put(
        param: K,
        value: A,
    ): A = invoke(
        Put(param, value),
    )

    suspend fun <K> PutRepository<K, Unit>.put(
        value: K,
    ) = invoke(
        Put(value, Unit),
    )

    suspend fun <K> DeleteRepository<K>.delete(param: K) =
        invoke(Delete(param))

    suspend fun <K> DeleteDataSource<K>.delete(param: K) =
        delete(Delete(param))

    suspend fun <K, A> GetDataSource<K, A>.get(queryKey: K): A =
        invoke(Get(queryKey))

    suspend fun <K, A> PutDataSource<K, A>.post(param: K): A =
        invoke(Put(param, null))

    suspend fun <K, A> PutDataSource<K, A>.put(param: K, value: A): A =
        invoke(Put(param, value))

    suspend fun <K> PutDataSource<K, Unit>.put(param: K) =
        invoke(Put(param, Unit))

    suspend fun <K, A> GetRepositoryStrategy<K, A>.get(
        operation: Operation,
        q: K,
    ) = when (operation) {
        Store -> with(
            ArcherRaise(
                this@ArcherRaise,
                ignoreCache(settings = configuration)
            )
        ) {
            val repository = create(operation)
            repository.get(q)
        }

        Main,
        MainSync,
        StoreSync -> create((operation)).get(q)
    }

    suspend inline fun <K : Any, reified A> GetRepositoryStrategy<K, A>.invalidate(
        key: K
    ): A {
        invalidateCache<A>(key)
        return get(MainSync, key)
    }
}

sealed interface Ice<out A> {
    data object Idle : Ice<Nothing>
    data class Content<A>(val value: A) : Ice<A>
    data class Error(val error: Failure) : Ice<Nothing>
}

inline fun <A, T> Ice<A>.fold(
    ifIdle: () -> T,
    ifContent: (A) -> T,
    ifError: (Failure) -> T,
): T = when (this) {
    is Ice.Idle -> ifIdle()
    is Content -> ifContent(value)
    is Error -> ifError(error)
}

@OptIn(ExperimentalTypeInference::class)
inline fun <A> ice(
    configuration: Configuration = Configuration.Default,
    @BuilderInference block: ArcherRaise.() -> A
): Ice<A> =
    recover({ Ice.Content(block(ArcherRaise(this, configuration))) }) { e ->
        when (e) {
            is Idle -> Ice.Idle
            else -> Ice.Error(e)
        }
    }

@OptIn(ExperimentalTypeInference::class)
inline fun <A> either(
    configuration: Configuration = Configuration.Default,
    @BuilderInference block: ArcherRaise.() -> A
): Result<A> =
    recover({ Either.Right(block(ArcherRaise(this, configuration))) }, ::Left)

@OptIn(ExperimentalTypeInference::class)
inline fun <A> result(
    configuration: Configuration = Configuration.Default,
    @BuilderInference block: ArcherRaise.() -> A
): Result<A> = either(block = block, configuration = configuration)

@OptIn(ExperimentalTypeInference::class)
inline fun <A> nullable(
    configuration: Configuration = Configuration.Default,
    @BuilderInference block: ArcherRaise.() -> A
): A? =
    recover({ block(ArcherRaise(this, configuration)) }) { null }

@OptIn(ExperimentalTypeInference::class)
inline fun <A> nil(
    configuration: Configuration = Configuration.Default,
    @BuilderInference block: ArcherRaise.() -> A
): A? = nullable(configuration, block)

@OptIn(ExperimentalTypeInference::class)
inline fun <A> option(
    configuration: Configuration = Configuration.Default,
    @BuilderInference block: ArcherRaise.() -> A
): Option<A> =
    recover({ block(ArcherRaise(this, configuration)).some() }) { None }

@OptIn(ExperimentalTypeInference::class)
inline fun <A> bool(
    configuration: Configuration = Configuration.Default,
    @BuilderInference block: ArcherRaise.() -> A
): Boolean =
    recover({
        block(ArcherRaise(this, configuration))
        true
    }) { false }

@OptIn(ExperimentalTypeInference::class)
inline fun <A> unit(
    configuration: Configuration = Configuration.Default,
    @BuilderInference block: ArcherRaise.() -> A
): Unit =
    recover({ block(ArcherRaise(this, configuration)) }) { }

@OptIn(ExperimentalTypeInference::class)
@RaiseDSL
context(settings: Settings) inline fun <A> archerRecover(
    @BuilderInference block: ArcherRaise.() -> A,
    @BuilderInference recover: (error: Failure) -> A,
): A = recover({ block(ArcherRaise(this, settings.configuration)) }, recover)

@OptIn(ExperimentalTypeInference::class)
@RaiseDSL
context(settings: Settings) inline fun <A> archerRecover(
    @BuilderInference block: ArcherRaise.() -> A,
    @BuilderInference recover: (error: Failure) -> A,
    @BuilderInference catch: (error: Throwable) -> A,
): A = recover({ block(ArcherRaise(this, settings.configuration)) }, recover, catch)
