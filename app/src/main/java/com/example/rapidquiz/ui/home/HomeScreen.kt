package com.example.rapidquiz.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rapidquiz.R
import com.example.rapidquiz.data.model.Category
import com.example.rapidquiz.data.model.SessionCreated
import com.example.rapidquiz.ui.common.ErrorView
import com.example.rapidquiz.ui.common.LoadingView
import com.example.rapidquiz.ui.common.errorMessageRes

/** Sunucudaki ikon adını Material ikonuna eşler; bilinmeyen ad için varsayılan ikon kullanılır. */
fun categoryIcon(name: String?): ImageVector = when (name?.lowercase()?.replace(Regex("[^a-z]"), "")) {
    "science", "bilim", "flask", "atom" -> Icons.Filled.Science
    "history", "tarih" -> Icons.Filled.HistoryEdu
    "geography", "globe", "public", "world", "cografya" -> Icons.Filled.Public
    "sports", "sport", "soccer", "football", "spor" -> Icons.Filled.SportsSoccer
    "art", "palette", "sanat" -> Icons.Filled.Palette
    "music", "muzik" -> Icons.Filled.MusicNote
    "movie", "movies", "film", "cinema", "sinema" -> Icons.Filled.Movie
    "tech", "technology", "computer", "code", "teknoloji" -> Icons.Filled.Computer
    "book", "books", "literature", "edebiyat" -> Icons.AutoMirrored.Filled.MenuBook
    "math", "calculate", "matematik" -> Icons.Filled.Calculate
    "nature", "park", "doga" -> Icons.Filled.Park
    "food", "restaurant", "yemek" -> Icons.Filled.Restaurant
    else -> Icons.Filled.Quiz
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSessionStarted: (SessionCreated) -> Unit,
    onOpenLeaderboard: () -> Unit,
    sessionEndedNotice: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) { viewModel.sessionStarted.collect(onSessionStarted) }

    val startError = state.startErrorCode?.let { stringResource(errorMessageRes(it)) }
    LaunchedEffect(startError) {
        if (startError != null) {
            snackbar.showSnackbar(startError)
            viewModel.startErrorShown()
        }
    }
    val expiredNotice = stringResource(R.string.error_session_expired)
    LaunchedEffect(sessionEndedNotice) {
        if (sessionEndedNotice) snackbar.showSnackbar(expiredNotice)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenLeaderboard) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = stringResource(R.string.leaderboard_open))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when {
            state.loading -> LoadingView(modifier)
            state.errorCode != null -> ErrorView(state.errorCode!!, modifier, onRetry = viewModel::load)
            else -> LazyColumn(
                modifier = modifier,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.categories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        starting = state.startingSlug == category.slug,
                        enabled = state.startingSlug == null,
                        onClick = { viewModel.start(category) },
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(category: Category, starting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                categoryIcon(category.icon),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                category.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (starting) {
                Spacer(Modifier.width(12.dp))
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}
