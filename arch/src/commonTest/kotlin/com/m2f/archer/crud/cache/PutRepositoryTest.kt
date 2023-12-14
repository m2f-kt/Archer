package com.m2f.archer.crud.cache

import com.m2f.archer.crud.PutDataSource
import com.m2f.archer.crud.put
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.repository.toRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PutRepositoryTest : BehaviorSpec({

    Given("A PutRepository") {
        val putDataSource: PutDataSource<Int, String> =
            putDataSource { key, value -> "put $value with key $key" }
        val repository = putDataSource.toRepository()

        When("Calling put") {
            val result = repository.put(1, "value")

            Then("The result should be the same as the put data source") {
                result shouldBe putDataSource.put(1, "value")
            }
        }
    }

    Given("A PutRepository without value (value is Unit)") {
        val postDataSource: PutDataSource<Int, Unit> = putDataSource { key, _ -> "put with key $key" }
        val repository = postDataSource.toRepository()

        When("Calling put passing a single value") {
            val result = repository.put(1)

            Then("The result should be the same as the put data source passing Unit as value") {
                result shouldBe postDataSource.put(1)
            }
        }
    }
})