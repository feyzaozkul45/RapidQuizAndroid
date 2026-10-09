package com.example.rapidquiz.data.api

import android.os.SystemClock
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class ClientHeadersInterceptor(private val clientVersion: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .header("X-Client-Type", "android")
                .header("X-Client-Version", clientVersion)
                .header("Accept", "application/json")
                .build(),
        )
}

/** Sunucunun en az bir kez yanıt verip vermediğini tutar (Render ücretsiz planda uyur). */
class ServerState {
    @Volatile
    var awake = false
}

/** Sunucu ilk yanıtını verene kadar uzun zaman aşımı + uyanma hatalarında yeniden deneme. */
class ColdStartInterceptor(
    private val state: ServerState,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
    private val sleep: (Long) -> Unit = { Thread.sleep(it) },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val startedAt = elapsedRealtime()
        while (true) {
            val timeout = if (state.awake) AWAKE_TIMEOUT_S else COLD_TIMEOUT_S
            try {
                val response = chain
                    .withConnectTimeout(timeout, TimeUnit.SECONDS)
                    .withReadTimeout(timeout, TimeUnit.SECONDS)
                    .proceed(chain.request())
                if (response.code !in WAKING_CODES) {
                    state.awake = true // 4xx/409 de olsa sunucu yanıt verdi
                    return response
                }
                if (state.awake || elapsedTooLong(startedAt)) return response
                response.close()
            } catch (e: IOException) {
                if (state.awake || elapsedTooLong(startedAt) || chain.call().isCanceled()) throw e
            }
            sleep(RETRY_DELAY_MS)
        }
    }

    private fun elapsedTooLong(startedAt: Long) =
        elapsedRealtime() - startedAt + RETRY_DELAY_MS >= TOTAL_BUDGET_MS

    private companion object {
        const val AWAKE_TIMEOUT_S = 10
        const val COLD_TIMEOUT_S = 90
        const val RETRY_DELAY_MS = 3_000L
        const val TOTAL_BUDGET_MS = 90_000L
        val WAKING_CODES = setOf(502, 503, 504)
    }
}
