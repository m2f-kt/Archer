package com.m2f.archer.repository

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.crud.DeleteRepository
import com.m2f.archer.datasource.DataSource
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Delete

class SingleDataSourceRepository<F, in Q, out A>(
    private val dataSource: DataSource<F, Q, A & Any>,
) : Repository<F, Q, A & Any> {
    override suspend fun ArcherRaise.invoke(q: Q): A & Any = dataSource.run { invoke(q) }
}

fun <F, Q, A> DataSource<F, Q, A & Any>.toRepository(): Repository<F, Q, A & Any> =
    SingleDataSourceRepository(this)

fun <K> DeleteDataSource<K>.toDataSource(): DataSource<Failure, Delete<K>, Unit> = DataSource { q ->
    delete(q)
}

fun <K> DeleteDataSource<K>.toDeleteRepository(): DeleteRepository<K> =
    toDataSource().toRepository()
