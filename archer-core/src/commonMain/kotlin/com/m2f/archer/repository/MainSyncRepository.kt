package com.m2f.archer.repository

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.recover
import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put

class MainSyncRepository<K, A>(
    private val mainDataSource: GetDataSource<K, A & Any>,
    private val storeDataSource: StoreDataSource<K, A & Any>,
    private val fallbackChecks: List<Failure> = emptyList(),
) : GetRepository<K, A & Any> {

    override suspend fun invoke(q: Get<K>): Either<Failure, A & Any> =
        mainDataSource(q)
            .flatMap { storeDataSource(Put(q.key, it)) }
            .recover { f ->
                if (f in fallbackChecks) {
                    storeDataSource(q)
                        .mapLeft { f }
                        .bind()
                } else {
                    raise(f)
                }
            }
}
