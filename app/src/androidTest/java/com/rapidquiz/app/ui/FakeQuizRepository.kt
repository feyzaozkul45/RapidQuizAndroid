package com.rapidquiz.app.ui

import com.rapidquiz.app.data.QuizRepository
import com.rapidquiz.app.data.api.ApiError
import com.rapidquiz.app.data.model.AnswerRequest
import com.rapidquiz.app.data.model.AnswerResult
import com.rapidquiz.app.data.model.Category
import com.rapidquiz.app.data.model.Choice
import com.rapidquiz.app.data.model.Leaderboard
import com.rapidquiz.app.data.model.LeaderboardEntry
import com.rapidquiz.app.data.model.PlayerNameResult
import com.rapidquiz.app.data.model.Question
import com.rapidquiz.app.data.model.QuizResult
import com.rapidquiz.app.data.model.SessionCreated

class FakeQuizRepository : QuizRepository {
    val questions = ArrayDeque<Question>()
    val answers = ArrayDeque<AnswerResult>()
    val sentAnswers = mutableListOf<AnswerRequest>()
    var questionCalls = 0
    var categoryList = listOf(
        Category(1, "Fizik", "fizik", null, "science", 1),
        Category(2, "Yazılım", "yazilim", null, "code", 2),
    )
    val boards = mutableMapOf<String, Leaderboard>()

    // Sonuç ekranı
    var quizResult: QuizResult? = null
    var nameError: Throwable? = null
    val savedNames = mutableListOf<String>()
    var sessionError: Throwable? = null
    var createSessionCalls = 0
    var lastSessionCategory: String? = null

    /** Sunucu gibi: cevaplanana kadar aynı soruyu döndürür (ON_RESUME eşitlemesi buna dayanır). */
    override suspend fun currentQuestion(sessionId: String): Question {
        questionCalls++
        return questions.first()
    }

    override suspend fun answer(sessionId: String, request: AnswerRequest): AnswerResult {
        sentAnswers += request
        questions.removeFirstOrNull()
        return answers.removeFirst()
    }

    override suspend fun categories() = categoryList

    override suspend fun leaderboard(category: String, sessionId: String?) = boards[category] ?: Leaderboard(category, emptyList())

    override suspend fun rememberQuestion(category: String, questionId: Int) = Unit

    override suspend fun createSession(category: String): SessionCreated {
        createSessionCalls++
        lastSessionCategory = category
        sessionError?.let { throw it }
        return SessionCreated("new-session", category, 20, 5)
    }

    override suspend fun result(sessionId: String): QuizResult = quizResult ?: throw ApiError("session_not_found", 404)

    override suspend fun setPlayerName(sessionId: String, name: String): PlayerNameResult {
        savedNames += name
        nameError?.let { throw it }
        return PlayerNameResult(sessionId, "fizik", name, 1300, 13)
    }

    companion object {
        fun question(id: Int, position: Int, remaining: Double = 5.0, total: Int = 2) = Question(
            position = position,
            total = total,
            questionId = id,
            text = "Soru metni $id",
            choices = listOf(Choice(1, "Seçenek A"), Choice(2, "Seçenek B"), Choice(3, "Seçenek C"), Choice(4, "Seçenek D")),
            timeLimitSeconds = 5,
            servedAt = "2026-10-01T10:00:00Z",
            remainingSeconds = remaining,
        )

        fun answer(correct: Boolean, last: Boolean = false, score: Int = 100) =
            AnswerResult(correct, correctChoiceId = 1, points = if (correct) 100 else 0, isLast = last, scoreSoFar = score)

        fun result(playerName: String? = null) = QuizResult(
            sessionId = "s1",
            category = "fizik",
            status = "completed",
            score = 1300,
            correctCount = 13,
            totalQuestions = 20,
            maxScore = 2000,
            playerName = playerName,
        )

        fun entry(rank: Int, name: String, score: Int, me: Boolean = false) =
            LeaderboardEntry(rank, name, score, score / 100, "2026-10-01T10:00:00Z", me)
    }
}
