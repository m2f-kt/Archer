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
    private val storeDataSource: StoreDataSource<K, A & Any>,
    private val mainDataSource: GetDataSource<K, A & Any>,
    private val fallbackChecks: List<Failure> = emptyList(),
    private val mainFallbackChecks: List<Failure> = emptyList(),
) : GetRepository<K, A> {

    override suspend fun ArcherRaise.invoke(q: Get<K>): A & Any =
        archerRecover(
            block = {
                storeDataSource.get(q.key)
            },
            recover = { failure ->
                if (failure in fallbackChecks) {
                    MainSyncRepository(
                        mainDataSource,
                        storeDataSource,
                        mainFallbackChecks,
                    ).get(q.key)
                } else {
                    raise(failure)
                }
            },
            catch = { exception -> raise(Unhandled(exception)) }
        )
}
