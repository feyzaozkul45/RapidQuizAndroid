package com.rapidquiz.app

import android.content.Context
import com.rapidquiz.app.data.ApiQuizRepository
import com.rapidquiz.app.data.DataStoreRecentQuestionsStore
import com.rapidquiz.app.data.QuizRepository
import com.rapidquiz.app.data.api.ApiJson
import com.rapidquiz.app.data.api.ClientHeadersInterceptor
import com.rapidquiz.app.data.api.ColdStartInterceptor
import com.rapidquiz.app.data.api.RapidQuizApi
import com.rapidquiz.app.data.api.ServerState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Elle bağımlılık enjeksiyonu: ağ ve depo nesneleri uygulama boyunca tek kopyadır. */
class AppContainer(context: Context) {
    val serverState = ServerState()

    val api: RapidQuizApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(ClientHeadersInterceptor(BuildConfig.CLIENT_VERSION))
                .addInterceptor(ColdStartInterceptor(serverState))
                .build(),
        )
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RapidQuizApi::class.java)

    val repository: QuizRepository =
        ApiQuizRepository(api, DataStoreRecentQuestionsStore(context.applicationContext))
}
