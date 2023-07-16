package com.better.alarm.question

class QuestionList {

    /**
     * Question 생성시
     *
     * 1. QuestionType에 Type 하나를 추가해준다 (예: C, DataStructure)
     * 2. QuestionList{Type} 의 클래스를 생성해서, 문제 리스트와 getter를 생성한다.
     * 3. questionMap에 key, value를 추가해준다
     * 4. 버튼도 업데이트 하고, 연동 한다. onCreateQuestionSelectView 메소드로 가서 연결해준다
     */
    private val questionsMap = mapOf(
        QuestionType.PYTHON to QuestionListPython().getPythonQuestions(),
        QuestionType.SQL to QuestionListSQL().getSQLQuestions(),
        QuestionType.JAVA to QuestionListJava().getJavaQuestions(),
        QuestionType.CODING_INTERVIEW to QuestionListCodingInterview().getCodingInterviewQuestions()
    )

    fun getRandomQuestion(questionType: QuestionType): Question? {
        return when (questionType) {
            QuestionType.ALL -> {
                getAllQuestions().random()
            }
            else -> {
                questionsMap[questionType]?.random()
            }
        }
    }

    private fun getAllQuestions(): List<Question> {
        return questionsMap.values.flatten()
    }
}



