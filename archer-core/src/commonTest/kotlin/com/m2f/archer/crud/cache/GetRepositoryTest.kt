package com.m2f.archer.crud.cache

import com.m2f.archer.crud.either
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.repository.toRepository
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec

class GetRepositoryTest : FunSpec({

    test("Repository without key") {
        val get = getDataSource<Unit, String> { "main" }
        val repository = get.toRepository()

        either { repository.get(Unit) } shouldBeRight "main"
    }
})
