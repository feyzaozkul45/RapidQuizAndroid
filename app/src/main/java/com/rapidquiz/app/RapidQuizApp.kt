package com.rapidquiz.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RapidQuizApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Sunucuyu uyandırmak için arka planda sağlık kontrolü; yanıt önemsenmez.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { container.api.health().close() }
        }
    }
}
