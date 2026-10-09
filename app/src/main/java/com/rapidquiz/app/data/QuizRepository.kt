package com.rapidquiz.app.data

import com.rapidquiz.app.data.api.RapidQuizApi
import com.rapidquiz.app.data.api.toApiError
import com.rapidquiz.app.data.model.AnswerRequest
import com.rapidquiz.app.data.model.AnswerResult
import com.rapidquiz.app.data.model.Category
import com.rapidquiz.app.data.model.CreateSessionRequest
import com.rapidquiz.app.data.model.Leaderboard
import com.rapidquiz.app.data.model.PlayerNameRequest
import com.rapidquiz.app.data.model.PlayerNameResult
import com.rapidquiz.app.data.model.Question
import com.rapidquiz.app.data.model.QuizResult
import com.rapidquiz.app.data.model.SessionCreated
import kotlinx.coroutines.CancellationException

/** Tüm metotlar hata durumunda yalnızca [com.rapidquiz.app.data.api.ApiError] fırlatır. */
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
