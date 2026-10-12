package com.rapidquiz.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rapidquiz.app.AppContainer
import com.rapidquiz.app.data.QuizRepository
import com.rapidquiz.app.data.api.toApiError
import com.rapidquiz.app.data.model.Category
import com.rapidquiz.app.data.model.SessionCreated
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val errorCode: String? = null,
    /** Oturumu başlatılmakta olan kategori; doluyken kartlar kilitlidir. */
    val startingSlug: String? = null,
    val startErrorCode: String? = null,
)

class HomeViewModel(private val repo: QuizRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val startedSessions = Channel<SessionCreated>(Channel.BUFFERED)
    val sessionStarted = startedSessions.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, errorCode = null) }
        viewModelScope.launch {
            try {
                val list = repo.categories().sortedBy { it.order }
                _state.update { it.copy(loading = false, categories = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(loading = false, errorCode = e.toApiError().code) }
            }
        }
    }

    fun start(category: Category) {
        if (_state.value.startingSlug != null) return
        _state.update { it.copy(startingSlug = category.slug, startErrorCode = null) }
        viewModelScope.launch {
            try {
                startedSessions.send(repo.createSession(category.slug))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(startErrorCode = e.toApiError().code) }
            } finally {
                _state.update { it.copy(startingSlug = null) }
            }
        }
    }

    fun startErrorShown() = _state.update { it.copy(startErrorCode = null) }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { HomeViewModel(container.repository) }
        }
    }
}
