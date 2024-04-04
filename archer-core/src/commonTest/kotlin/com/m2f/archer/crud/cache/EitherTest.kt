package com.m2f.archer.crud.cache

import com.m2f.archer.crud.either
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.DataNotFound
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec

class EitherTest : FunSpec({

    test("regular call wraps into Right") {
        val getDataSource = getDataSource<Int, String> { "Success" }
        either { getDataSource.get(0) } shouldBeRight "Success"
    }

    test("failure call wraps into None") {
        val getDataSource = getDataSource<Int, String> { raise(DataNotFound) }
        either { getDataSource.get(0) } shouldBeLeft DataNotFound
    }
})