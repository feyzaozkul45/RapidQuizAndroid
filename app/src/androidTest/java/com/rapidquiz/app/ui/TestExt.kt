package com.rapidquiz.app.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule

fun ComposeTestRule.onAllNodesWithTextExists(text: String): Boolean = onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

/** Skor tablosu satırları tek bir erişilebilirlik düğümüdür; içeriğine contentDescription ile bakılır. */
fun ComposeTestRule.rowExists(text: String): Boolean =
    onAllNodes(androidx.compose.ui.test.hasContentDescription(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
