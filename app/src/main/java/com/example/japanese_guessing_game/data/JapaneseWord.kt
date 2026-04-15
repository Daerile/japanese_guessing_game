package com.example.japanese_guessing_game.data

import kotlinx.serialization.Serializable

@Serializable
data class JapaneseWord(
    val id: Int,
    val meaning_1: String,
    val meaning_2: String? = null,
    val hiragana: String,
    val added_at: String
)