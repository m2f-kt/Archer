package com.m2f.archer.repository

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.crud.DeleteRepository
import com.m2f.archer.datasource.DataSource
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Delete

class SingleDataSourceRepository<F, in Q, out A>(
    private val dataSource: DataSource<F, Q, A>,
) : Repository<F, Q, A> {
    override suspend fun ArcherRaise.invoke(q: Q): A = dataSource.run { invoke(q) }
}

fun <F, Q, A> DataSource<F, Q, A>.toRepository(): Repository<F, Q, A> =
    SingleDataSourceRepository(this)

fun <K> DeleteDataSource<K>.toDataSource(): DataSource<Failure, Delete<K>, Unit> = DataSource { q ->
    delete(q)
}

fun <K> DeleteDataSource<K>.toDeleteRepository(): DeleteRepository<K> =
    toDataSource().toRepository()
