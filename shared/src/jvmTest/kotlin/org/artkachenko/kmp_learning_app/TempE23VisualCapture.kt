package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.content.BundledLearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonCodeBlockTag
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonComparisonTag
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyListTag
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.learningUnitCardTag
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.learningLessonRowTag
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class TempE23VisualCapture {
    @Test
    fun capture() {
        val out = File(System.getenv("E23_SHOTS") ?: "/tmp/e23shots").also { it.mkdirs() }
        val units = runBlocking { BundledLearningContentRepository().getActiveUnitsByTopic("android_ui") }
        listOf(400, 1100).forEach { w ->
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            try {
                runSkikoComposeUiTest(size = Size(w.toFloat(), 900f)) {
                    val db = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
                        .setDriver(BundledSQLiteDriver()).build()
                    runBlocking {
                        CurriculumImporter(db, loadCurriculum = { BundledCurriculumSource.load() })
                            .importCurriculum()
                    }
                    setContent {
                        MaterialTheme {
                            KoinApplication(
                                configuration = koinConfiguration {
                                    modules(
                                        listOf(
                                            curriculumDataModule, learningContentModule,
                                            assessmentDataModule, savedQuestionDataModule,
                                            lessonStudyDataModule, topicStudyPresentationModule,
                                            module { single<CurriculumDatabase> { db } },
                                        ),
                                    )
                                },
                            ) {
                                CompositionLocalProvider(
                                    LocalUriHandler provides object : UriHandler {
                                        override fun openUri(uri: String) = Unit
                                    },
                                ) {
                                    Box(Modifier.size(w.dp, 900.dp).testTag("win")) { App() }
                                }
                            }
                        }
                    }
                    fun shoot(name: String) {
                        ImageIO.write(
                            onNodeWithTag("win").captureToImage().toAwtImage(),
                            "png", File(out, "$name-$w.png"),
                        )
                    }
                    waitUntil(timeoutMillis = 20_000) { onAllNodesWithText("UI — Views & Jetpack Compose").fetchSemanticsNodes().isNotEmpty() }
                    onNodeWithText("UI — Views & Jetpack Compose").performClick()
                    waitUntil(timeoutMillis = 20_000) { onAllNodesWithTag(TopicStudyListTag).fetchSemanticsNodes().isNotEmpty() }
                    shoot("01-topic")
                    // Unit 4: richest blocks (wide comparisons + code). The Units are a lazy list,
                    // so the row has to be scrolled into existence before it can be clicked.
                    val unit = units[3]
                    onNodeWithTag(TopicStudyListTag)
                        .performScrollToNode(hasTestTag(learningUnitCardTag(unit.id)))
                    onNodeWithTag(learningUnitCardTag(unit.id)).performClick()
                    waitUntil(timeoutMillis = 20_000) { onAllNodesWithTag(learningLessonRowTag(unit.lessons.first().id)).fetchSemanticsNodes().isNotEmpty() }
                    shoot("02-unit")
                    onNodeWithTag(learningLessonRowTag(unit.lessons.first().id)).performScrollTo().performClick()
                    waitUntil(timeoutMillis = 20_000) { onAllNodesWithText(unit.lessons.first().title).fetchSemanticsNodes().isNotEmpty() }
                    shoot("03-lesson-top")
                    onAllNodesWithTag(LearningLessonComparisonTag, useUnmergedTree = true)[0].performScrollTo()
                    shoot("04-comparison")
                    onAllNodesWithTag(LearningLessonCodeBlockTag, useUnmergedTree = true)[0].performScrollTo()
                    shoot("05-code")
                    onNodeWithText(unit.lessons.first().sources.first().title).performScrollTo()
                    shoot("06-sources")
                }
            } finally {
                stopKoin(); Dispatchers.resetMain()
            }
        }
    }
}
