package com.m2f.archer.crud

import com.m2f.archer.crud.operation.Main
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Operation
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.failure.Failure
import com.m2f.archer.repository.MainSyncRepository
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
        is Main -> mainDataSource.toRepository()
        is Store -> storeDataSource.toRepository()
        is MainSync -> MainSyncRepository(mainDataSource, storeDataSource, mainFallback)
        is StoreSync -> StoreSyncRepository(
            storeDataSource,
            mainDataSource,
            storeFallback,
            mainFallback,
        )
    }
}

infix fun <K, A> GetDataSource<K, A>.fallbackWith(store: StoreDataSource<K, A>): GetRepository<K, A> =
    cacheStrategy(this, store).create(MainSync)
