package com.m2f.archer.repository

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.crud.DeleteRepository
import com.m2f.archer.datasource.DataSource
import com.m2f.archer.query.Delete

class SingleDataSourceRepository<in Q, out A>(
    private val dataSource: DataSource<Q, A>,
) : Repository<Q, A> {
    override suspend fun ArcherRaise.invoke(q: Q): A = dataSource.run { invoke(q) }
}

fun <Q, A> DataSource<Q, A>.toRepository(): Repository<Q, A> =
    SingleDataSourceRepository(this)

fun <K> DeleteDataSource<K>.toDataSource(): DataSource<Delete<K>, Unit> = DataSource { q ->
    delete(q)
}

fun <K> DeleteDataSource<K>.toDeleteRepository(): DeleteRepository<K> =
    toDataSource().toRepository()
