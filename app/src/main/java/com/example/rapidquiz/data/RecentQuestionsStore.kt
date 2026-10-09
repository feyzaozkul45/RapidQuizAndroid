package com.example.rapidquiz.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** Kategori başına son görülen soru ID'leri (eskiden yeniye). */
interface RecentQuestionsStore {
    suspend fun get(category: String): List<Int>
    suspend fun remember(category: String, questionId: Int)
}

object RecentQuestions {
    const val MAX = 40

    /** Zaten varsa önce siler, sonra sona ekler; son [MAX] kaydı tutar. */
    fun add(list: List<Int>, id: Int): List<Int> = (list.filter { it != id } + id).takeLast(MAX)

    fun parse(raw: String?): List<Int> =
        raw?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.filter { it > 0 }?.takeLast(MAX).orEmpty()

    fun serialize(list: List<Int>): String = list.joinToString(",")
}

private val Context.recentDataStore by preferencesDataStore(name = "recent_questions")

class DataStoreRecentQuestionsStore(private val context: Context) : RecentQuestionsStore {
    private fun key(category: String) = stringPreferencesKey("recent_$category")

    override suspend fun get(category: String): List<Int> =
        runCatching { RecentQuestions.parse(context.recentDataStore.data.first()[key(category)]) }
            .getOrDefault(emptyList())

    override suspend fun remember(category: String, questionId: Int) {
        if (questionId <= 0) return
        runCatching {
            context.recentDataStore.edit { prefs ->
                val current = RecentQuestions.parse(prefs[key(category)])
                prefs[key(category)] = RecentQuestions.serialize(RecentQuestions.add(current, questionId))
            }
        }
    }
}
