package com.example.rapidquiz.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    val icon: String? = null,
    val order: Int,
)

@Serializable
data class CreateSessionRequest(
    val category: String,
    @SerialName("client_type") val clientType: String = "android",
    @SerialName("recent_question_ids") val recentQuestionIds: List<Int> = emptyList(),
)

@Serializable
data class SessionCreated(
    @SerialName("session_id") val sessionId: String,
    val category: String,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("time_limit_seconds") val timeLimitSeconds: Int,
)

@Serializable
data class Choice(val id: Int, val text: String)

@Serializable
data class Question(
    val position: Int, // 0 tabanlı
    val total: Int,
    @SerialName("question_id") val questionId: Int,
    val text: String,
    val choices: List<Choice>,
    @SerialName("time_limit_seconds") val timeLimitSeconds: Int,
    @SerialName("served_at") val servedAt: String,
    @SerialName("remaining_seconds") val remainingSeconds: Double,
)

@Serializable
data class AnswerRequest(
    @SerialName("question_id") val questionId: Int,
    @SerialName("choice_id") val choiceId: Int?, // süre dolduysa null
)

@Serializable
data class AnswerResult(
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("correct_choice_id") val correctChoiceId: Int? = null,
    val points: Int,
    @SerialName("too_fast") val tooFast: Boolean = false,
    @SerialName("is_last") val isLast: Boolean,
    @SerialName("score_so_far") val scoreSoFar: Int,
)

@Serializable
data class QuizResult(
    @SerialName("session_id") val sessionId: String,
    val category: String,
    val status: String,
    val score: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("max_score") val maxScore: Int,
    @SerialName("player_name") val playerName: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
)

@Serializable
data class PlayerNameRequest(@SerialName("player_name") val playerName: String)

@Serializable
data class PlayerNameResult(
    @SerialName("session_id") val sessionId: String,
    val category: String,
    @SerialName("player_name") val playerName: String,
    val score: Int,
    @SerialName("correct_count") val correctCount: Int,
)

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    @SerialName("player_name") val playerName: String,
    val score: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("finished_at") val finishedAt: String,
    @SerialName("is_me") val isMe: Boolean = false,
)

@Serializable
data class Leaderboard(
    val category: String,
    val entries: List<LeaderboardEntry>,
    val me: LeaderboardEntry? = null, // yalnızca session_id verildiyse ve oturum tabloda ise
)
