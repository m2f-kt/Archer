package com.m2f.archer.crud.cache

import com.m2f.archer.crud.getDataSource
import com.m2f.archer.repository.toRepository
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class GetRepositoryTest {

    @Test
    fun `Repository without key`() = runArcherTest {
        val get = getDataSource<Unit, String> { "main" }
        val repository = get.toRepository()

        val result = either { repository.get(Unit) }
        result shouldBeRight "main"
    }
}