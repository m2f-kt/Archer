package com.m2f.archer.crud.cache

import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.utils.archerTest
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec

class OptionTest : FunSpec({

    archerTest("regular call wraps into Some content") {
        val getDataSource = getDataSource<Int, String> { "Success" }
        option { getDataSource.get(0) } shouldBeSome "Success"
    }

    archerTest("failure call wraps into None") {
        val getDataSource = getDataSource<Int, String> { raise(DataNotFound) }
        option { getDataSource.get(0) }.shouldBeNone()
    }
})
