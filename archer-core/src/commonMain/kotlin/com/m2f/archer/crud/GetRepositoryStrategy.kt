package com.m2f.archer.crud

import com.m2f.archer.crud.operation.MainOperation
import com.m2f.archer.crud.operation.MainSyncOperation
import com.m2f.archer.crud.operation.Operation
import com.m2f.archer.crud.operation.StoreOperation
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.failure.Failure
import com.m2f.archer.repository.MainSyncRepository
import com.m2f.archer.repository.StoreSyncRepository
import com.m2f.archer.repository.toRepository

fun interface GetRepositoryStrategy<K, out A> {
    fun create(operation: Operation): GetRepository<K, A>
}

class StrategyBuilder<K, A>(
    val mainDataSource: GetDataSource<K, A & Any>,
    val storeDataSource: StoreDataSource<K, A & Any>
) {

    val mainFallback: List<Failure> = mainFallbacks
    val storeFallback: List<Failure> = storageFallbacks

    fun build(): GetRepositoryStrategy<K, A & Any> = cacheStrategy(
        mainDataSource,
        storeDataSource,
        mainFallback,
        storeFallback,
    )
}

fun <K, A> cacheStrategy(
    mainDataSource: GetDataSource<K, A & Any>,
    storeDataSource: StoreDataSource<K, A & Any>,
    mainFallback: List<Failure> = mainFallbacks,
    storeFallback: List<Failure> = storageFallbacks,
): GetRepositoryStrategy<K, A & Any> = GetRepositoryStrategy { operation ->
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

infix fun <K, A> GetDataSource<K, A & Any>.fallbackWith(store: StoreDataSource<K, A & Any>): GetRepository<K, A & Any> =
    cacheStrategy(this, store).create(MainSyncOperation)

suspend fun <K, A> GetRepositoryStrategy<K, A>.get(
    operation: Operation,
    q: K,
) = create(operation).get(q)
