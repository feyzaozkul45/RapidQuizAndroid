package com.rapidquiz.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rapidquiz.app.data.api.ApiError
import com.rapidquiz.app.data.model.SessionCreated
import com.rapidquiz.app.ui.result.ResultScreen
import com.rapidquiz.app.ui.result.ResultViewModel
import com.rapidquiz.app.ui.theme.RapidQuizTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultScreenTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val repo = FakeQuizRepository()
    private var leaderboard: Pair<String, String>? = null
    private var replay: SessionCreated? = null
    private var wentHome = false

    private fun launch(playerName: String? = null) {
        repo.quizResult = FakeQuizRepository.result(playerName)
        val vm = ResultViewModel(repo, "s1")
        rule.setContent {
            RapidQuizTheme(dynamicColor = false) {
                ResultScreen(
                    viewModel = vm,
                    onOpenLeaderboard = { category, session -> leaderboard = category to session },
                    onPlayAgain = { replay = it },
                    onHome = { wentHome = true },
                )
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("1300 puan") }
    }

    private fun typeName(text: String) = rule.onNode(hasSetTextAction()).performTextInput(text)

    @Test fun showsScoreAndCorrectCount() {
        launch()
        rule.onNodeWithText("1300 puan").assertIsDisplayed()
        rule.onNodeWithText("13/20 doğru").assertIsDisplayed()
    }

    @Test fun saveIsDisabledForTooShortName() {
        launch()
        typeName("a")
        rule.onNodeWithText("İsim en az 2 karakter olmalı.").assertIsDisplayed()
        rule.onNodeWithText("Kaydet").assertIsNotEnabled()
    }

    @Test fun invalidCharactersAreRejectedBeforeSending() {
        launch()
        typeName("Ali!")
        rule.onNodeWithText("Yalnızca harf, rakam, boşluk ve tire kullanabilirsin.").assertIsDisplayed()
        rule.onNodeWithText("Kaydet").assertIsNotEnabled()
        assertEquals(emptyList<String>(), repo.savedNames)
    }

    @Test fun inappropriateNameShowsPoliteErrorAndIsNotSent() {
        launch()
        typeName("Siktir")
        rule.onNodeWithText("Bu isim herkese açık skor tablosu için uygun görünmüyor. Lütfen başka bir isim dene.")
            .assertIsDisplayed()
        rule.onNodeWithText("Kaydet").assertIsNotEnabled()
        assertEquals(emptyList<String>(), repo.savedNames)
    }

    @Test fun tooLongNameIsRejected() {
        launch()
        typeName("a".repeat(21))
        rule.onNodeWithText("İsim en fazla 20 karakter olabilir.").assertIsDisplayed()
        rule.onNodeWithText("Kaydet").assertIsNotEnabled()
    }

    @Test fun successfulSaveSendsNormalizedNameAndOpensLeaderboard() {
        launch()
        typeName("  Ayşe   Nur ")
        rule.onNodeWithText("Kaydet").assertIsEnabled().performClick()

        rule.waitUntil(5_000) { leaderboard != null }
        assertEquals(listOf("Ayşe Nur"), repo.savedNames)
        assertEquals("fizik" to "s1", leaderboard)
    }

    @Test fun networkErrorShowsMessageAndAllowsRetry() {
        launch()
        repo.nameError = ApiError("network_error")
        typeName("Ayşe")
        rule.onNodeWithText("Kaydet").performClick()

        rule.waitUntil(5_000) {
            rule.onAllNodesWithTextExists("Sunucuya ulaşılamadı. Bağlantını kontrol edip tekrar dene.")
        }
        assertNull(leaderboard)
        rule.onNodeWithText("Kaydet").assertIsEnabled()

        repo.nameError = null
        rule.onNodeWithText("Kaydet").performClick()
        rule.waitUntil(5_000) { leaderboard != null }
        assertEquals(listOf("Ayşe", "Ayşe"), repo.savedNames)
    }

    @Test fun serverRejectionIsShownInTurkish() {
        launch()
        repo.nameError = ApiError("name_already_set", 409)
        typeName("Ayşe")
        rule.onNodeWithText("Kaydet").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Bu skor için isim zaten kaydedilmiş.") }
        assertNull(leaderboard)
    }

    @Test fun alreadySavedResultOffersLeaderboardInsteadOfForm() {
        launch(playerName = "Mehmet")
        rule.onNodeWithText("İsim kaydedildi: Mehmet").assertIsDisplayed()
        rule.onNodeWithText("Skor tablosunu gör").performClick()
        assertEquals("fizik" to "s1", leaderboard)
    }

    @Test fun playAgainStartsNewSessionInSameCategory() {
        launch()
        rule.onNodeWithText("Tekrar oyna").performClick()
        rule.waitUntil(5_000) { replay != null }
        assertEquals("new-session", replay?.sessionId)
        assertEquals("fizik", replay?.category)
    }

    @Test fun playAgainFailureShowsMessageAndStaysOnScreen() {
        launch()
        repo.sessionError = ApiError("category_unavailable", 409)
        rule.onNodeWithText("Tekrar oyna").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithTextExists("Bu kategoride şu an yeterli soru yok. Başka bir kategori dene.")
        }
        assertNull(replay)
        rule.onNodeWithText("Tekrar oyna").assertIsEnabled()
    }

    @Test fun homeButtonNavigatesHome() {
        launch()
        rule.onNodeWithText("Ana sayfa").performClick()
        assertTrue(wentHome)
    }
}
