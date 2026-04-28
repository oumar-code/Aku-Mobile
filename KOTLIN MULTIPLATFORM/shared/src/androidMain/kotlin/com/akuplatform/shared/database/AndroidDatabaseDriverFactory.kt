package com.akuplatform.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of [DatabaseDriverFactory].
 *
 * Creates an [AndroidSqliteDriver] backed by the on-device SQLite database named [DB_NAME].
 */
class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver =
        AndroidSqliteDriver(AkuDatabase.Schema, context, DB_NAME)

    companion object {
        const val DB_NAME = "aku_db"
    }
}
