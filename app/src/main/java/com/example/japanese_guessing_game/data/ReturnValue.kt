package com.example.japanese_guessing_game.data

import kotlinx.serialization.Serializable

@Serializable
data class ReturnValue(
    val statusCode: Int,
    val text: String?
)