package org.artkachenko.kmp_learning_app.data.local.curriculum.importer

import androidx.room3.withWriteTransaction
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
                        dao.deleteAnswerOptionsForQuestionExcept(
                            questionId = questionId,
                            keepAnswerIds = options.map { it.id },
                        )
                    }
            }
        }

        return CurriculumImportResult.Imported
    }
}
