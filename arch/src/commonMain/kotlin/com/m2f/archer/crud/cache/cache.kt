package com.m2f.archer.crud.cache

import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepositoryStrategy
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cacheStrategy
import com.m2f.archer.datasource.InMemoryDataSource

inline fun <reified K, reified A> GetDataSource<K, A>.cache(
    storage: StoreDataSource<K, A> = InMemoryDataSource()
): GetRepositoryStrategy<K, A> = cacheStrategy(this, storage)