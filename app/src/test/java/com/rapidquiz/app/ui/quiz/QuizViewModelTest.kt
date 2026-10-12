package com.rapidquiz.app.ui.quiz

import com.rapidquiz.app.data.QuizRepository
import com.rapidquiz.app.data.api.ApiError
import com.rapidquiz.app.data.model.AnswerRequest
import com.rapidquiz.app.data.model.AnswerResult
import com.rapidquiz.app.data.model.Category
import com.rapidquiz.app.data.model.Choice
import com.rapidquiz.app.data.model.Leaderboard
import com.rapidquiz.app.data.model.PlayerNameResult
import com.rapidquiz.app.data.model.Question
import com.rapidquiz.app.data.model.QuizResult
import com.rapidquiz.app.data.model.SessionCreated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun question(id: Int, position: Int = 0, remaining: Double = 5.0) = Question(
        position = position,
        total = 2,
        questionId = id,
        text = "Soru $id",
        choices = listOf(Choice(1, "A"), Choice(2, "B"), Choice(3, "C"), Choice(4, "D")),
        timeLimitSeconds = 5,
        servedAt = "2026-10-01T10:00:00Z",
        remainingSeconds = remaining,
    )

    private fun result(last: Boolean = false, score: Int = 100) =
        AnswerResult(isCorrect = true, correctChoiceId = 1, points = 100, isLast = last, scoreSoFar = score)

    private class FakeRepo : QuizRepository {
        val questions = ArrayDeque<Result<Question>>()
        val answers = ArrayDeque<Result<AnswerResult>>()
        val sent = mutableListOf<AnswerRequest>()
        val remembered = mutableListOf<Pair<String, Int>>()
        var questionCalls = 0

        override suspend fun currentQuestion(sessionId: String): Question {
            questionCalls++
            return questions.removeFirst().getOrThrow()
        }

        override suspend fun answer(sessionId: String, request: AnswerRequest): AnswerResult {
            sent += request
            return answers.removeFirst().getOrThrow()
        }

        override suspend fun rememberQuestion(category: String, questionId: Int) {
            remembered += category to questionId
        }

        override suspend fun categories(): List<Category> = error("unused")
        override suspend fun createSession(category: String): SessionCreated = error("unused")
        override suspend fun result(sessionId: String): QuizResult = error("unused")
        override suspend fun setPlayerName(sessionId: String, name: String): PlayerNameResult = error("unused")
        override suspend fun leaderboard(category: String, sessionId: String?): Leaderboard = error("unused")
    }

    private fun TestScope.vm(repo: FakeRepo) =
        QuizViewModel(repo, "s1", "bilim", elapsedRealtime = { currentTime }, feedbackDelayMs = 1_000)

    @Test fun loadsQuestionAndRemembersIt() = runTest(dispatcher) {
        val repo = FakeRepo().apply { questions += Result.success(question(11)) }
        val vm = vm(repo)
        runCurrent()
        val s = vm.state.value as QuizUiState.Question
        assertEquals(11, s.q.questionId)
        assertEquals(listOf("bilim" to 11), repo.remembered)
        vm.submit(1) // sayacı temizle
        advanceUntilIdle()
    }

    @Test fun timeoutSendsNullChoice() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            questions += Result.success(question(11))
            questions += Result.success(question(12, 1))
            answers += Result.success(result())
        }
        val vm = vm(repo)
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(listOf(AnswerRequest(11, null)), repo.sent)
        assertTrue((vm.state.value as QuizUiState.Feedback).timedOut)
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(12, (vm.state.value as QuizUiState.Question).q.questionId)
        vm.submit(1)
        advanceUntilIdle()
    }

    @Test fun countdownStartsFromRemainingSeconds() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            questions += Result.success(question(11, remaining = 2.0))
            answers += Result.success(result(last = true))
        }
        val vm = vm(repo)
        runCurrent()
        advanceTimeBy(2_001)
        runCurrent()
        assertEquals(listOf(AnswerRequest(11, null)), repo.sent)
    }

    @Test fun doubleTapSendsSingleRequest() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            questions += Result.success(question(11))
            answers += Result.success(result(last = true))
        }
        val vm = vm(repo)
        runCurrent()
        vm.submit(1)
        vm.submit(2)
        advanceUntilIdle()
        assertEquals(listOf(AnswerRequest(11, 1)), repo.sent)
    }

    @Test fun lastAnswerFinishes() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            questions += Result.success(question(11))
            answers += Result.success(result(last = true, score = 700))
        }
        val vm = vm(repo)
        runCurrent()
        vm.submit(1)
        runCurrent()
        assertEquals(700, (vm.state.value as QuizUiState.Feedback).result.scoreSoFar)
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(QuizUiState.Finished, vm.state.value)
    }

    @Test fun questionMismatchResyncsWithCurrentQuestion() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            questions += Result.success(question(11))
            questions += Result.success(question(12, 1))
            answers += Result.failure(ApiError("question_mismatch", 409))
        }
        val vm = vm(repo)
        runCurrent()
        vm.submit(1)
        runCurrent()
        assertEquals(2, repo.questionCalls)
        assertEquals(12, (vm.state.value as QuizUiState.Question).q.questionId)
        vm.submit(1)
        advanceUntilIdle()
    }

    @Test fun networkErrorOnAnswerResyncsInsteadOfRetrying() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            questions += Result.success(question(11))
            questions += Result.failure(ApiError("network_error"))
            answers += Result.failure(ApiError("network_error"))
        }
        val vm = vm(repo)
        runCurrent()
        vm.submit(1)
        runCurrent()
        assertEquals(1, repo.sent.size)
        assertEquals(QuizUiState.Error("network_error"), vm.state.value)
    }

    @Test fun sessionCompletedOnLoadMeansFinished() = runTest(dispatcher) {
        val repo = FakeRepo().apply { questions += Result.failure(ApiError("session_completed", 409)) }
        val vm = vm(repo)
        runCurrent()
        assertEquals(QuizUiState.Finished, vm.state.value)
    }

    @Test fun resyncReloadsOnlyWhileQuestionIsActive() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            questions += Result.success(question(11, remaining = 3.2))
            questions += Result.success(question(11, remaining = 1.0))
        }
        val vm = vm(repo)
        runCurrent()
        vm.onAppBackgrounded()
        vm.resync()
        runCurrent()
        assertEquals(2, repo.questionCalls)
        assertEquals(1_000L, (vm.state.value as QuizUiState.Question).remainingMs)
        assertNull((vm.state.value as QuizUiState.Question).selectedId)
        vm.onAppBackgrounded()
        vm.submit(1)
        runCurrent()
        vm.resync() // cevap gönderilirken yok sayılır
        runCurrent()
        assertEquals(2, repo.questionCalls)
    }

    @Test fun resyncWithoutBackgroundingDoesNotRefetch() = runTest(dispatcher) {
        val repo = FakeRepo().apply { questions += Result.success(question(11)) }
        val vm = vm(repo)
        runCurrent()
        vm.resync() // ilk açılıştaki ON_RESUME
        runCurrent()
        assertEquals(1, repo.questionCalls)
        vm.submit(1)
        advanceUntilIdle()
    }

    @Test fun resyncRunsOncePerBackgroundTrip() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            questions += Result.success(question(11))
            questions += Result.success(question(11, remaining = 4.0))
        }
        val vm = vm(repo)
        runCurrent()
        vm.onAppBackgrounded()
        vm.resync()
        runCurrent()
        vm.resync() // ikinci ON_RESUME (ör. dialog kapanışı) tekrar çağırmaz
        runCurrent()
        assertEquals(2, repo.questionCalls)
        vm.submit(1)
        advanceUntilIdle()
    }
}
