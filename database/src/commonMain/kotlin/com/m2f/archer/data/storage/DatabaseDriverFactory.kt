package com.m2f.archer.data.storage

import app.cash.sqldelight.db.QueryResult.AsyncValue
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

internal const val databaseName = "expiration_registry"
expect class DatabaseDriverFactory {

    companion object {
        suspend fun createDriver(schema: SqlSchema<AsyncValue<Unit>>): SqlDriver
    }
}
