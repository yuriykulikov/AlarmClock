package com.better.alarm.question

class QuestionListPython {
    fun getPythonQuestions(): List<Question> {
        return pythonQuestions
    }

    private val pythonQuestions = listOf(
        Question(
            id = 0,
            description = "Python 문제입니다. 정답은 0번?",
            choices = listOf("Python", "GET", "FETCH", "SELECT"),
            correctAnswer = 0
        ),
    )
}
