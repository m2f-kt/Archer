package com.m2f.archer.datasource.concurrency

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.datasource.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
actual fun <F, Q, A> DataSource<F, Q, A>.parallelism(parallelism: Int): DataSource<F, Q, A> =
    object : DataSource<F, Q, A> {
        val dispatcher = Dispatchers.IO.limitedParallelism(parallelism)
        override suspend fun ArcherRaise.invoke(q: Q): A & Any =
            withContext(dispatcher) { this@parallelism.run { invoke(q) } }
    }
