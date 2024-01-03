package com.m2f.archer.data.storage

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {

    companion object {
        fun createDriver(databaseName: String): SqlDriver
    }
}