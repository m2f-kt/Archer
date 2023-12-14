package com.m2f.archer.crud.cache

import com.m2f.archer.crud.put
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.query.Put
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec

class PutDataSourceTest: FunSpec( {

    context("Creation") {
        test("creating a putDataSource with the DSL ensures the value is not null") {
            val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

            putDataSource.put(1, "test") shouldBeRight "put test with key 1"
            putDataSource.invoke(Put(1, null)) shouldBeLeft DataEmpty
        }
    }
})