package com.m2f.archer.crud.cache

import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.crud.deleteDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.utils.archerTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeleteDataSourceTest : FunSpec({

    archerTest("Create a delete data source with dsl") {
        val deleteDSL = deleteDataSource<Int> { }
        val delete = DeleteDataSource<Int> { }

        ice { deleteDSL.delete(1) } shouldBe ice { delete.delete(1) }
    }

    archerTest("Create a delete failing data source with dsl") {
        val deleteDSL = deleteDataSource<Int> { raise(DataNotFound) }
        val delete = DeleteDataSource<Int> { raise(DataNotFound) }

        ice { deleteDSL.delete(1) } shouldBe ice { delete.delete(1) }
    }
})
