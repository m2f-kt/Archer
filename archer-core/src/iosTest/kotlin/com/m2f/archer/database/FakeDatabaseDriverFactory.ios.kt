package com.m2f.archer.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult.AsyncValue
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration

@Suppress("UtilityClassWithPublicConstructor")
internal actual object FakeDatabaseDriverFactory {
    actual suspend fun createDriver(schema: SqlSchema<AsyncValue<Unit>>): SqlDriver =
        NativeSqliteDriver(
            DatabaseConfiguration(
                name = "test.db",
                version = 1,
                inMemory = true,
                create = { connection ->
                    wrapConnection(connection) { schema.synchronous().create(it) }
                },
            ),
        )
}