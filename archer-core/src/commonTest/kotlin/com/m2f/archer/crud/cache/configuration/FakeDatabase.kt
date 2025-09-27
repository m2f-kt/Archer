package com.m2f.archer.crud.cache.configuration

import com.m2f.archer.ExpirationRegistryQueries
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cache.cache
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.database.FakeDatabaseDriverFactory
import com.m2f.archer.sqldelight.CacheExpirationDatabase

val fakeQueriesRepo
    get() = getDatabase()

private fun getDatabase(): GetRepository<Unit, ExpirationRegistryQueries> =
    getDataSource<Unit, ExpirationRegistryQueries> {
        CacheExpirationDatabase(
            FakeDatabaseDriverFactory.createDriver(CacheExpirationDatabase.Schema)
        ).expirationRegistryQueries
    }.cache(expiration = Never).create(StoreSync)
