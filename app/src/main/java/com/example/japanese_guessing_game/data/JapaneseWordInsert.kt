package com.example.japanese_guessing_game.data

import kotlinx.serialization.Serializable

@Serializable
data class JapaneseWordInsert(
    val meaning_1: String,
    val meaning_2: String? = null,
    val hiragana: String
)
