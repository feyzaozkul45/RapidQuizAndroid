package com.rapidquiz.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rapidquiz.app.data.model.Leaderboard
import com.rapidquiz.app.ui.FakeQuizRepository.Companion.entry
import com.rapidquiz.app.ui.leaderboard.LeaderboardScreen
import com.rapidquiz.app.ui.leaderboard.LeaderboardViewModel
import com.rapidquiz.app.ui.theme.RapidQuizTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LeaderboardScreenTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val repo = FakeQuizRepository()

    private fun launch(category: String? = null, sessionId: String? = null) {
        val vm = LeaderboardViewModel(repo, category, sessionId)
        rule.setContent { RapidQuizTheme(dynamicColor = false) { LeaderboardScreen(vm, onBack = {}) } }
    }

    @Test fun showsFirstCategoryAndSwitchesWithChips() {
        repo.boards["fizik"] = Leaderboard("fizik", listOf(entry(1, "Ayşe", 1800), entry(2, "Mehmet", 1500)))
        repo.boards["yazilim"] = Leaderboard("yazilim", listOf(entry(1, "Zeynep", 2000)))
        launch()

        rule.waitUntil(5_000) { rule.rowExists("Ayşe") }
        rule.onNodeWithContentDescription("Mehmet", substring = true).assertIsDisplayed()

        rule.onNodeWithText("Yazılım").performClick()
        rule.waitUntil(5_000) { rule.rowExists("Zeynep") }
        rule.onNodeWithContentDescription("2000 puan", substring = true).assertIsDisplayed()
    }

    @Test fun showsOwnRankBelowListWhenOutsideTop10() {
        repo.boards["fizik"] = Leaderboard(
            "fizik",
            listOf(entry(1, "Ayşe", 1800)),
            me = entry(15, "Ben", 700, me = true),
        )
        launch(category = "fizik", sessionId = "s1")

        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Senin sıran") }
        rule.onNodeWithContentDescription("Senin sonucun: 15. sıra, Ben", substring = true).assertIsDisplayed()
    }

    @Test fun ownRowInsideTopIsNotDuplicated() {
        repo.boards["fizik"] = Leaderboard(
            "fizik",
            listOf(entry(1, "Ben", 1800, me = true)),
            me = entry(1, "Ben", 1800, me = true),
        )
        launch(category = "fizik", sessionId = "s1")

        rule.waitUntil(5_000) { rule.rowExists("Ben") }
        assertFalse(rule.onAllNodesWithTextExists("Senin sıran"))
    }

    @Test fun emptyBoardShowsMessage() {
        launch()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Bu kategoride henüz skor yok.") }
    }
}
