package com.m2f.archer.crud.validate

import com.m2f.archer.datasource.DataSource
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid

inline fun <Q, A> DataSource<Failure, Q, A>.validate(crossinline validation: suspend (A) -> Boolean) =
    DataSource<Failure, Q, A> { query ->
        val result = invoke(query)
        if (!validation(result)) raise(Invalid) else result
    }
