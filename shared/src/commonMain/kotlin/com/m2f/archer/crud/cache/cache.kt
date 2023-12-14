package com.m2f.archer.crud.cache

import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cacheFactory
import com.m2f.archer.crud.operation.Operation
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.datasource.InMemoryDataSource

inline fun <reified K, reified A> GetDataSource<K, A>.cache(
    storage: StoreDataSource<K, A> = InMemoryDataSource(),
    operation: Operation = StoreSyncOperation,
): GetRepository<K, A> = cacheFactory(this, storage).create(operation)