package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionCorrectAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionSourceEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.SubtopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.TopicEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

internal class CurriculumDatabaseTest {
    @Test
    fun representativeGraphCanBePersistedAndReadBack() = runTest {
        withTestDatabase { database ->
            val dao = database.curriculumDao()

            insertRepresentativeGraph(dao)

            assertEquals(
                TopicEntity(id = "kotlin_language", name = "Kotlin Language", status = "ACTIVE", sortOrder = 0),
                dao.getTopicById("kotlin_language"),
            )
            assertEquals(
                SubtopicEntity(
                    id = "kotlin_generics",
                    topicId = "kotlin_language",
                    name = "Generics and variance",
                    status = "ACTIVE",
                    sortOrder = 0,
                ),
                dao.getSubtopicById("kotlin_generics"),
            )
            assertEquals(
                QuestionEntity(
                    id = "kotlin_generics_001",
                    topicId = "kotlin_language",
                    subtopicId = "kotlin_generics",
                    text = "Which declarations are valid? Select all that apply.",
                    explanation = "Generic variance controls how parameterized types relate to each other.",
                    status = "ACTIVE",
                    sortOrder = 0,
                ),
                dao.getQuestionById("kotlin_generics_001"),
            )
            assertEquals(
                listOf(
                    AnswerOptionEntity(
                        questionId = "kotlin_generics_001",
                        id = "kotlin_generics_001_a",
                        text = "Use out when a type parameter is only produced.",
                        sortOrder = 0,
                    ),
                    AnswerOptionEntity(
                        questionId = "kotlin_generics_001",
                        id = "kotlin_generics_001_b",
                        text = "Use in when a type parameter is only consumed.",
                        sortOrder = 1,
                    ),
                    AnswerOptionEntity(
                        questionId = "kotlin_generics_001",
                        id = "kotlin_generics_001_c",
                        text = "Variance has no effect on assignability.",
                        sortOrder = 2,
                    ),
                ),
                dao.getAnswerOptionsForQuestion("kotlin_generics_001"),
            )
            assertEquals(
                listOf("kotlin_generics_001_a", "kotlin_generics_001_b"),
                dao.getCorrectAnswerIdsForQuestion("kotlin_generics_001"),
            )
            assertEquals(
                listOf(
                    QuestionSourceEntity(
                        questionId = "kotlin_generics_001",
                        url = "https://kotlinlang.org/docs/generics.html",
                        title = "Generics: In, Out, Where",
                        sortOrder = 0,
                    ),
                    QuestionSourceEntity(
                        questionId = "kotlin_generics_001",
                        url = "https://kotlinlang.org/docs/classes.html",
                        title = "Classes",
                        sortOrder = 1,
                    ),
                ),
                dao.getSourcesForQuestion("kotlin_generics_001"),
            )
        }
    }

    @Test
    fun answerOptionsAndSourcesAreReadInSortOrder() = runTest {
        withTestDatabase { database ->
            val dao = database.curriculumDao()

            dao.upsertTopics(listOf(TopicEntity("topic", "Topic", "ACTIVE", sortOrder = 0)))
            dao.upsertSubtopics(listOf(SubtopicEntity("subtopic", "topic", "Subtopic", "ACTIVE", sortOrder = 0)))
            dao.upsertQuestions(
                listOf(
                    QuestionEntity(
                        id = "question",
                        topicId = "topic",
                        subtopicId = "subtopic",
                        text = "Question?",
                        explanation = "Explanation.",
                        status = "ACTIVE",
                        sortOrder = 0,
                    ),
                ),
            )
            dao.upsertAnswerOptions(
                listOf(
                    AnswerOptionEntity("question", "answer_c", "Third", sortOrder = 2),
                    AnswerOptionEntity("question", "answer_a", "First", sortOrder = 0),
                    AnswerOptionEntity("question", "answer_b", "Second", sortOrder = 1),
                ),
            )
            dao.upsertQuestionSources(
                listOf(
                    QuestionSourceEntity("question", "https://example.com/second", "Second", sortOrder = 1),
                    QuestionSourceEntity("question", "https://example.com/first", "First", sortOrder = 0),
                ),
            )

            assertEquals(
                listOf("answer_a", "answer_b", "answer_c"),
                dao.getAnswerOptionsForQuestion("question").map { it.id },
            )
            assertEquals(
                listOf("https://example.com/first", "https://example.com/second"),
                dao.getSourcesForQuestion("question").map { it.url },
            )
        }
    }

    @Test
    fun questionRejectsMismatchedTopicAndSubtopicPair() = runTest {
        withTestDatabase { database ->
            val dao = database.curriculumDao()

            dao.upsertTopics(
                listOf(
                    TopicEntity("android_ui", "Android UI", "ACTIVE", sortOrder = 0),
                    TopicEntity("kotlin_language", "Kotlin Language", "ACTIVE", sortOrder = 1),
                ),
            )
            dao.upsertSubtopics(
                listOf(
                    SubtopicEntity(
                        id = "kotlin_generics",
                        topicId = "kotlin_language",
                        name = "Generics and variance",
                        status = "ACTIVE",
                        sortOrder = 0,
                    ),
                ),
            )

            assertFails {
                dao.upsertQuestions(
                    listOf(
                        QuestionEntity(
                            id = "invalid_question",
                            topicId = "android_ui",
                            subtopicId = "kotlin_generics",
                            text = "Invalid hierarchy?",
                            explanation = "This should not persist.",
                            status = "ACTIVE",
                            sortOrder = 0,
                        ),
                    ),
                )
            }
        }
    }

    @Test
    fun correctAnswerRejectsAnswerFromDifferentQuestion() = runTest {
        withTestDatabase { database ->
            val dao = database.curriculumDao()

            dao.upsertTopics(listOf(TopicEntity("topic", "Topic", "ACTIVE", sortOrder = 0)))
            dao.upsertSubtopics(listOf(SubtopicEntity("subtopic", "topic", "Subtopic", "ACTIVE", sortOrder = 0)))
            dao.upsertQuestions(
                listOf(
                    QuestionEntity("question_a", "topic", "subtopic", "Question A?", "Explanation A.", "ACTIVE", sortOrder = 0),
                    QuestionEntity("question_b", "topic", "subtopic", "Question B?", "Explanation B.", "ACTIVE", sortOrder = 1),
                ),
            )
            dao.upsertAnswerOptions(
                listOf(
                    AnswerOptionEntity("question_a", "answer_a1", "Answer A1", sortOrder = 0),
                    AnswerOptionEntity("question_b", "answer_b1", "Answer B1", sortOrder = 0),
                ),
            )

            assertFails {
                dao.upsertCorrectAnswers(
                    listOf(
                        QuestionCorrectAnswerEntity(
                            questionId = "question_a",
                            answerId = "answer_b1",
                        ),
                    ),
                )
            }
        }
    }

    private suspend fun withTestDatabase(
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

    private suspend fun insertRepresentativeGraph(dao: CurriculumDao) {
        dao.upsertTopics(
            listOf(
                TopicEntity(
                    id = "kotlin_language",
                    name = "Kotlin Language",
                    status = "ACTIVE",
                    sortOrder = 0,
                ),
            ),
        )
        dao.upsertSubtopics(
            listOf(
                SubtopicEntity(
                    id = "kotlin_generics",
                    topicId = "kotlin_language",
                    name = "Generics and variance",
                    status = "ACTIVE",
                    sortOrder = 0,
                ),
            ),
        )
        dao.upsertQuestions(
            listOf(
                QuestionEntity(
                    id = "kotlin_generics_001",
                    topicId = "kotlin_language",
                    subtopicId = "kotlin_generics",
                    text = "Which declarations are valid? Select all that apply.",
                    explanation = "Generic variance controls how parameterized types relate to each other.",
                    status = "ACTIVE",
                    sortOrder = 0,
                ),
            ),
        )
        dao.upsertAnswerOptions(
            listOf(
                AnswerOptionEntity(
                    questionId = "kotlin_generics_001",
                    id = "kotlin_generics_001_a",
                    text = "Use out when a type parameter is only produced.",
                    sortOrder = 0,
                ),
                AnswerOptionEntity(
                    questionId = "kotlin_generics_001",
                    id = "kotlin_generics_001_b",
                    text = "Use in when a type parameter is only consumed.",
                    sortOrder = 1,
                ),
                AnswerOptionEntity(
                    questionId = "kotlin_generics_001",
                    id = "kotlin_generics_001_c",
                    text = "Variance has no effect on assignability.",
                    sortOrder = 2,
                ),
            ),
        )
        dao.upsertCorrectAnswers(
            listOf(
                QuestionCorrectAnswerEntity(
                    questionId = "kotlin_generics_001",
                    answerId = "kotlin_generics_001_a",
                ),
                QuestionCorrectAnswerEntity(
                    questionId = "kotlin_generics_001",
                    answerId = "kotlin_generics_001_b",
                ),
            ),
        )
        dao.upsertQuestionSources(
            listOf(
                QuestionSourceEntity(
                    questionId = "kotlin_generics_001",
                    url = "https://kotlinlang.org/docs/generics.html",
                    title = "Generics: In, Out, Where",
                    sortOrder = 0,
                ),
                QuestionSourceEntity(
                    questionId = "kotlin_generics_001",
                    url = "https://kotlinlang.org/docs/classes.html",
                    title = "Classes",
                    sortOrder = 1,
                ),
            ),
        )
    }
}
