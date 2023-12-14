package com.m2f.archer.crud.cache

import com.m2f.archer.crud.delete
import com.m2f.archer.crud.deleteDataSource
import com.m2f.archer.repository.toDeleteRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class DeleteRepository: BehaviorSpec({

    Given("A Delete Repository") {
        var deleteCalled = false
        val detleteDataSource = deleteDataSource<Int> { key -> deleteCalled = true }
        val repository = detleteDataSource.toDeleteRepository()

        When("Calling delete") {
            repository.delete(1)

            Then("The delete data source should be called") {
                deleteCalled shouldBe true
            }
        }
    }
})