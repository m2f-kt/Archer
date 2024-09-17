package com.m2f.archer.datasource.concurrency

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.datasource.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
actual fun <Q, A> DataSource<Q, A>.parallelism(parallelism: Int): DataSource<Q, A> =
    object : DataSource<Q, A> {
        val dispatcher = Dispatchers.IO.limitedParallelism(parallelism)
        override suspend fun ArcherRaise.invoke(q: Q): A =
            withContext(dispatcher) { this@parallelism.run { invoke(q) } }
    }
