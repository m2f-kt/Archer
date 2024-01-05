package com.m2f.archer.crud.cache.memcache.deps

import com.m2f.archer.ExpirationRegistryQueries
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.cache.cache
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.data.storage.DatabaseDriverFactory.createDriver
import com.m2f.archer.sqldelight.CacheExpirationDatabase

internal val queriesRepo by lazy { getDatabase() }

private fun getDatabase(): GetRepository<Unit, ExpirationRegistryQueries> =
    getDataSource<Unit, ExpirationRegistryQueries> {
        CacheExpirationDatabase(createDriver(CacheExpirationDatabase.Schema)).expirationRegistryQueries
    }
        .cache()
        .create(StoreSyncOperation)
