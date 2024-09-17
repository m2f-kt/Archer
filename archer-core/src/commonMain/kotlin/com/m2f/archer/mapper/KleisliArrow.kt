package com.m2f.archer.mapper

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.datasource.DataSource
import com.m2f.archer.repository.Repository
import com.m2f.archer.repository.toRepository

fun <Q, A, B> DataSource<Q, A>.andThen(f: suspend ArcherRaise.(A) -> B): DataSource<Q, B> =
    DataSource<Q, B> { query ->
        f(execute(query))
    }

fun <Q, A, B> DataSource<Q, A>.andThen(ds: DataSource<A, B>): DataSource<Q, B> =
    DataSource<Q, B> { query ->
        ds.execute(execute(query))
    }

fun <Q, A, B> Repository<Q, A>.andThen(f: suspend ArcherRaise.(A) -> B): Repository<Q, B> =
    DataSource<Q, B> { query ->
        f(execute(query))
    }.toRepository()

fun <Q, A, B> Repository<Q, A>.andThen(ds: Repository<A, B>): Repository<Q, B> =
    DataSource<Q, B> { query ->
        ds.execute(execute(query))
    }.toRepository()
