package com.m2f.archer.mapper

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.datasource.DataSource

fun <Q, A, B> DataSource<Q, A>.andThen(f: suspend ArcherRaise.(A) -> B): DataSource<Q, B> =
    DataSource<Q, B> { query ->
        f(execute(query))
    }

fun <Q, A, B> DataSource<Q, A>.andThen(ds: DataSource<A, B>): DataSource<Q, B> =
    DataSource<Q, B> { query ->
        ds.execute(execute(query))
    }
