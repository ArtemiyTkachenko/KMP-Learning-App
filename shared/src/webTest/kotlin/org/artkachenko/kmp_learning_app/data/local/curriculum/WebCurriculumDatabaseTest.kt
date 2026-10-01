package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.artkachenko.kmp_learning_app.sqlite_worker.createSQLiteWasmWorker

/**
 * The web database through its real path: Room, `WebWorkerSQLiteDriver`, the packaged SQLite
 * worker, sqlite-wasm and OPFS, in a real browser.
 *
 * Every other database test runs on the JVM's bundled SQLite. The repositories rely on Room
 * transactions being atomic and on foreign keys being enforced, and on the web each statement of
 * such a transaction is a separate message to a worker, so those guarantees are checked here
 * rather than assumed from the JVM.
 */
internal class WebCurriculumDatabaseTest {

    @Test
    fun theProductionBuilderOpensTheCurrentSchemaWithForeignKeysEnforced() = runTest {
        val database = createWebCurriculumDatabase()
        try {
            assertEquals(null, database.curriculumDao().getTopicById("missing"))
            assertFailsWith<SQLiteException> {
                database.withWriteTransaction {
                    executeSQL(
                        "INSERT INTO subtopic (id, topic_id, name, status, sort_order) " +
                            "VALUES ('orphan', 'missing', 'Orphan', 'ACTIVE', 0)",
                    )
                }
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun aFailedWriteTransactionLeavesNothingBehind() = runTest {
        val database = openIsolatedDatabase()
        try {
            assertFailsWith<IllegalStateException> {
                database.withWriteTransaction {
                    insertTopic("written_then_rolled_back")
                    error("fail after the first write")
                }
            }

            assertEquals(0L, database.topicCount())
        } finally {
            database.close()
        }
    }

    @Test
    fun concurrentWriteTransactionsDoNotInterleave() = runTest {
        val database = openIsolatedDatabase()
        try {
            // Each transaction reads the count, suspends, then writes a row named after what it
            // read. Interleaved, both would read 0 and the second insert would collide.
            List(3) {
                async {
                    database.withWriteTransaction {
                        val seen = usePrepared("SELECT COUNT(*) FROM topic") { it.step(); it.getLong(0) }
                        yield()
                        insertTopic("topic_$seen")
                    }
                }
            }.awaitAll()

            assertEquals(3L, database.topicCount())
        } finally {
            database.close()
        }
    }

    private fun openIsolatedDatabase(): CurriculumDatabase =
        Room.databaseBuilder<CurriculumDatabase>(name = "web-test-${Random.nextLong()}.db")
            .setDriver(createSQLiteWasmWorker())
            .addMigrations(*curriculumDatabaseMigrations.toTypedArray())
            .build()

    private suspend fun androidx.room3.PooledConnection.insertTopic(id: String) {
        executeSQL("INSERT INTO topic (id, name, status, sort_order) VALUES ('$id', '$id', 'ACTIVE', 0)")
    }

    private suspend fun CurriculumDatabase.topicCount(): Long =
        withReadTransaction { usePrepared("SELECT COUNT(*) FROM topic") { it.step(); it.getLong(0) } }
}
