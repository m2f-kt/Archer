package com.m2f.archer.repository

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.archerRecover
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Unhandled
import com.m2f.archer.query.Get

class StoreSyncRepository<K, A>(
    private val storeDataSource: StoreDataSource<K, A>,
    private val mainDataSource: GetDataSource<K, A>,
    private val fallbackChecks: (Failure) -> Boolean = { false },
    private val mainFallbackChecks: (Failure) -> Boolean = { false },
) : GetRepository<K, A> {

    override suspend fun ArcherRaise.invoke(q: Get<K>): A =
        archerRecover(
            block = {
                storeDataSource.get(q.key)
            },
            recover = { failure: Failure ->
                if (fallbackChecks(failure)) {
                    MainSyncRepository(
                        mainDataSource,
                        storeDataSource,
                        mainFallbackChecks,
                    ).get(q.key)
                } else {
                    raise(failure)
                }
            },
            catch = { exception: Throwable -> raise(Unhandled(exception)) }
        )
}
