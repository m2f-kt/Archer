package com.m2f.archer.crud.cache.memcache.deps

import com.m2f.archer.ExpirationRegistryQueries
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.cache.cache
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.data.storage.DatabaseDriverFactory.Companion.createDriver
import com.m2f.archer.sqldelight.CacheExpirationDatabase

internal val queriesRepo by lazy {
    getDatabase("expiration_registry")
}

private fun getDatabase(name: String): GetRepository<String, ExpirationRegistryQueries> =
    getDataSource<String, ExpirationRegistryQueries> { databaseName ->
        CacheExpirationDatabase(createDriver(databaseName)).expirationRegistryQueries
    }
        .cache()
        .create(StoreSyncOperation)