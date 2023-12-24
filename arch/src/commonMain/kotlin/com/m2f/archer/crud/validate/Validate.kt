package com.m2f.archer.crud.validate

import arrow.core.raise.either
import com.airalo.babel.architecture.datasource.DataSource
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid

inline fun <Q, A> DataSource<Failure, Q, A>.validate(crossinline validation: (A) -> Boolean) =
    DataSource<Failure, Q, A> { query ->
        either {
            val result = invoke(query).bind()
            if(!validation(result)) raise(Invalid) else result
        }
    }