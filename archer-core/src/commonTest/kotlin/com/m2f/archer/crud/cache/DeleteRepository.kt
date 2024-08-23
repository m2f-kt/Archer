package com.m2f.archer.crud.cache

import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.repository.toDeleteRepository
import com.m2f.archer.utils.archerTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeleteRepository : FunSpec({

    archerTest("A Delete Repository Calling delete data source should be called") {
        var deleteCalled = false
        val detleteDataSource = DeleteDataSource<Int> { _ -> deleteCalled = true }
        val repository = detleteDataSource.toDeleteRepository()

        either { repository.delete(1) }

        deleteCalled shouldBe true
    }
})
