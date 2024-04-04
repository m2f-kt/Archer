package com.m2f.archer.datasource

import com.m2f.archer.crud.cache.CacheDataSource

/**
 * A data source that stores data in memory.
 *
 * @param K The type of the key that the data source uses to store data.
 * @param A The type of the data that the data source stores.
 */
expect class InMemoryDataSource<K, A>(initialValues: Map<K, A> = emptyMap()) :
    CacheDataSource<K, A>
