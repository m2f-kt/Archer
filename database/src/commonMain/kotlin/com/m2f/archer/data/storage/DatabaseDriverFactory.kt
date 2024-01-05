package com.m2f.archer.data.storage

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

const val DATABASE_NAME: String = "expiration_registry.db"
expect object DatabaseDriverFactory {

    suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver
}
