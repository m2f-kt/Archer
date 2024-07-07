package com.m2f.archer.crud

import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.DefaultConfiguration
import com.m2f.archer.crud.operation.Main
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Operation
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.repository.MainSyncRepository
import com.m2f.archer.repository.StoreSyncRepository
import com.m2f.archer.repository.toRepository

fun interface GetRepositoryStrategy<K, out A> {
    fun create(operation: Operation): GetRepository<K, A>
}

class StrategyBuilder<K, A>(
    val mainDataSource: GetDataSource<K, A>,
    val storeDataSource: StoreDataSource<K, A>
)

fun <K, A> cacheStrategy(
    configuration: Configuration = DefaultConfiguration,
    mainDataSource: GetDataSource<K, A>,
    storeDataSource: StoreDataSource<K, A>,
): GetRepositoryStrategy<K, A> = GetRepositoryStrategy { operation ->
    when (operation) {
        is Main -> mainDataSource.toRepository()
        is Store -> storeDataSource.toRepository()
        is MainSync -> MainSyncRepository(mainDataSource, storeDataSource, configuration.mainFallbacks)
        is StoreSync -> StoreSyncRepository(
            storeDataSource,
            mainDataSource,
            configuration.storageFallbacks,
            configuration.mainFallbacks,
        )
    }
}

infix fun <K, A> GetDataSource<K, A>.fallbackWith(
    store: StoreDataSource<K, A>
): GetRepository<K, A> =
    cacheStrategy(DefaultConfiguration, this, store).create(MainSync)
