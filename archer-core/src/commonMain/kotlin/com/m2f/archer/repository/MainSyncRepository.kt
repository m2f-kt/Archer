package com.m2f.archer.repository

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.archerRecover
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Unhandled
import com.m2f.archer.query.Get

class MainSyncRepository<K, A>(
    private val mainDataSource: GetDataSource<K, A>,
    private val storeDataSource: StoreDataSource<K, A>,
    private val fallbackChecks: (Failure) -> Boolean = { false },
) : GetRepository<K, A> {

    override suspend fun ArcherRaise.invoke(q: Get<K>): A =
        archerRecover(
            block = {
                storeDataSource.put(q.key, mainDataSource.get(q.key))
            },
            recover = { failure ->
                if (fallbackChecks(failure)) {

                    archerRecover(
                        block = { storeDataSource.get(q.key) },
                        recover = {
                            if (it is Unhandled) {
                                raise(it)
                            } else {
                                raise(failure)
                            }
                        }
                    )
                } else {
                    raise(failure)
                }
            },
            catch = { exception -> raise(Unhandled(exception)) }
        )
}
