package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * E15-05 compatibility verification for authored `QuestionLevel` metadata.
 *
 * These tests deliberately cross boundaries that the per-layer E15-02 and E15-04 tests do
 * not: the real bundled curriculum runs through decode, validation, import, Room, and the
 * repository, and a legacy on-disk database runs through migration followed by a current
 * bundled import. Level behaviour within one layer is already covered by
 * `CurriculumImporterTest`, `LocalCurriculumRepositoryTest`, and
 * `CurriculumDatabaseMigrationTest`, so it is not repeated here.
 */
internal class QuestionLevelEndToEndTest {
    @Test
    fun freshImportPersistsEveryAuthoredLevelFromTheBundledCurriculum() = runTest {
        val bundled = BundledCurriculumSource.load()

        withInMemoryDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database).importCurriculum(),
            )

            val repository = LocalCurriculumRepository(database)
            val expected = bundled.eligibleActiveQuestions()
            val actual = repository.getActiveQuestions()

            assertEquals(expected.map { it.id }, actual.map { it.id })
            assertEquals(expected.map { it.level }, actual.map { it.level })
            assertEquals(
                QuestionLevel.entries.toSet(),
                actual.map { it.level }.toSet(),
                "A fresh import must persist all three authored levels",
            )

            // Retired bundled Questions are imported too, so their authored level has to
            // survive the same path even though they are never selectable.
            val deprecated = bundled.questions.filter { it.status == ContentStatus.DEPRECATED }
            assertTrue(deprecated.isNotEmpty())
            deprecated.forEach { authored ->
                assertEquals(
                    authored.level,
                    repository.getQuestionById(authored.id)?.level,
                    authored.id,
                )
            }
        }
    }

    @Test
    fun levelFilteredRepositoryQueriesMatchTheBundledCurriculum() = runTest {
        val bundled = BundledCurriculumSource.load()

        withInMemoryDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database).importCurriculum(),
            )

            val repository = LocalCurriculumRepository(database)
            val eligible = bundled.eligibleActiveQuestions()

            QuestionLevel.entries.forEach { level ->
                val expected = eligible.filter { it.level == level }.map { it.id }
                assertTrue(expected.isNotEmpty(), "No bundled Question is authored $level")
                assertEquals(
                    expected,
                    repository.getActiveQuestionsByLevels(setOf(level)).map { it.id },
                    level.name,
                )
            }

            val orLevels = setOf(QuestionLevel.APPLIED, QuestionLevel.ADVANCED)
            assertEquals(
                eligible.filter { it.level in orLevels }.map { it.id },
                repository.getActiveQuestionsByLevels(orLevels).map { it.id },
            )
            assertEquals(
                repository.getActiveQuestions().map { it.id },
                repository.getActiveQuestionsByLevels(QuestionLevel.entries.toSet()).map { it.id },
            )

            // Scope the combined filters onto real content that actually spans levels.
            val advanced = eligible.first { it.level == QuestionLevel.ADVANCED }
            val topicExpected = eligible
                .filter { it.topicId == advanced.topicId && it.level in orLevels }
                .map { it.id }
            assertTrue(topicExpected.size > 1)
            assertEquals(
                topicExpected,
                repository.getActiveQuestionsByTopicAndLevels(advanced.topicId, orLevels)
                    .map { it.id },
            )
            val subtopicExpected = eligible
                .filter { it.subtopicId == advanced.subtopicId && it.level == QuestionLevel.ADVANCED }
                .map { it.id }
            assertEquals(
                subtopicExpected,
                repository.getActiveQuestionsBySubtopicAndLevels(
                    subtopicId = advanced.subtopicId,
                    levels = setOf(QuestionLevel.ADVANCED),
                ).map { it.id },
            )

            assertEquals(emptyList(), repository.getActiveQuestionsByLevels(emptySet()))
            assertEquals(
                emptyList(),
                repository.getActiveQuestionsByTopicAndLevels(advanced.topicId, emptySet()),
            )
            assertEquals(
                emptyList(),
                repository.getActiveQuestionsBySubtopicAndLevels(advanced.subtopicId, emptySet()),
            )

            // Historical resolution stays outside ACTIVE and level eligibility.
            val retired = bundled.questions.first { it.status == ContentStatus.DEPRECATED }
            val resolved = repository.getQuestionById(retired.id)
            assertNotNull(resolved)
            assertEquals(ContentStatus.DEPRECATED, resolved.status)
            assertEquals(retired.level, resolved.level)
            assertTrue(
                repository.getActiveQuestionsByLevels(QuestionLevel.entries.toSet())
                    .none { it.id == retired.id },
            )
        }
    }

    @Test
    fun reimportRestoresTheAuthoredLevelWhenThePersistedValueDiverges() = runTest {
        val bundled = BundledCurriculumSource.load()
        val advanced = bundled.eligibleActiveQuestions().first { it.level == QuestionLevel.ADVANCED }

        withInMemoryDatabase { database ->
            val importer = CurriculumImporter(database)
            assertEquals(CurriculumImportResult.Imported, importer.importCurriculum())

            val dao = database.curriculumDao()
            val authoredRow = assertNotNull(dao.getQuestionById(advanced.id))
            val beforeCounts = dao.countQuestions()

            // Simulate a database whose stored level no longer agrees with the bundle,
            // which is what a migrated or older install looks like.
            dao.upsertQuestions(listOf(authoredRow.copy(level = QuestionLevel.FOUNDATION.name)))
            assertEquals(QuestionLevel.FOUNDATION.name, dao.getQuestionById(advanced.id)?.level)

            assertEquals(CurriculumImportResult.Imported, importer.importCurriculum())

            // Comparing the whole row proves the authored level wins without the re-import
            // touching identity, text, mode, or lifecycle status.
            assertEquals(authoredRow, dao.getQuestionById(advanced.id))
            assertEquals(beforeCounts, dao.countQuestions())
            assertEquals(
                QuestionLevel.ADVANCED,
                LocalCurriculumRepository(database).getQuestionById(advanced.id)?.level,
            )
        }
    }

    @Test
    fun migratedLegacyDatabaseAdoptsAuthoredLevelsFromTheNextBundledImport() = runTest {
        val bundled = BundledCurriculumSource.load()
        val advanced = bundled.eligibleActiveQuestions().first { it.level == QuestionLevel.ADVANCED }
        val databasePath = Files.createTempDirectory("curriculum-level-migration")
            .resolve("curriculum.db")
        val helper = MigrationTestHelper(
            schemaDirectoryPath = Path.of("schemas").toAbsolutePath(),
            databasePath = databasePath,
            driver = BundledSQLiteDriver(),
            databaseClass = CurriculumDatabase::class,
            databaseFactory = { CurriculumDatabaseConstructor.initialize() },
        )

        // A version 4 install: one Question the current bundle still authors, and one it
        // has since stopped authoring but a completed attempt still references.
        helper.createDatabase(version = 4).use { connection ->
            connection.executeSQL(
                "INSERT INTO topic (id, name, status, sort_order) VALUES ('${advanced.topicId}', 'Legacy topic', 'ACTIVE', 0)",
            )
            connection.executeSQL(
                """
                INSERT INTO subtopic (id, topic_id, name, status, sort_order)
                VALUES ('${advanced.subtopicId}', '${advanced.topicId}', 'Legacy subtopic', 'ACTIVE', 0)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question (
                    id, topic_id, subtopic_id, text, selection_mode, explanation, status, sort_order
                ) VALUES
                    ('${advanced.id}', '${advanced.topicId}', '${advanced.subtopicId}', 'Legacy text?', 'SINGLE', 'Legacy explanation.', 'ACTIVE', 0),
                    ('$LegacyOnlyQuestionId', '${advanced.topicId}', '${advanced.subtopicId}', 'Retired text?', 'SINGLE', 'Retired explanation.', 'ACTIVE', 1)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO answer_option (question_id, id, text, sort_order, status)
                VALUES
                    ('${advanced.id}', 'legacy_answer_a', 'Legacy answer A', 0, 'ACTIVE'),
                    ('${advanced.id}', 'legacy_answer_b', 'Legacy answer B', 1, 'ACTIVE'),
                    ('$LegacyOnlyQuestionId', 'legacy_only_answer_a', 'Retired answer A', 0, 'ACTIVE'),
                    ('$LegacyOnlyQuestionId', 'legacy_only_answer_b', 'Retired answer B', 1, 'ACTIVE')
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_correct_answer (question_id, answer_id)
                VALUES
                    ('${advanced.id}', 'legacy_answer_a'),
                    ('$LegacyOnlyQuestionId', 'legacy_only_answer_a')
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO test_attempt (
                    id, config_type, requested_question_count, scope_type, scope_id, status,
                    score_total_questions, score_correct_answers, started_at_epoch_millis,
                    completed_at_epoch_millis
                ) VALUES ('$LegacyAttemptId', 'MIXED', 1, NULL, NULL, 'COMPLETED', 1, 1, 1000, 2000)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_attempt (test_attempt_id, question_id, sort_order, is_correct)
                VALUES ('$LegacyAttemptId', '$LegacyOnlyQuestionId', 0, 1)
                """,
            )
            connection.executeSQL(
                """
                INSERT INTO question_attempt_selected_answer (test_attempt_id, question_id, answer_id)
                VALUES ('$LegacyAttemptId', '$LegacyOnlyQuestionId', 'legacy_only_answer_a')
                """,
            )
        }

        helper.runMigrationsAndValidate(
            version = 5,
            migrations = listOf(MIGRATION_4_5),
        ).use { connection ->
            connection.prepare("SELECT id, level FROM question ORDER BY sort_order").use { statement ->
                assertTrue(statement.step())
                assertEquals(advanced.id, statement.getText(0))
                assertEquals(QuestionLevel.FOUNDATION.name, statement.getText(1))
                assertTrue(statement.step())
                assertEquals(LegacyOnlyQuestionId, statement.getText(0))
                assertEquals(QuestionLevel.FOUNDATION.name, statement.getText(1))
            }
        }

        val database = Room.databaseBuilder<CurriculumDatabase>(name = databasePath.toString())
            .setDriver(BundledSQLiteDriver())
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
            )
            .build()
        try {
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database).importCurriculum(),
            )

            val repository = LocalCurriculumRepository(database)
            val persistedLevels = repository.getActiveQuestions().associate { it.id to it.level }

            // Every bundled Question carries its current authored level, so the deterministic
            // migration value cannot have leaked into the imported bank.
            assertEquals(
                bundled.eligibleActiveQuestions().associate { it.id to it.level },
                persistedLevels - LegacyOnlyQuestionId,
            )
            // The retained historical Question keeps the legacy value E15-02 assigned it.
            assertEquals(QuestionLevel.FOUNDATION, persistedLevels[LegacyOnlyQuestionId])

            val reimported = repository.getQuestionById(advanced.id)
            assertNotNull(reimported)
            assertEquals(QuestionLevel.ADVANCED, reimported.level)
            assertEquals(advanced.text, reimported.text)
            assertEquals(advanced.correctAnswerIds.sorted(), reimported.correctAnswerIds.sorted())

            val attempt = AssessmentAttemptStore(database).getById(LegacyAttemptId)
            assertNotNull(attempt)
            assertEquals(
                listOf(LegacyOnlyQuestionId),
                attempt.questionAttempts.map { it.questionId },
            )
        } finally {
            database.close()
        }
    }

    private suspend fun withInMemoryDatabase(
        block: suspend (CurriculumDatabase) -> Unit,
    ) {
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    /**
     * The bundled Questions the repository is expected to treat as eligible, in import
     * order: ACTIVE Questions under an ACTIVE Subtopic and an ACTIVE Topic.
     */
    private fun Curriculum.eligibleActiveQuestions(): List<Question> {
        val activeTopicIds = topics.filter { it.status == ContentStatus.ACTIVE }.map { it.id }.toSet()
        val activeSubtopicIds = subtopics.filter { it.status == ContentStatus.ACTIVE }.map { it.id }.toSet()
        return questions.filter {
            it.status == ContentStatus.ACTIVE &&
                it.topicId in activeTopicIds &&
                it.subtopicId in activeSubtopicIds
        }
    }

    private companion object {
        const val LegacyOnlyQuestionId = "legacy_only_question"
        const val LegacyAttemptId = "legacy_attempt"
    }
}
