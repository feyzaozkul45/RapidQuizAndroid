package com.rapidquiz.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rapidquiz.app.data.model.AnswerRequest
import com.rapidquiz.app.ui.FakeQuizRepository.Companion.answer
import com.rapidquiz.app.ui.FakeQuizRepository.Companion.question
import com.rapidquiz.app.ui.quiz.QuizScreen
import com.rapidquiz.app.ui.quiz.QuizViewModel
import com.rapidquiz.app.ui.theme.RapidQuizTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuizScreenTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val repo = FakeQuizRepository()
    private var finished = false
    private var exited = false

    private fun launch() {
        val vm = QuizViewModel(repo, "s1", "fizik", feedbackDelayMs = 1_500)
        rule.setContent {
            RapidQuizTheme(dynamicColor = false) {
                QuizScreen(viewModel = vm, onFinished = { finished = true }, onExit = { exited = true })
            }
        }
    }

    private fun pressBack() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    @Test fun fullQuizFlowShowsFeedbackThenFinishes() {
        repo.questions += question(1, 0)
        repo.questions += question(2, 1)
        repo.answers += answer(correct = true, score = 100)
        repo.answers += answer(correct = false, last = true, score = 100)
        launch()

        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Soru 1/2") }
        rule.onNodeWithText("Seçenek A").performClick()
        rule.onNodeWithText("Doğru!").assertIsDisplayed()

        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Soru 2/2") }
        rule.onNodeWithText("Puan: 100").assertIsDisplayed()
        rule.onNodeWithText("Seçenek B").performClick()
        rule.onNodeWithText("Yanlış").assertIsDisplayed()

        rule.waitUntil(5_000) { finished }
        assertEquals(listOf(AnswerRequest(1, 1), AnswerRequest(2, 2)), repo.sentAnswers)
    }

    @Test fun timeoutSendsNullChoiceAndShowsMessage() {
        repo.questions += question(1, 0, remaining = 1.0)
        repo.questions += question(2, 1)
        repo.answers += answer(correct = false)
        launch()

        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Süre doldu") }
        assertEquals(listOf(AnswerRequest(1, null)), repo.sentAnswers)
    }

    @Test fun backShowsConfirmationAndCanBeDismissed() {
        repo.questions += question(1, 0, remaining = 60.0)
        launch()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Soru 1/2") }

        pressBack()
        rule.onNodeWithText("Quizden çıkılsın mı?").assertIsDisplayed()
        rule.onNodeWithText("Devam et").performClick()
        rule.onNodeWithText("Soru 1/2").assertIsDisplayed()
        assertFalse(exited)
    }

    @Test fun confirmingBackExitsQuiz() {
        repo.questions += question(1, 0, remaining = 60.0)
        launch()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Soru 1/2") }

        pressBack()
        rule.onNodeWithText("Çık").performClick()
        assertTrue(exited)
    }
}
