package com.m2f.archer.crud.cache

import com.m2f.archer.crud.delete
import com.m2f.archer.crud.deleteDataSource
import com.m2f.archer.repository.toDeleteRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeleteRepository : FunSpec({

    test("A Delete Repository Calling delete data source should be called") {
        var deleteCalled = false
        val detleteDataSource = deleteDataSource<Int> { _ -> deleteCalled = true }
        val repository = detleteDataSource.toDeleteRepository()

        repository.delete(1)

        deleteCalled shouldBe true
    }
})
