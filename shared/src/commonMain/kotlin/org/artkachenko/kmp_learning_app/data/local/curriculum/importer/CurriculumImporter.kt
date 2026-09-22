package org.artkachenko.kmp_learning_app.data.local.curriculum.importer

import androidx.room3.executeSQL
import androidx.room3.withWriteTransaction
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.validation.CurriculumValidator
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase

internal class CurriculumImporter(
    private val database: CurriculumDatabase,
    private val loadCurriculum: suspend () -> Curriculum = {
        BundledCurriculumSource.load()
    },
    private val validator: CurriculumValidator = CurriculumValidator(),
) {
    suspend fun importCurriculum(): CurriculumImportResult {
        val curriculum = loadCurriculum()
        val validationErrors = validator.validate(curriculum)
        if (validationErrors.isNotEmpty()) {
            return CurriculumImportResult.Rejected(validationErrors)
        }

        val snapshot = curriculum.toPersistenceSnapshot()
        val incomingQuestionIds = snapshot.questions.map { it.id }
        val dao = database.curriculumDao()

        database.withWriteTransaction {
            // Foreign keys are enforced per statement by default, but this transaction
            // rewrites a whole content graph, so intermediate rows legitimately disagree
            // with each other. Re-homing a Subtopic to another Topic is the case that
            // proves it: updating subtopic.topic_id orphans the question rows that still
            // name the old pair, even though the same bundle moves those questions in the
            // next statement. Deferring to COMMIT checks the finished graph instead of
            // each step of it, and SQLite resets the pragma at every COMMIT or ROLLBACK.
            executeSQL("PRAGMA defer_foreign_keys = ON")

            dao.upsertTopics(snapshot.topics)
            dao.upsertSubtopics(snapshot.subtopics)
            dao.upsertQuestions(snapshot.questions)
            dao.upsertAnswerOptions(snapshot.answerOptions)

            if (incomingQuestionIds.isNotEmpty()) {
                dao.deleteCorrectAnswersForQuestions(incomingQuestionIds)
                dao.upsertCorrectAnswers(snapshot.correctAnswers)

                dao.deleteQuestionSourcesForQuestions(incomingQuestionIds)
                dao.upsertQuestionSources(snapshot.sources)

                // Stale answer options are removed last: question_correct_answer also has a
                // foreign key onto answer_option(question_id, id), so an option that was
                // correct in the previous bundle can only be deleted once its correct-answer
                // rows have been replaced above. Grouping the just-written options keeps each
                // keep-list non-empty and exactly matching what was persisted.
                snapshot.answerOptions
                    .groupBy { it.questionId }
                    .forEach { (questionId, options) ->
                        val keepAnswerIds = options.map { it.id }
                        dao.deleteAnswerOptionsForQuestionExcept(
                            questionId = questionId,
                            keepAnswerIds = keepAnswerIds,
                        )
                        // Whatever survived the delete is referenced by a historical
                        // attempt. Retire it so it stays reviewable without being offered
                        // as an extra choice in new assessments.
                        dao.deprecateAnswerOptionsForQuestionExcept(
                            questionId = questionId,
                            keepAnswerIds = keepAnswerIds,
                            deprecatedStatus = ContentStatus.DEPRECATED.name,
                        )
                    }
            }
        }

        return CurriculumImportResult.Imported
    }
}
