package com.m2f.archer.datasource.concurrency

import com.m2f.archer.datasource.DataSource

/**
 * Due to Js Single Threaded nature, this function does nothing.
 */
actual fun <Q, A> DataSource<Q, A>.parallelism(parallelism: Int): DataSource<Q, A> = this
