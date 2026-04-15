package com.example.japanese_guessing_game.data

import android.content.Context
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.atomicfu.TraceBase
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class WordRepository(private val context: Context) {
    private val supabase = SupabaseManager.client
    private val cacheFile = File(context.filesDir, "words_cache.json")
    private val json = Json { ignoreUnknownKeys = true }

    // Fetch all words from Supabase and cache them
    suspend fun getAllWords(): List<JapaneseWord> {
        // Timeout after 10 seconds
        val words = withTimeout(10000L) {
            supabase.postgrest["japanese_words"]
                .select()
                .decodeList<JapaneseWord>()
        }
        saveToCache(words)
        return words
    }

    // Load words from local cache
    fun loadFromCache(): List<JapaneseWord> {
        return if (cacheFile.exists()) {
            try {
                val jsonString = cacheFile.readText()
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun saveToCache(words: List<JapaneseWord>) {
        try {
            val jsonString = json.encodeToString(words)
            cacheFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // Add a new word
    suspend fun addWord(word: JapaneseWordInsert): ReturnValue {
        return try {
            val result = withTimeout(5000L) {
                supabase.postgrest["japanese_words"]
                    .insert(word)

            }
            ReturnValue(201, "successful word addition")
        } catch (e: UnknownRestException) {
            e.printStackTrace()
            ReturnValue(e.statusCode, e.error)
        }
    }
}
