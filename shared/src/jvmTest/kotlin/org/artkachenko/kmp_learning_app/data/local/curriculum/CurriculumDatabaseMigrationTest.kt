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
    fun migrationFromOneToFourPreservesCurriculumAndHistoricalAssessmentRows() = runTest {
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

        helper.runMigrationsAndValidate(
            version = 4,
            migrations = listOf(MIGRATION_2_3, MIGRATION_3_4),
        ).use { connection ->
            connection.prepare("SELECT selection_mode FROM question WHERE id = 'question'").use { statement ->
                assertTrue(statement.step())
                assertEquals("SINGLE", statement.getText(0))
            }
            connection.prepare("SELECT COUNT(*) FROM question_attempt_selected_answer").use { statement ->
                assertTrue(statement.step())
                assertEquals(1, statement.getLong(0).toInt())
            }
        }
    }

    @Test
    fun migrationFromTwoToThreeMarksExistingAnswerOptionsActive() = runTest {
        val databasePath = Files.createTempDirectory("curriculum-migration-test")
            .resolve("curriculum.db")
        val helper = MigrationTestHelper(
            schemaDirectoryPath = Path.of("schemas").toAbsolutePath(),
            databasePath = databasePath,
            driver = BundledSQLiteDriver(),
            databaseClass = CurriculumDatabase::class,
            databaseFactory = { CurriculumDatabaseConstructor.initialize() },
        )

        helper.createDatabase(version = 2).use { connection ->
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
            version = 3,
            migrations = listOf(MIGRATION_2_3),
        ).use { connection ->
            // Every option stored before v3 was authored by the bundled curriculum, so
            // the migration must leave it selectable rather than silently retiring it.
            connection.prepare("SELECT status FROM answer_option WHERE id = 'answer_a'").use { statement ->
                assertTrue(statement.step())
                assertEquals("ACTIVE", statement.getText(0))
            }
        }
    }

    @Test
    fun migrationFromThreeToFourDerivesLegacyModesAndPreservesQuestionData() = runTest {
        val databasePath = Files.createTempDirectory("curriculum-migration-test")
            .resolve("curriculum.db")
        val helper = MigrationTestHelper(
            schemaDirectoryPath = Path.of("schemas").toAbsolutePath(),
            databasePath = databasePath,
            driver = BundledSQLiteDriver(),
            databaseClass = CurriculumDatabase::class,
            databaseFactory = { CurriculumDatabaseConstructor.initialize() },
        )

        helper.createDatabase(version = 3).use { connection ->
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
                VALUES
                    ('single_question', 'topic', 'subtopic', 'Single?', 'Single explanation.', 'ACTIVE', 0),
                    ('multiple_question', 'topic', 'subtopic', 'Multiple?', 'Multiple explanation.', 'DEPRECATED', 1)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO answer_option (question_id, id, text, sort_order, status)
                VALUES
                    ('single_question', 'single_a', 'Single A', 0, 'ACTIVE'),
                    ('multiple_question', 'multiple_a', 'Multiple A', 0, 'ACTIVE'),
                    ('multiple_question', 'multiple_b', 'Multiple B', 1, 'ACTIVE')
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_correct_answer (question_id, answer_id)
                VALUES
                    ('single_question', 'single_a'),
                    ('multiple_question', 'multiple_a'),
                    ('multiple_question', 'multiple_b')
                """,
            )
        }

        helper.runMigrationsAndValidate(
            version = 4,
            migrations = listOf(MIGRATION_3_4),
        ).use { connection ->
            connection.prepare(
                "SELECT id, selection_mode, text, explanation, status, sort_order FROM question ORDER BY sort_order",
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("single_question", statement.getText(0))
                assertEquals("SINGLE", statement.getText(1))
                assertEquals("Single?", statement.getText(2))
                assertEquals("Single explanation.", statement.getText(3))
                assertEquals("ACTIVE", statement.getText(4))
                assertEquals(0, statement.getLong(5).toInt())

                assertTrue(statement.step())
                assertEquals("multiple_question", statement.getText(0))
                assertEquals("MULTIPLE", statement.getText(1))
                assertEquals("Multiple?", statement.getText(2))
                assertEquals("Multiple explanation.", statement.getText(3))
                assertEquals("DEPRECATED", statement.getText(4))
                assertEquals(1, statement.getLong(5).toInt())
            }
            connection.prepare("SELECT COUNT(*) FROM question_correct_answer").use { statement ->
                assertTrue(statement.step())
                assertEquals(3, statement.getLong(0).toInt())
            }
        }
    }
}
