package com.m2f.archer.crud

import com.m2f.archer.crud.operation.MainOperation
import com.m2f.archer.crud.operation.MainSyncOperation
import com.m2f.archer.crud.operation.Operation
import com.m2f.archer.crud.operation.StoreOperation
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Get
import com.m2f.archer.repository.MainSyncRepository
import com.m2f.archer.repository.Repository
import com.m2f.archer.repository.StoreSyncRepository
import com.m2f.archer.repository.toRepository

fun interface GetRepositoryStrategy<K, out A> {
    fun create(operation: Operation): GetRepository<K, A>
}

class StrategyBuilder<K, A>(
    val mainDataSource: GetDataSource<K, A>,
    val storeDataSource: StoreDataSource<K, A>
) {

    val mainFallback: List<Failure> = mainFallbacks
    val storeFallback: List<Failure> = storageFallbacks

    fun build(): GetRepositoryStrategy<K, A> = cacheStrategy(
        mainDataSource,
        storeDataSource,
        mainFallback,
        storeFallback,
    )
}

fun <K, A> cacheStrategy(
    mainDataSource: GetDataSource<K, A>,
    storeDataSource: StoreDataSource<K, A>,
    mainFallback: List<Failure> = mainFallbacks,
    storeFallback: List<Failure> = storageFallbacks,
): GetRepositoryStrategy<K, A> = GetRepositoryStrategy { operation ->
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
    cacheStrategy(this, store).create(MainSyncOperation)

/**
 *
 * [Uncurry](https://en.wikipedia.org/wiki/Currying) the [GetRepositoryStrategy] to create a [Repository] using the provided [operation]
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
suspend inline fun <reified K, reified A> GetRepositoryStrategy<K, A>.get(
    operation: Operation,
    q: K,
) = create(operation).get(q)
