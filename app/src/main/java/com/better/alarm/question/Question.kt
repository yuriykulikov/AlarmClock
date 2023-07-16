package com.better.alarm.question

data class Question(
    val id: Int,
    val description: String,
    val choices: List<String>,
    val correctAnswer: Int,
)

