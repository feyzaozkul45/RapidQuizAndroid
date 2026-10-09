package com.rapidquiz.app.ui.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rapidquiz.app.R
import com.rapidquiz.app.data.model.LeaderboardEntry
import com.rapidquiz.app.ui.common.ErrorView
import com.rapidquiz.app.ui.common.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.leaderboard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.categories.isNotEmpty()) {
                CategoryChips(
                    categories = state.categories.map { it.slug to it.name },
                    selected = state.selectedSlug,
                    onSelect = viewModel::select,
                )
            }
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.loading -> LoadingView()
                    state.errorCode != null -> ErrorView(state.errorCode!!, onRetry = viewModel::retry)
                    state.entries.isEmpty() -> EmptyBoard()
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.entries, key = { it.rank }) { LeaderboardRow(it) }
                        state.meOutsideTop?.let { me ->
                            item(key = "me") {
                                Text(
                                    stringResource(R.string.leaderboard_your_rank),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                LeaderboardRow(me.copy(isMe = true))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBoard() {
    // PullToRefreshBox kaydırılabilir içerik bekler; boş durumda da çekerek yenileme çalışsın.
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            Text(
                stringResource(R.string.leaderboard_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun CategoryChips(categories: List<Pair<String, String>>, selected: String?, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.first }) { (slug, name) ->
            FilterChip(selected = slug == selected, onClick = { onSelect(slug) }, label = { Text(name) })
        }
    }
}

@Composable
fun LeaderboardRow(entry: LeaderboardEntry) {
    val label = stringResource(
        if (entry.isMe) R.string.leaderboard_row_description_me else R.string.leaderboard_row_description,
        entry.rank, entry.playerName, entry.score, entry.correctCount,
    )
    Card(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = label },
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${entry.rank}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    entry.playerName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.leaderboard_correct_count, entry.correctCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box { Text("${entry.score}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        }
    }
}
