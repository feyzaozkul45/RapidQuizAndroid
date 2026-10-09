package com.rapidquiz.app.data.api

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
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RapidQuizApi {
    @GET("categories/")
    suspend fun categories(): List<Category>

    @POST("quiz-sessions/")
    suspend fun createSession(@Body body: CreateSessionRequest): SessionCreated

    @GET("quiz-sessions/{id}/current-question/")
    suspend fun currentQuestion(@Path("id") id: String): Question

    @POST("quiz-sessions/{id}/answers/")
    suspend fun answer(@Path("id") id: String, @Body body: AnswerRequest): AnswerResult

    @GET("quiz-sessions/{id}/result/")
    suspend fun result(@Path("id") id: String): QuizResult

    @PATCH("quiz-sessions/{id}/player-name/")
    suspend fun setPlayerName(@Path("id") id: String, @Body body: PlayerNameRequest): PlayerNameResult

    @GET("leaderboard/")
    suspend fun leaderboard(
        @Query("category") category: String,
        @Query("limit") limit: Int = 10,
        @Query("session_id") sessionId: String? = null,
    ): Leaderboard

    @GET("health/")
    suspend fun health(): ResponseBody
}
