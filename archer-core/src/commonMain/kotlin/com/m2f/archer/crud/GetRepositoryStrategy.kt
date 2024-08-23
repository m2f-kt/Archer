package com.m2f.archer.crud

import com.m2f.archer.crud.operation.Operation

fun interface GetRepositoryStrategy<K, out A> {
    fun create(operation: Operation): GetRepository<K, A>
}

class StrategyBuilder<K, A>(
    val mainDataSource: GetDataSource<K, A>,
    val storeDataSource: StoreDataSource<K, A>
)
