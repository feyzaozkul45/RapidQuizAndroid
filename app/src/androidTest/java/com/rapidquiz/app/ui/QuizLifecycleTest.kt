package com.rapidquiz.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rapidquiz.app.ui.FakeQuizRepository.Companion.question
import com.rapidquiz.app.ui.quiz.QuizScreen
import com.rapidquiz.app.ui.quiz.QuizViewModel
import com.rapidquiz.app.ui.theme.RapidQuizTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** ON_RESUME eşitlemesi yalnızca uygulama arka plandan dönünce çalışmalı. */
@RunWith(AndroidJUnit4::class)
class QuizLifecycleTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val repo = FakeQuizRepository()

    private fun launch() {
        repo.questions += question(1, 0, remaining = 60.0)
        val vm = QuizViewModel(repo, "s1", "fizik")
        rule.setContent {
            RapidQuizTheme(dynamicColor = false) { QuizScreen(viewModel = vm, onFinished = {}, onExit = {}) }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Soru 1/2") }
        rule.waitForIdle()
    }

    @Test fun firstOpenFetchesQuestionOnlyOnce() {
        launch()
        assertEquals(1, repo.questionCalls)
    }

    @Test fun returningFromBackgroundResyncsWithServer() {
        launch()
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED) // ON_STOP
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitUntil(5_000) { repo.questionCalls == 2 }
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Soru 1/2") }
    }

    @Test fun pauseWithoutStopDoesNotResync() {
        launch()
        rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED) // yalnızca ON_PAUSE
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
        assertEquals(1, repo.questionCalls)
    }
}
