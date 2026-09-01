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
    fun migrationFromOneToSixPreservesCurriculumAndHistoricalAssessmentRows() = runTest {
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
            version = 6,
            migrations = listOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6),
        ).use { connection ->
            connection.prepare("SELECT selection_mode, level FROM question WHERE id = 'question'").use { statement ->
                assertTrue(statement.step())
                assertEquals("SINGLE", statement.getText(0))
                assertEquals("FOUNDATION", statement.getText(1))
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

    @Test
    fun migrationFromFourToFiveAddsFoundationLevelWithoutChangingCurriculumOrHistory() = runTest {
        val databasePath = Files.createTempDirectory("curriculum-migration-test")
            .resolve("curriculum.db")
        val helper = MigrationTestHelper(
            schemaDirectoryPath = Path.of("schemas").toAbsolutePath(),
            databasePath = databasePath,
            driver = BundledSQLiteDriver(),
            databaseClass = CurriculumDatabase::class,
            databaseFactory = { CurriculumDatabaseConstructor.initialize() },
        )

        helper.createDatabase(version = 4).use { connection ->
            connection.executeSQL("INSERT INTO topic (id, name, status, sort_order) VALUES ('topic', 'Topic', 'ACTIVE', 0)")
            connection.executeSQL(
                """
                INSERT INTO subtopic (id, topic_id, name, status, sort_order)
                VALUES ('subtopic', 'topic', 'Subtopic', 'ACTIVE', 0)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question (
                    id, topic_id, subtopic_id, text, selection_mode, explanation, status, sort_order
                ) VALUES
                    ('active_question', 'topic', 'subtopic', 'Active?', 'SINGLE', 'Active explanation.', 'ACTIVE', 0),
                    ('deprecated_question', 'topic', 'subtopic', 'Deprecated?', 'MULTIPLE', 'Deprecated explanation.', 'DEPRECATED', 1)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO answer_option (question_id, id, text, sort_order, status)
                VALUES
                    ('active_question', 'active_a', 'Active A', 0, 'ACTIVE'),
                    ('deprecated_question', 'deprecated_a', 'Deprecated A', 0, 'ACTIVE'),
                    ('deprecated_question', 'deprecated_b', 'Deprecated B', 1, 'DEPRECATED')
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_correct_answer (question_id, answer_id)
                VALUES
                    ('active_question', 'active_a'),
                    ('deprecated_question', 'deprecated_a')
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_source (question_id, url, title, sort_order)
                VALUES ('deprecated_question', 'https://example.com/source', 'Source', 0)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO test_attempt (
                    id, config_type, requested_question_count, scope_type, scope_id, status,
                    score_total_questions, score_correct_answers, started_at_epoch_millis,
                    completed_at_epoch_millis
                ) VALUES ('attempt', 'MIXED', 1, NULL, NULL, 'COMPLETED', 1, 0, 1000, 2000)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_attempt (test_attempt_id, question_id, sort_order, is_correct)
                VALUES ('attempt', 'deprecated_question', 0, 0)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_attempt_selected_answer (test_attempt_id, question_id, answer_id)
                VALUES ('attempt', 'deprecated_question', 'deprecated_b')
                """,
            )
        }

        helper.runMigrationsAndValidate(
            version = 5,
            migrations = listOf(MIGRATION_4_5),
        ).use { connection ->
            connection.prepare(
                "SELECT id, level, text, selection_mode, explanation, status, sort_order FROM question ORDER BY sort_order",
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("active_question", statement.getText(0))
                assertEquals("FOUNDATION", statement.getText(1))
                assertEquals("Active?", statement.getText(2))
                assertEquals("SINGLE", statement.getText(3))
                assertEquals("Active explanation.", statement.getText(4))
                assertEquals("ACTIVE", statement.getText(5))
                assertEquals(0, statement.getLong(6).toInt())

                assertTrue(statement.step())
                assertEquals("deprecated_question", statement.getText(0))
                assertEquals("FOUNDATION", statement.getText(1))
                assertEquals("Deprecated?", statement.getText(2))
                assertEquals("MULTIPLE", statement.getText(3))
                assertEquals("Deprecated explanation.", statement.getText(4))
                assertEquals("DEPRECATED", statement.getText(5))
                assertEquals(1, statement.getLong(6).toInt())
            }
            connection.prepare(
                "SELECT question_id, id, text, status, sort_order FROM answer_option ORDER BY question_id, sort_order",
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("active_question", statement.getText(0))
                assertEquals("active_a", statement.getText(1))
                assertEquals("Active A", statement.getText(2))
                assertEquals("ACTIVE", statement.getText(3))
                assertEquals(0, statement.getLong(4).toInt())

                assertTrue(statement.step())
                assertEquals("deprecated_question", statement.getText(0))
                assertEquals("deprecated_a", statement.getText(1))
                assertEquals("Deprecated A", statement.getText(2))
                assertEquals("ACTIVE", statement.getText(3))
                assertEquals(0, statement.getLong(4).toInt())

                assertTrue(statement.step())
                assertEquals("deprecated_question", statement.getText(0))
                assertEquals("deprecated_b", statement.getText(1))
                assertEquals("Deprecated B", statement.getText(2))
                assertEquals("DEPRECATED", statement.getText(3))
                assertEquals(1, statement.getLong(4).toInt())
            }
            connection.prepare(
                "SELECT question_id, answer_id FROM question_correct_answer ORDER BY question_id",
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("active_question", statement.getText(0))
                assertEquals("active_a", statement.getText(1))
                assertTrue(statement.step())
                assertEquals("deprecated_question", statement.getText(0))
                assertEquals("deprecated_a", statement.getText(1))
            }
            connection.prepare("SELECT url, title, sort_order FROM question_source WHERE question_id = 'deprecated_question'").use { statement ->
                assertTrue(statement.step())
                assertEquals("https://example.com/source", statement.getText(0))
                assertEquals("Source", statement.getText(1))
                assertEquals(0, statement.getLong(2).toInt())
            }
            connection.prepare("SELECT status, score_total_questions, score_correct_answers FROM test_attempt WHERE id = 'attempt'").use { statement ->
                assertTrue(statement.step())
                assertEquals("COMPLETED", statement.getText(0))
                assertEquals(1, statement.getLong(1).toInt())
                assertEquals(0, statement.getLong(2).toInt())
            }
            connection.prepare("SELECT is_correct FROM question_attempt WHERE question_id = 'deprecated_question'").use { statement ->
                assertTrue(statement.step())
                assertEquals(0, statement.getLong(0).toInt())
            }
            connection.prepare("SELECT answer_id FROM question_attempt_selected_answer").use { statement ->
                assertTrue(statement.step())
                assertEquals("deprecated_b", statement.getText(0))
            }
            connection.prepare("PRAGMA foreign_key_check").use { statement ->
                assertTrue(!statement.step())
            }
        }
    }

    /**
     * The Practice Builder can narrow a run by level and source, so those two dimensions became
     * part of the attempt record. Existing rows must survive the change without being given a
     * selection they were never offered: a null column is what the mapper reads back as the
     * historical all-levels ALL request.
     */
    @Test
    fun migrationFromFiveToSixLeavesHistoricalAttemptsWithoutAPracticeSelection() = runTest {
        val databasePath = Files.createTempDirectory("curriculum-migration-test")
            .resolve("curriculum.db")
        val helper = MigrationTestHelper(
            schemaDirectoryPath = Path.of("schemas").toAbsolutePath(),
            databasePath = databasePath,
            driver = BundledSQLiteDriver(),
            databaseClass = CurriculumDatabase::class,
            databaseFactory = { CurriculumDatabaseConstructor.initialize() },
        )

        helper.createDatabase(version = 5).use { connection ->
            connection.executeSQL("INSERT INTO topic (id, name, status, sort_order) VALUES ('topic', 'Topic', 'ACTIVE', 0)")
            connection.executeSQL(
                """
                INSERT INTO subtopic (id, topic_id, name, status, sort_order)
                VALUES ('subtopic', 'topic', 'Subtopic', 'ACTIVE', 0)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question (
                    id, topic_id, subtopic_id, text, selection_mode, level, explanation, status, sort_order
                ) VALUES ('question', 'topic', 'subtopic', 'Question?', 'SINGLE', 'ADVANCED', 'Explanation.', 'ACTIVE', 0)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO answer_option (question_id, id, text, sort_order, status)
                VALUES ('question', 'answer_a', 'Answer A', 0, 'ACTIVE')
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO test_attempt (
                    id, config_type, requested_question_count, scope_type, scope_id, status,
                    score_total_questions, score_correct_answers, started_at_epoch_millis,
                    completed_at_epoch_millis
                ) VALUES
                    ('focused_attempt', 'FOCUSED', 10, 'TOPIC', 'topic', 'COMPLETED', 1, 1, 1000, 2000),
                    ('mixed_attempt', 'MIXED', 20, NULL, NULL, 'COMPLETED', 1, 0, 3000, 4000)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_attempt (test_attempt_id, question_id, sort_order, is_correct)
                VALUES ('focused_attempt', 'question', 0, 1)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_attempt_selected_answer (test_attempt_id, question_id, answer_id)
                VALUES ('focused_attempt', 'question', 'answer_a')
                """,
            )
        }

        helper.runMigrationsAndValidate(
            version = 6,
            migrations = listOf(MIGRATION_5_6),
        ).use { connection ->
            connection.prepare(
                """
                SELECT id, practice_levels, practice_source, config_type, requested_question_count,
                       scope_type, scope_id, status, score_correct_answers
                FROM test_attempt ORDER BY id
                """,
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("focused_attempt", statement.getText(0))
                assertTrue(statement.isNull(1))
                assertTrue(statement.isNull(2))
                assertEquals("FOCUSED", statement.getText(3))
                assertEquals(10, statement.getLong(4).toInt())
                assertEquals("TOPIC", statement.getText(5))
                assertEquals("topic", statement.getText(6))
                assertEquals("COMPLETED", statement.getText(7))
                assertEquals(1, statement.getLong(8).toInt())

                assertTrue(statement.step())
                assertEquals("mixed_attempt", statement.getText(0))
                assertTrue(statement.isNull(1))
                assertTrue(statement.isNull(2))
                assertEquals("MIXED", statement.getText(3))
                assertEquals(20, statement.getLong(4).toInt())
            }
            connection.prepare("SELECT answer_id FROM question_attempt_selected_answer").use { statement ->
                assertTrue(statement.step())
                assertEquals("answer_a", statement.getText(0))
            }
            connection.prepare("SELECT level FROM question WHERE id = 'question'").use { statement ->
                assertTrue(statement.step())
                assertEquals("ADVANCED", statement.getText(0))
            }
            connection.prepare("PRAGMA foreign_key_check").use { statement ->
                assertTrue(!statement.step())
            }
        }
    }
}
