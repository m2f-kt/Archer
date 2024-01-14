package com.m2f.archer.crud.cache

import com.m2f.archer.crud.PutDataSource
import com.m2f.archer.crud.put
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.repository.toRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PutRepositoryTest : FunSpec({

    test("A PutRepository Calling put The result should be the same as the put data source") {
        val putDataSource: PutDataSource<Int, String> =
            putDataSource { key, value -> "put $value with key $key" }
        val repository = putDataSource.toRepository()

        val result = repository.put(1, "value")

        result shouldBe putDataSource.put(1, "value")
    }

    test("A PutRepository without value (value is Unit) Calling put passing a single value The result should be the same as the put data source passing Unit as value") {
        val postDataSource: PutDataSource<Int, Unit> = putDataSource { key, _ -> "put with key $key" }
        val repository = postDataSource.toRepository()

        val result = repository.put(1)

        result shouldBe postDataSource.put(1)
    }
})
