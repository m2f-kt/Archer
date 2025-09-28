package com.m2f.archer.crud.cache.memcache.deps

import com.m2f.archer.ExpirationRegistryQueries
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cache.cache
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.data.storage.DatabaseDriverFactory.Companion.createDriver
import com.m2f.archer.sqldelight.CacheExpirationDatabase

val queriesRepo by lazy {
    getDatabase()
}

internal fun getDatabase(): GetRepository<Unit, ExpirationRegistryQueries> =
    getDataSource<Unit, ExpirationRegistryQueries> {
        CacheExpirationDatabase(createDriver(CacheExpirationDatabase.Schema)).expirationRegistryQueries
    }.cache(expiration = Never).create(StoreSync)
