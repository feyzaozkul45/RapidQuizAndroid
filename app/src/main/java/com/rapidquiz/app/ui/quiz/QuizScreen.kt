package com.rapidquiz.app.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rapidquiz.app.R
import com.rapidquiz.app.data.model.Choice
import com.rapidquiz.app.data.model.Question
import com.rapidquiz.app.ui.common.ErrorView
import com.rapidquiz.app.ui.common.LoadingView
import com.rapidquiz.app.ui.common.isSessionGone
import kotlin.math.ceil

private val CorrectGreen = Color(0xFF2E7D32)
private val WrongRed = Color(0xFFC62828)

@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onFinished: () -> Unit,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }

    // Quiz sırasında ekran uykuya geçmesin.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resync() }

    val finished = state is QuizUiState.Finished
    LaunchedEffect(finished) { if (finished) onFinished() }

    BackHandler(enabled = !finished) { confirmExit = true }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.quiz_exit_title)) },
            text = { Text(stringResource(R.string.quiz_exit_message)) },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; onExit() }) { Text(stringResource(R.string.quiz_exit_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.quiz_exit_cancel)) }
            },
        )
    }

    Scaffold { padding ->
        val modifier = Modifier.padding(padding)
        when (val s = state) {
            QuizUiState.Loading, QuizUiState.Finished -> LoadingView(modifier)
            is QuizUiState.Error ->
                if (isSessionGone(s.code)) {
                    ErrorView(s.code, modifier, secondaryLabel = stringResource(R.string.home), onSecondary = onExit)
                } else {
                    ErrorView(
                        s.code,
                        modifier,
                        onRetry = viewModel::loadQuestion,
                        secondaryLabel = stringResource(R.string.home),
                        onSecondary = onExit,
                    )
                }
            is QuizUiState.Question -> QuestionContent(
                modifier = modifier,
                q = s.q,
                score = s.score,
                remainingMs = s.remainingMs,
                selectedId = s.selectedId,
                correctId = null,
                feedback = null,
                enabled = !s.submitting,
                onChoice = { viewModel.submit(it) },
            )
            is QuizUiState.Feedback -> QuestionContent(
                modifier = modifier,
                q = s.q,
                score = s.result.scoreSoFar,
                remainingMs = 0,
                selectedId = s.selectedId,
                correctId = s.result.correctChoiceId,
                feedback = s,
                enabled = false,
                onChoice = {},
            )
        }
    }
}

@Composable
private fun QuestionContent(
    modifier: Modifier,
    q: Question,
    score: Int,
    remainingMs: Long,
    selectedId: Int?,
    correctId: Int?,
    feedback: QuizUiState.Feedback?,
    enabled: Boolean,
    onChoice: (Int) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.quiz_question_of, q.position + 1, q.total),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.quiz_score, score), style = MaterialTheme.typography.titleMedium)
        }

        if (feedback == null) {
            CountdownRing(remainingMs = remainingMs, totalMs = q.timeLimitSeconds * 1000L)
        } else {
            FeedbackBanner(feedback)
        }

        Text(
            q.text,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            q.choices.forEach { choice ->
                ChoiceButton(
                    choice = choice,
                    enabled = enabled,
                    state = when {
                        correctId != null && choice.id == correctId -> ChoiceState.Correct
                        feedback != null && choice.id == selectedId -> ChoiceState.Wrong
                        selectedId == choice.id -> ChoiceState.Selected
                        else -> ChoiceState.Idle
                    },
                    onClick = { onChoice(choice.id) },
                )
            }
        }
    }
}

@Composable
fun CountdownRing(remainingMs: Long, totalMs: Long) {
    val secondsLeft = ceil(remainingMs / 1000.0).toInt()
    val description = stringResource(R.string.quiz_seconds_left, secondsLeft)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(96.dp).clearAndSetSemantics { contentDescription = description },
    ) {
        CircularProgressIndicator(
            progress = { (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f) },
            modifier = Modifier.size(96.dp),
            strokeWidth = 8.dp,
        )
        Text(secondsLeft.toString(), style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun FeedbackBanner(f: QuizUiState.Feedback) {
    val (text, color) = when {
        f.timedOut -> stringResource(R.string.quiz_timeout) to WrongRed
        f.result.tooFast -> stringResource(R.string.quiz_too_fast) to MaterialTheme.colorScheme.tertiary
        f.result.isCorrect -> stringResource(R.string.quiz_correct) to CorrectGreen
        else -> stringResource(R.string.quiz_wrong) to WrongRed
    }
    Column(
        modifier = Modifier.heightIn(min = 96.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, style = MaterialTheme.typography.headlineSmall, color = color, textAlign = TextAlign.Center)
        if (f.result.points > 0) {
            Spacer(Modifier.padding(2.dp))
            Text(stringResource(R.string.quiz_points, f.result.points), style = MaterialTheme.typography.titleMedium)
        }
    }
}

enum class ChoiceState { Idle, Selected, Correct, Wrong }

@Composable
fun ChoiceButton(choice: Choice, enabled: Boolean, state: ChoiceState, onClick: () -> Unit) {
    val colors = when (state) {
        ChoiceState.Correct -> ButtonDefaults.buttonColors(
            containerColor = CorrectGreen, contentColor = Color.White,
            disabledContainerColor = CorrectGreen, disabledContentColor = Color.White,
        )
        ChoiceState.Wrong -> ButtonDefaults.buttonColors(
            containerColor = WrongRed, contentColor = Color.White,
            disabledContainerColor = WrongRed, disabledContentColor = Color.White,
        )
        ChoiceState.Selected -> ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
        )
        ChoiceState.Idle -> ButtonDefaults.filledTonalButtonColors()
    }
    val stateText = when (state) {
        ChoiceState.Correct -> stringResource(R.string.quiz_correct)
        ChoiceState.Wrong -> stringResource(R.string.quiz_wrong)
        else -> null
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics {
            stateText?.let { stateDescription = it }
        },
    ) {
        // Doğru/yanlış yalnızca renkle anlatılmasın: simge de gösterilir.
        when (state) {
            ChoiceState.Correct -> Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            ChoiceState.Wrong -> Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            else -> Unit
        }
        Text(choice.text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}
