package com.rapidquiz.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rapidquiz.app.data.api.ApiError
import com.rapidquiz.app.navigation.Home
import com.rapidquiz.app.navigation.Leaderboard
import com.rapidquiz.app.navigation.Quiz
import com.rapidquiz.app.ui.FakeQuizRepository.Companion.question
import com.rapidquiz.app.ui.home.HomeScreen
import com.rapidquiz.app.ui.home.HomeViewModel
import com.rapidquiz.app.ui.leaderboard.LeaderboardScreen
import com.rapidquiz.app.ui.leaderboard.LeaderboardViewModel
import com.rapidquiz.app.ui.quiz.QuizScreen
import com.rapidquiz.app.ui.quiz.QuizViewModel
import com.rapidquiz.app.ui.theme.RapidQuizTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Ana ekran → kategori kartı → Quiz ekranı gezinmesi (AppNavHost ile aynı rota tipleri, sahte repository). */
@RunWith(AndroidJUnit4::class)
class HomeNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val repo = FakeQuizRepository()

    private fun launch() {
        rule.setContent {
            RapidQuizTheme(dynamicColor = false) {
                val nav = rememberNavController()
                NavHost(nav, startDestination = Home) {
                    composable<Home> {
                        val vm = remember { HomeViewModel(repo) }
                        HomeScreen(
                            viewModel = vm,
                            onSessionStarted = { nav.navigate(Quiz(it.sessionId, it.category)) },
                            onOpenLeaderboard = { nav.navigate(Leaderboard()) },
                        )
                    }
                    composable<Quiz> { entry ->
                        val route = entry.toRoute<Quiz>()
                        val vm = remember { QuizViewModel(repo, route.sessionId, route.category, feedbackDelayMs = 1_500) }
                        QuizScreen(viewModel = vm, onFinished = {}, onExit = { nav.popBackStack() })
                    }
                    composable<Leaderboard> {
                        val vm = remember { LeaderboardViewModel(repo, null, null) }
                        LeaderboardScreen(vm, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }

    @Test fun categoryCardsAreListed() {
        launch()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Fizik") }
        rule.onNodeWithText("Yazılım").assertIsDisplayed()
    }

    @Test fun tappingCategoryCardStartsSessionAndOpensQuiz() {
        repo.questions += question(1, 0)
        launch()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Yazılım") }

        rule.onNodeWithText("Yazılım").performClick()

        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Soru 1/2") }
        rule.onNodeWithText("Seçenek A").assertIsDisplayed()
        assertEquals(1, repo.createSessionCalls)
        assertEquals("yazilim", repo.lastSessionCategory)
    }

    @Test fun sessionFailureShowsMessageAndStaysOnHome() {
        repo.sessionError = ApiError("category_unavailable", 409)
        launch()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Fizik") }

        rule.onNodeWithText("Fizik").performClick()

        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Bu kategoride şu an yeterli soru yok. Başka bir kategori dene.") }
        assertFalse(rule.onAllNodesWithTextExists("Soru 1/2"))
        rule.onNodeWithText("Kategoriler").assertIsDisplayed()
    }
}
