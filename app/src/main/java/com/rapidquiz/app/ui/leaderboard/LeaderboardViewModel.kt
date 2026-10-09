package com.rapidquiz.app.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rapidquiz.app.AppContainer
import com.rapidquiz.app.data.QuizRepository
import com.rapidquiz.app.data.api.toApiError
import com.rapidquiz.app.data.model.Category
import com.rapidquiz.app.data.model.LeaderboardEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val categories: List<Category> = emptyList(),
    val selectedSlug: String? = null,
    val entries: List<LeaderboardEntry> = emptyList(),
    val me: LeaderboardEntry? = null,
    val errorCode: String? = null,
) {
    /** İlk 10'da yer almayan kullanıcı satırı. */
    val meOutsideTop: LeaderboardEntry? get() = me?.takeIf { entries.none { e -> e.isMe } }
}

/**
 * [initialCategory] ve [sessionId] yalnızca bir oturumdan (sonuç ekranı) açıldığında dolu gelir.
 * session_id yalnızca o oturumun kategorisi için gönderilir.
 */
class LeaderboardViewModel(
    private val repo: QuizRepository,
    private val initialCategory: String?,
    private val sessionId: String?,
) : ViewModel() {
    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        loadAll(refreshing = false)
    }

    fun retry() = loadAll(refreshing = false)

    fun refresh() {
        if (_state.value.categories.isEmpty()) loadAll(refreshing = true) else loadBoard(_state.value.selectedSlug, refreshing = true)
    }

    fun select(slug: String) {
        if (slug == _state.value.selectedSlug) return
        loadBoard(slug, refreshing = false)
    }

    private fun loadAll(refreshing: Boolean) {
        job?.cancel()
        _state.update { it.copy(loading = !refreshing, refreshing = refreshing, errorCode = null) }
        job = viewModelScope.launch {
            try {
                val cats = repo.categories().sortedBy { it.order }
                val slug = initialCategory?.takeIf { s -> cats.any { it.slug == s } } ?: cats.firstOrNull()?.slug
                _state.update { it.copy(categories = cats) }
                if (slug == null) {
                    _state.update { it.copy(loading = false, refreshing = false) }
                } else {
                    fetchBoard(slug)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(loading = false, refreshing = false, errorCode = e.toApiError().code) }
            }
        }
    }

    private fun loadBoard(slug: String?, refreshing: Boolean) {
        if (slug == null) return
        job?.cancel()
        _state.update {
            it.copy(
                loading = !refreshing,
                refreshing = refreshing,
                selectedSlug = slug,
                errorCode = null,
                entries = if (refreshing) it.entries else emptyList(),
                me = if (refreshing) it.me else null,
            )
        }
        job = viewModelScope.launch {
            try {
                fetchBoard(slug)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(loading = false, refreshing = false, errorCode = e.toApiError().code) }
            }
        }
    }

    private suspend fun fetchBoard(slug: String) {
        _state.update { it.copy(selectedSlug = slug) }
        val board = repo.leaderboard(slug, sessionId?.takeIf { slug == initialCategory })
        _state.update { it.copy(loading = false, refreshing = false, entries = board.entries, me = board.me) }
    }

    companion object {
        fun factory(container: AppContainer, category: String?, sessionId: String?) = viewModelFactory {
            initializer { LeaderboardViewModel(container.repository, category, sessionId) }
        }
    }
}
