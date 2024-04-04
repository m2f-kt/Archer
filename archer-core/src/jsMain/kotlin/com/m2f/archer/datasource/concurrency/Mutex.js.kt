package com.m2f.archer.datasource.concurrency

import com.m2f.archer.datasource.DataSource

/**
 * Due to Js Single Threaded nature, this function does nothing.
 */
actual fun <F, Q, A> DataSource<F, Q, A>.parallelism(parallelism: Int): DataSource<F, Q, A> = this
