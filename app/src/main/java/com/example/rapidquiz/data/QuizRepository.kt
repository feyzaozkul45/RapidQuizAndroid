package com.example.rapidquiz.data

import com.example.rapidquiz.data.api.RapidQuizApi
import com.example.rapidquiz.data.api.toApiError
import com.example.rapidquiz.data.model.AnswerRequest
import com.example.rapidquiz.data.model.AnswerResult
import com.example.rapidquiz.data.model.Category
import com.example.rapidquiz.data.model.CreateSessionRequest
import com.example.rapidquiz.data.model.Leaderboard
import com.example.rapidquiz.data.model.PlayerNameRequest
import com.example.rapidquiz.data.model.PlayerNameResult
import com.example.rapidquiz.data.model.Question
import com.example.rapidquiz.data.model.QuizResult
import com.example.rapidquiz.data.model.SessionCreated
import kotlinx.coroutines.CancellationException

/** Tüm metotlar hata durumunda yalnızca [com.example.rapidquiz.data.api.ApiError] fırlatır. */
interface QuizRepository {
    suspend fun categories(): List<Category>
    suspend fun createSession(category: String): SessionCreated
    suspend fun currentQuestion(sessionId: String): Question
    suspend fun answer(sessionId: String, request: AnswerRequest): AnswerResult
    suspend fun result(sessionId: String): QuizResult
    suspend fun setPlayerName(sessionId: String, name: String): PlayerNameResult
    suspend fun leaderboard(category: String, sessionId: String?): Leaderboard
    suspend fun rememberQuestion(category: String, questionId: Int)
}

class ApiQuizRepository(
    private val api: RapidQuizApi,
    private val recent: RecentQuestionsStore,
) : QuizRepository {
    override suspend fun categories() = call { api.categories() }

    override suspend fun createSession(category: String) = call {
        api.createSession(CreateSessionRequest(category, recentQuestionIds = recent.get(category)))
    }

    override suspend fun currentQuestion(sessionId: String) = call { api.currentQuestion(sessionId) }

    override suspend fun answer(sessionId: String, request: AnswerRequest) = call { api.answer(sessionId, request) }

    override suspend fun result(sessionId: String) = call { api.result(sessionId) }

    override suspend fun setPlayerName(sessionId: String, name: String) =
        call { api.setPlayerName(sessionId, PlayerNameRequest(name)) }

    override suspend fun leaderboard(category: String, sessionId: String?) =
        call { api.leaderboard(category = category, limit = 10, sessionId = sessionId) }

    override suspend fun rememberQuestion(category: String, questionId: Int) = recent.remember(category, questionId)

    private suspend inline fun <T> call(block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw e.toApiError()
    }
}
