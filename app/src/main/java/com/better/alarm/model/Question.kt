package com.better.alarm.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class Question(
    val id: Int,
    val description: String,
    val choices: List<String>,
    val correctAnswer: Int
)

class Questions {
    private val gson = Gson()

    fun saveToJsonFile(context: Context, fileName: String, questions: List<Question>) {
        val jsonString = gson.toJson(questions)
        val file = File(context.filesDir, fileName)
        file.writeText(jsonString)
    }

    fun loadFromJsonFile(context: Context, fileName: String): List<Question>? {
        val file = File(context.filesDir, fileName)

        if (!file.exists()) {
            return null
        }

        val jsonString = file.readText()
        val questionType = object : TypeToken<List<Question>>() {}.type

        return gson.fromJson(jsonString, questionType)
    }
}

