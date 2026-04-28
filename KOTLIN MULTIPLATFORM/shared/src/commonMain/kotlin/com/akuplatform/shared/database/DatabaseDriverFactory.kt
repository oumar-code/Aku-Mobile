package com.akuplatform.shared.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific factory that produces a [SqlDriver] for the local SQLDelight database.
 *
 * Implementations:
 * - Android: [AndroidDatabaseDriverFactory] using [app.cash.sqldelight.driver.android.AndroidSqliteDriver]
 * - iOS: [IosDatabaseDriverFactory] using [app.cash.sqldelight.driver.native.NativeSqliteDriver]
 * - Tests: supply an in-memory [app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver]
 */
interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
