package com.example.rapidquiz

import android.content.Context
import com.example.rapidquiz.data.ApiQuizRepository
import com.example.rapidquiz.data.DataStoreRecentQuestionsStore
import com.example.rapidquiz.data.QuizRepository
import com.example.rapidquiz.data.api.ApiJson
import com.example.rapidquiz.data.api.ClientHeadersInterceptor
import com.example.rapidquiz.data.api.ColdStartInterceptor
import com.example.rapidquiz.data.api.RapidQuizApi
import com.example.rapidquiz.data.api.ServerState
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
