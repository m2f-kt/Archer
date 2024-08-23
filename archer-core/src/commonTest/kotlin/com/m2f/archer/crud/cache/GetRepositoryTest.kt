package com.m2f.archer.crud.cache

import com.m2f.archer.crud.getDataSource
import com.m2f.archer.repository.toRepository
import com.m2f.archer.utils.archerTest
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec

class GetRepositoryTest : FunSpec({

    archerTest("Repository without key") {
        val get = getDataSource<Unit, String> { "main" }
        val repository = get.toRepository()

        either { repository.get(Unit) } shouldBeRight "main"
    }
})
