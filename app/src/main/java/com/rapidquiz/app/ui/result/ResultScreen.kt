package com.rapidquiz.app.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rapidquiz.app.R
import com.rapidquiz.app.data.model.SessionCreated
import com.rapidquiz.app.ui.common.ErrorView
import com.rapidquiz.app.ui.common.LoadingView
import com.rapidquiz.app.ui.common.errorMessageRes
import com.rapidquiz.app.util.PlayerName

@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onOpenLeaderboard: (category: String, sessionId: String) -> Unit,
    onPlayAgain: (SessionCreated) -> Unit,
    onHome: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ResultEvent.OpenLeaderboard -> onOpenLeaderboard(event.category, event.sessionId)
                is ResultEvent.OpenQuiz -> onPlayAgain(event.session)
            }
        }
    }

    Scaffold { padding ->
        val modifier = Modifier.padding(padding)
        val result = state.result
        when {
            state.loading -> LoadingView(modifier)
            result == null -> ErrorView(
                state.errorCode ?: "unknown",
                modifier,
                onRetry = viewModel::load,
                secondaryLabel = stringResource(R.string.home),
                onSecondary = onHome,
            )
            else -> Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.result_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.result_score, result.score),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.result_correct_count, result.correctCount, result.totalQuestions),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(12.dp))

                val saved = state.savedName
                if (saved != null) {
                    Text(stringResource(R.string.result_name_saved, saved), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { onOpenLeaderboard(result.category, result.sessionId) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.result_view_leaderboard))
                    }
                } else {
                    NameForm(state = state, onNameChange = viewModel::onNameChange, onSave = viewModel::save)
                }

                state.replayErrorCode?.let {
                    Text(stringResource(errorMessageRes(it)), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                OutlinedButton(
                    onClick = viewModel::playAgain,
                    enabled = !state.replaying,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.result_play_again)) }
                TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.home)) }
            }
        }
    }
}

@Composable
fun NameForm(state: ResultUiState, onNameChange: (String) -> Unit, onSave: () -> Unit) {
    val problemText = state.problem?.let {
        stringResource(
            when (it) {
                PlayerName.Problem.TOO_SHORT -> R.string.name_too_short
                PlayerName.Problem.TOO_LONG -> R.string.name_too_long
                PlayerName.Problem.INVALID_CHARS -> R.string.name_invalid_chars
            },
        )
    }
    val errorText = problemText ?: state.saveErrorCode?.let { stringResource(errorMessageRes(it)) }

    OutlinedTextField(
        value = state.name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.result_name_label)) },
        placeholder = { Text(stringResource(R.string.result_name_hint)) },
        singleLine = true,
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (state.canSave) onSave() }),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
        if (state.saving) {
            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
        } else {
            Text(stringResource(R.string.result_save))
        }
    }
}
