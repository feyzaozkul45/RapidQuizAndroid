package com.rapidquiz.app.ui.quiz

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rapidquiz.app.AppContainer
import com.rapidquiz.app.data.QuizRepository
import com.rapidquiz.app.data.api.toApiError
import com.rapidquiz.app.data.model.AnswerRequest
import com.rapidquiz.app.data.model.AnswerResult
import com.rapidquiz.app.data.model.Question
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuizUiState {
    data object Loading : QuizUiState

    data class Question(
        val q: com.rapidquiz.app.data.model.Question,
        val remainingMs: Long,
        val selectedId: Int?,
        val submitting: Boolean,
        val score: Int,
    ) : QuizUiState

    data class Feedback(
        val q: com.rapidquiz.app.data.model.Question,
        val selectedId: Int?,
        val result: AnswerResult,
        val timedOut: Boolean,
    ) : QuizUiState

    data object Finished : QuizUiState

    data class Error(val code: String) : QuizUiState
}

class QuizViewModel(
    private val repo: QuizRepository,
    private val sessionId: String,
    private val category: String,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
    private val feedbackDelayMs: Long = FEEDBACK_DELAY_MS,
) : ViewModel() {
    private val _state = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var score = 0
    private var loadJob: Job? = null
    private var tickJob: Job? = null

    init {
        loadQuestion()
    }

    fun loadQuestion() {
        loadJob?.cancel()
        tickJob?.cancel()
        _state.value = QuizUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val q = repo.currentQuestion(sessionId)
                repo.rememberQuestion(category, q.questionId)
                startCountdown(q)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val code = e.toApiError().code
                _state.value = if (code == "session_completed") QuizUiState.Finished else QuizUiState.Error(code)
            }
        }
    }

    private fun startCountdown(q: Question) {
        val totalMs = (q.remainingSeconds * 1000).toLong().coerceAtLeast(0)
        val deadline = elapsedRealtime() + totalMs
        _state.value = QuizUiState.Question(q, totalMs, selectedId = null, submitting = false, score = score)
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val left = deadline - elapsedRealtime()
                if (left <= 0) {
                    submit(null)
                    return@launch
                }
                (_state.value as? QuizUiState.Question)?.let { _state.value = it.copy(remainingMs = left) }
                delay(TICK_MS)
            }
        }
    }

    /** [choiceId] == null: süre doldu. İlk çağrıda kilitlenir; sonraki çağrılar yok sayılır. */
    fun submit(choiceId: Int?) {
        val cur = _state.value as? QuizUiState.Question ?: return
        if (cur.submitting) return
        tickJob?.cancel()
        _state.value = cur.copy(selectedId = choiceId, remainingMs = if (choiceId == null) 0 else cur.remainingMs, submitting = true)
        viewModelScope.launch {
            try {
                val r = repo.answer(sessionId, AnswerRequest(cur.q.questionId, choiceId))
                score = r.scoreSoFar
                _state.value = QuizUiState.Feedback(cur.q, choiceId, r, timedOut = choiceId == null)
                delay(feedbackDelayMs) // doğru/yanlış gösterimi, soru süresine dahil değil
                if (r.isLast) _state.value = QuizUiState.Finished else loadQuestion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Cevabın işlenip işlenmediği belli olmayabilir: körlemesine tekrarlamak yerine sunucuyla eşitle.
                when (e.toApiError().code) {
                    "question_mismatch", "question_not_served", "network_error" -> loadQuestion()
                    "session_completed" -> _state.value = QuizUiState.Finished
                    else -> _state.value = QuizUiState.Error(e.toApiError().code)
                }
            }
        }
    }

    /** Uygulama arka plandan dönünce çağrılır (süre sunucuda akmaya devam etti). */
    fun resync() {
        val cur = _state.value
        if (cur is QuizUiState.Question && !cur.submitting) loadQuestion()
    }

    companion object {
        const val FEEDBACK_DELAY_MS = 1_000L
        private const val TICK_MS = 50L

        fun factory(container: AppContainer, sessionId: String, category: String) = viewModelFactory {
            initializer { QuizViewModel(container.repository, sessionId, category) }
        }
    }
}
