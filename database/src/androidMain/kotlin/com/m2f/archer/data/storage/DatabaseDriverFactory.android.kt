package com.m2f.archer.data.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.m2f.archer.sqldelight.CacheExpirationDatabase
import com.m2f.archer.startup.applicationContext

actual class DatabaseDriverFactory {
    actual companion object {
        actual fun createDriver(databaseName: String): SqlDriver =
            AndroidSqliteDriver(CacheExpirationDatabase.Schema, applicationContext, "${databaseName}.db")
    }
}