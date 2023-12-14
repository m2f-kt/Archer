package com.m2f.archer.crud

import com.m2f.archer.repository.StoreSyncRepository
import com.m2f.archer.repository.toRepository
import com.m2f.archer.crud.operation.MainOperation
import com.m2f.archer.crud.operation.MainSyncOperation
import com.m2f.archer.crud.operation.Operation
import com.m2f.archer.crud.operation.StoreOperation
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Get
import com.m2f.archer.repository.MainSyncRepository
import com.m2f.archer.repository.Repository

fun interface RepositoryFactory<out F, in Q, out A> {
    fun create(operation: Operation): Repository<F, Q, A>
}

fun <K, Q : Get<K>, A> cacheFactory(
    mainDataSource: GetDataSource<K, A>,
    storeDataSource: StoreDataSource<K, A>,
    mainFallback: List<Failure> = mainAiraloFallbacks,
    storeFallback: List<Failure> = storageAiraloFallbacks,
): RepositoryFactory<Failure, Q, A> = RepositoryFactory { operation ->
    when (operation) {
        is MainOperation -> mainDataSource.toRepository()
        is StoreOperation -> storeDataSource.toRepository()
        is MainSyncOperation -> MainSyncRepository(mainDataSource, storeDataSource, mainFallback)
        is StoreSyncOperation -> StoreSyncRepository(
            storeDataSource,
            mainDataSource,
            storeFallback,
            mainFallback,
        )
    }
}

infix fun <K, A> GetDataSource<K, A>.fallbackWith(store: StoreDataSource<K, A>): GetRepository<K, A> =
    cacheFactory(this, store).create(MainSyncOperation)

/**
 *
 * [Uncurry](https://en.wikipedia.org/wiki/Currying) the [RepositoryFactory] to create a [Repository] using the provided [operation]
 * and fetch the data using [Q].
 *
 * @param F The generic type parameter representing some context or requirement for the repository.
 * @param K The generic key type used within the get operation.
 * @param Q A type that must implement the [Get] interface for the provided key [K].
 * @param A The generic type parameter representing some additional context or requirement for the repository.
 * @param operation The [Operation] to be performed.
 * @param q The query of type [Q] used to perform the get operation.
 * @return The result of the get operation, returned by invoking the created operation with the query [q].
 */
suspend inline fun <F, K, Q : Get<K>, A> RepositoryFactory<F, Q, A>.get(
    operation: Operation,
    q: Q,
) = create(operation)(q)

/**
 *
 * [Uncurry](https://en.wikipedia.org/wiki/Currying) the [RepositoryFactory] to create a [Repository] using the provided [operation].
 * and fetch the data using [Q].
 *
 * @param F The generic type parameter representing some context or requirement for the repository.
 * @param K The generic key type used within the get operation.
 * @param Q A type that must implement the [Get] interface for the provided key [K].
 * @param A The generic type parameter representing some additional context or requirement for the repository.
 * @param operation The [Operation] to be performed.
 * @param q The query of type [Q] used to perform the get operation.
 * @return The result of the get operation, returned by invoking the created operation with the query [q].
 */
suspend inline fun <F, K, A> RepositoryFactory<F, Get<K>, A>.get(
    operation: Operation,
    k: K
) =
    create(operation)(Get(k))