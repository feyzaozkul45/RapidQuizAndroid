package com.rapidquiz.app

import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.rapidquiz.app.data.api.ApiJson
import com.rapidquiz.app.data.api.ClientHeadersInterceptor
import com.rapidquiz.app.data.api.ColdStartInterceptor
import com.rapidquiz.app.data.api.RapidQuizApi
import com.rapidquiz.app.data.api.ServerState
import com.rapidquiz.app.data.model.AnswerRequest
import com.rapidquiz.app.data.model.CreateSessionRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * GERÇEK API'ye karşı süre ölçümü (yalnızca debug test APK'sında bulunur; release'e girmez).
 * Prod'da her turda bir quiz oturumu açar, bu yüzden varsayılan olarak atlanır:
 *
 *   ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.latency=true
 *       -Pandroid.testInstrumentationRunnerArguments.class=com.rapidquiz.app.ApiLatencyBenchmark
 *
 * Sonuçlar logcat'te "ApiLatency" etiketiyle yazılır. İlk tur ("first") sunucu uykudaysa soğuk başlangıçtır;
 * sonraki turlar ("warm") sıcak durumdur. Uygulamanın gerçek istemci yığınını (başlık + soğuk başlangıç
 * interceptor'ları) kullanır.
 */
class ApiLatencyBenchmark {
    private val args = InstrumentationRegistry.getArguments()
    private val rounds = args.getString("rounds")?.toIntOrNull() ?: 5

    private val api: RapidQuizApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(ClientHeadersInterceptor(BuildConfig.CLIENT_VERSION))
                .addInterceptor(ColdStartInterceptor(ServerState()))
                .build(),
        )
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RapidQuizApi::class.java)

    private val samples = linkedMapOf<String, MutableList<Long>>()

    private suspend fun <T> timed(label: String, round: Int, block: suspend () -> T): T {
        val start = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            val ms = SystemClock.elapsedRealtime() - start
            Log.i(TAG, "round=$round $label ${ms}ms")
            samples.getOrPut(label) { mutableListOf() }.add(ms)
        }
    }

    @Test fun measure() = runBlocking {
        assumeTrue("latency=true verilmedi; gerçek API'ye gidilmiyor", args.getString("latency") == "true")

        repeat(rounds) { round ->
            val categories = timed("categories", round) { api.categories() }
            val slug = categories.first().slug
            val session = timed("create_session", round) { api.createSession(CreateSessionRequest(slug)) }
            val question = timed("current_question", round) { api.currentQuestion(session.sessionId) }
            timed("answer", round) {
                api.answer(session.sessionId, AnswerRequest(question.questionId, question.choices.first().id))
            }
            timed("leaderboard", round) { api.leaderboard(category = slug, limit = 10, sessionId = null) }
        }

        Log.i(TAG, "---- ÖZET (ms) ----")
        for ((label, list) in samples) {
            val first = list.first()
            val warm = list.drop(1).sorted()
            val median = if (warm.isEmpty()) -1 else warm[warm.size / 2]
            Log.i(TAG, "$label: first=$first warm_median=$median warm_min=${warm.firstOrNull()} warm_max=${warm.lastOrNull()}")
        }
    }

    private companion object {
        const val TAG = "ApiLatency"
    }
}
