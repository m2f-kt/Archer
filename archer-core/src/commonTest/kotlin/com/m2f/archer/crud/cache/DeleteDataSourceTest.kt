package com.m2f.archer.crud.cache

import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.crud.deleteDataSource
import com.m2f.archer.crud.ice
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.repository.toDeleteRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeleteDataSourceTest : FunSpec({

    test("Create a delete data source with dsl") {
        val deleteDSL = deleteDataSource<Int> { }.toDeleteRepository()
        val delete = DeleteDataSource<Int> { }.toDeleteRepository()

        ice { deleteDSL.delete(1) } shouldBe ice { delete.delete(1) }
    }

    test("Create a delete failing data source with dsl") {
        val deleteDSL = deleteDataSource<Int> { raise(DataNotFound) }.toDeleteRepository()
        val delete = DeleteDataSource<Int> { raise(DataNotFound) }.toDeleteRepository()

        ice { deleteDSL.delete(1) } shouldBe ice { delete.delete(1) }
    }
})