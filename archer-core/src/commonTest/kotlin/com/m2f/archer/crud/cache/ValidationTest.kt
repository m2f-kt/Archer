package com.m2f.archer.crud.cache

import com.m2f.archer.crud.either
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.validate.validate
import com.m2f.archer.failure.Invalid
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec

class ValidationTest : FunSpec({

    test("always valid") {
        val get = getDataSource<Unit, String> { "hello" }
            .validate { true }

        either { get.get(Unit) } shouldBeRight "hello"
    }

    test("always invalid") {
        val get = getDataSource<Unit, String> { "hello" }
            .validate { false }

        either { get.get(Unit) } shouldBeLeft Invalid
    }
})
