package com.m2f.archer.database

import app.cash.sqldelight.db.QueryResult.AsyncValue
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

@Suppress("UtilityClassWithPublicConstructor")
internal expect object FakeDatabaseDriverFactory {
    suspend fun createDriver(schema: SqlSchema<AsyncValue<Unit>>): SqlDriver
}