package com.better.alarm.model

import kotlinx.serialization.Serializable

@Serializable
data class Questions(
    val id: Int,
    val title: String,
    val description: String,
    val choices: List<String>,
    val correctAnswer: Int
)
