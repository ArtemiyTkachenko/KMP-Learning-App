package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

internal class CurriculumDatabaseMigrationTest {
    @Test
    fun migrationFromOneToTwoPreservesCurriculumAndCreatesAssessmentTables() = runTest {
        val databasePath = Files.createTempDirectory("curriculum-migration-test")
            .resolve("curriculum.db")
        val helper = MigrationTestHelper(
            schemaDirectoryPath = Path.of("schemas").toAbsolutePath(),
            databasePath = databasePath,
            driver = BundledSQLiteDriver(),
            databaseClass = CurriculumDatabase::class,
            databaseFactory = { CurriculumDatabaseConstructor.initialize() },
        )

        helper.createDatabase(version = 1).use { connection ->
            connection.executeSQL("INSERT INTO topic (id, name, status, sort_order) VALUES ('topic', 'Topic', 'ACTIVE', 0)")
            connection.executeSQL(
                """
                INSERT INTO subtopic (id, topic_id, name, status, sort_order)
                VALUES ('subtopic', 'topic', 'Subtopic', 'ACTIVE', 0)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question (id, topic_id, subtopic_id, text, explanation, status, sort_order)
                VALUES ('question', 'topic', 'subtopic', 'Question?', 'Explanation.', 'ACTIVE', 0)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO answer_option (question_id, id, text, sort_order)
                VALUES ('question', 'answer_a', 'Answer A', 0)
                """,
            )
        }

        helper.runMigrationsAndValidate(
            version = 2,
            migrations = listOf(MIGRATION_1_2),
        ).use { connection ->
            connection.prepare("SELECT name FROM topic WHERE id = 'topic'").use { statement ->
                assertTrue(statement.step())
                assertEquals("Topic", statement.getText(0))
            }
            connection.executeSQL(
                """
                INSERT INTO test_attempt (
                    id,
                    config_type,
                    requested_question_count,
                    scope_type,
                    scope_id,
                    status,
                    score_total_questions,
                    score_correct_answers,
                    started_at_epoch_millis,
                    completed_at_epoch_millis
                ) VALUES (
                    'attempt',
                    'MIXED',
                    1,
                    NULL,
                    NULL,
                    'IN_PROGRESS',
                    NULL,
                    NULL,
                    1700000000000,
                    NULL
                )
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_attempt (test_attempt_id, question_id, sort_order, is_correct)
                VALUES ('attempt', 'question', 0, 1)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_attempt_selected_answer (test_attempt_id, question_id, answer_id)
                VALUES ('attempt', 'question', 'answer_a')
                """,
            )
            connection.prepare("SELECT COUNT(*) FROM question_attempt_selected_answer").use { statement ->
                assertTrue(statement.step())
                assertEquals(1, statement.getLong(0).toInt())
            }
        }
    }
}
