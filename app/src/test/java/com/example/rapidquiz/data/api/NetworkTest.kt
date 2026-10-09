package com.example.rapidquiz.data.api

import com.example.rapidquiz.data.model.AnswerRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class NetworkTest {
    private lateinit var server: MockWebServer
    private val state = ServerState()
    private val sleeps = mutableListOf<Long>()
    private var now = 0L

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After fun tearDown() = server.shutdown()

    private fun api(): RapidQuizApi = Retrofit.Builder()
        .baseUrl(server.url("/api/v1/"))
        .client(
            OkHttpClient.Builder()
                .addInterceptor(ClientHeadersInterceptor("9.9"))
                .addInterceptor(ColdStartInterceptor(state, elapsedRealtime = { now }, sleep = { sleeps += it; now += it }))
                .build(),
        )
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RapidQuizApi::class.java)

    private val categoriesJson = """[{"id":1,"name":"Bilim","slug":"bilim","order":1,"extra":"yok sayılır"}]"""

    @Test fun sendsClientHeaders() = runBlocking {
        server.enqueue(MockResponse().setBody(categoriesJson))
        api().categories()
        val req = server.takeRequest()
        assertEquals("android", req.getHeader("X-Client-Type"))
        assertEquals("9.9", req.getHeader("X-Client-Version"))
        assertEquals("application/json", req.getHeader("Accept"))
        assertEquals("/api/v1/categories/", req.path)
    }

    @Test fun retriesWhileWakingUp() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(502))
        server.enqueue(MockResponse().setBody(categoriesJson))
        val result = api().categories()
        assertEquals("bilim", result.single().slug)
        assertEquals(listOf(3_000L, 3_000L), sleeps)
        assertTrue(state.awake)
        assertEquals(3, server.requestCount)
    }

    @Test fun doesNotRetryAfterAwake() = runBlocking {
        state.awake = true
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":{"code":"server_error"}}"""))
        val e = runCatching { api().categories() }.exceptionOrNull()!!
        assertEquals("server_error", e.toApiError().code)
        assertEquals(1, server.requestCount)
        assertTrue(sleeps.isEmpty())
    }

    @Test fun clientErrorMarksServerAwakeWithoutRetry() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"error":{"code":"category_unavailable"}}"""))
        runCatching { api().categories() }
        assertTrue(state.awake)
        assertEquals(1, server.requestCount)
    }

    @Test fun givesUpAfterBudget() = runBlocking {
        repeat(40) { server.enqueue(MockResponse().setResponseCode(503)) }
        val e = runCatching { api().categories() }.exceptionOrNull()!!
        assertEquals(503, (e.toApiError().status))
        assertTrue("toplam bekleme 90 sn'yi aşmamalı", sleeps.sum() <= 90_000)
    }

    @Test fun timeoutAnswerBodyKeepsNullChoiceId() = runBlocking {
        state.awake = true
        server.enqueue(
            MockResponse().setBody(
                """{"is_correct":false,"points":0,"is_last":false,"score_so_far":0}""",
            ),
        )
        api().answer("abc", AnswerRequest(questionId = 7, choiceId = null))
        val req = server.takeRequest()
        assertEquals("""{"question_id":7,"choice_id":null}""", req.body.readUtf8())
    }
}
