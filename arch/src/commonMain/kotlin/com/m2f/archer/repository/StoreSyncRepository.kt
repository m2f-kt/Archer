package com.m2f.archer.repository

import arrow.core.Either
import arrow.core.recover
import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Get

class StoreSyncRepository<K, A>(
    private val storeDataSource: StoreDataSource<K, A>,
    private val mainDataSource: GetDataSource<K, A>,
    private val fallbackChecks: List<Failure> = emptyList(),
    private val mainFallbackChecks: List<Failure> = emptyList(),
) : GetRepository<K, A> {

    override suspend fun invoke(q: Get<K>): Either<Failure, A> =
        storeDataSource(q)
            .recover { f ->
                if (f in fallbackChecks) {
                    MainSyncRepository(
                        mainDataSource,
                        storeDataSource,
                        mainFallbackChecks,
                    )(q).bind()
                } else {
                    raise(f)
                }
            }
}
