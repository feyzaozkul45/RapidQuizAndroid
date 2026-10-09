package com.rapidquiz.app.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule

fun ComposeTestRule.onAllNodesWithTextExists(text: String): Boolean =
    onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
