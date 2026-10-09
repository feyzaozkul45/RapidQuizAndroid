package com.rapidquiz.app.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rapidquiz.app.AppContainer
import com.rapidquiz.app.data.QuizRepository
import com.rapidquiz.app.data.api.toApiError
import com.rapidquiz.app.data.model.QuizResult
import com.rapidquiz.app.data.model.SessionCreated
import com.rapidquiz.app.util.PlayerName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultUiState(
    val loading: Boolean = true,
    val errorCode: String? = null,
    val result: QuizResult? = null,
    val name: String = "",
    val saving: Boolean = false,
    val saveErrorCode: String? = null,
    val savedName: String? = null,
    val replaying: Boolean = false,
    val replayErrorCode: String? = null,
) {
    val problem: PlayerName.Problem? get() = if (name.isBlank()) null else PlayerName.validate(name)
    val canSave: Boolean get() = !saving && savedName == null && name.isNotBlank() && problem == null
}

sealed interface ResultEvent {
    data class OpenLeaderboard(val category: String, val sessionId: String) : ResultEvent
    data class OpenQuiz(val session: SessionCreated) : ResultEvent
}

class ResultViewModel(private val repo: QuizRepository, private val sessionId: String) : ViewModel() {
    private val _state = MutableStateFlow(ResultUiState())
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    private val _events = Channel<ResultEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, errorCode = null) }
        viewModelScope.launch {
            try {
                val r = repo.result(sessionId)
                _state.update { it.copy(loading = false, result = r, savedName = r.playerName) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(loading = false, errorCode = e.toApiError().code) }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, saveErrorCode = null) }

    fun save() {
        val cur = _state.value
        val result = cur.result ?: return
        if (!cur.canSave) return
        _state.update { it.copy(saving = true, saveErrorCode = null) }
        viewModelScope.launch {
            try {
                val saved = repo.setPlayerName(sessionId, PlayerName.normalize(cur.name))
                _state.update { it.copy(saving = false, savedName = saved.playerName) }
                _events.send(ResultEvent.OpenLeaderboard(result.category, sessionId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(saving = false, saveErrorCode = e.toApiError().code) }
            }
        }
    }

    fun playAgain() {
        val result = _state.value.result ?: return
        if (_state.value.replaying) return
        _state.update { it.copy(replaying = true, replayErrorCode = null) }
        viewModelScope.launch {
            try {
                _events.send(ResultEvent.OpenQuiz(repo.createSession(result.category)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(replayErrorCode = e.toApiError().code) }
            } finally {
                _state.update { it.copy(replaying = false) }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, sessionId: String) = viewModelFactory {
            initializer { ResultViewModel(container.repository, sessionId) }
        }
    }
}
