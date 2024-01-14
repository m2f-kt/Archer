package com.m2f.archer.datasource.concurrency

import arrow.core.Either
import com.m2f.archer.datasource.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
actual fun <F, Q, A> DataSource<F, Q, A>.parallelism(parallelism: Int): DataSource<F, Q, A> =
    object : DataSource<F, Q, A> {
        val dispatcher = Dispatchers.IO.limitedParallelism(parallelism)
        override suspend fun invoke(q: Q): Either<F, A> = withContext(dispatcher) { this@parallelism.invoke(q) }
    }
