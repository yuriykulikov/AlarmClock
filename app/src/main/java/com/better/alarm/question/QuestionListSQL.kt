package com.better.alarm.question

class QuestionListSQL {

    fun getSQLQuestions(): List<Question> {
        return sqlQuestions
    }

    private val sqlQuestions = listOf(
        Question(
            id = 0,
            description = "SQL에서 테이블의 모든 레코드를 선택하기 위해 어떤 키워드를 사용하나요?",
            choices = listOf("ALL", "GET", "FETCH", "SELECT"),
            correctAnswer = 3
        ),
        Question(
            id = 1,
            description = "SQL에서 'table_name' 테이블의 'column_name' 컬럼에서 유일한 값들만 선택하려면 어떻게 해야 하나요?",
            choices = listOf(
                "SELECT DISTINCT column_name FROM table_name",
                "SELECT UNIQUE column_name FROM table_name",
                "SELECT column_name FROM table_name DISTINCT",
                "SELECT column_name UNIQUE FROM table_name"
            ),
            correctAnswer = 0
        ),
        Question(
            id = 2,
            description = "SQL에서 테이블의 레코드를 삭제하려면 어떻게 해야 하나요?",
            choices = listOf(
                "REMOVE FROM table_name WHERE condition",
                "DELETE FROM table_name WHERE condition",
                "DROP FROM table_name WHERE condition",
                "CLEAR FROM table_name WHERE condition"
            ),
            correctAnswer = 1
        ),
        Question(
            id = 3,
            description = "SQL에서 두 테이블을 연결하기 위해 어떤 키워드를 사용하나요?",
            choices = listOf("CONNECT", "JOIN", "LINK", "BIND"),
            correctAnswer = 1
        ),
        Question(
            id = 4,
            description = "SQL에서 테이블에 새 레코드를 삽입하려면 어떤 키워드를 사용하나요?",
            choices = listOf("INSERT INTO", "ADD TO", "APPEND TO", "PUT INTO"),
            correctAnswer = 0
        ),
        Question(
            id = 5,
            description = "SQL에서 테이블의 특정 레코드를 업데이트하려면 어떤 키워드를 사용하나요?",
            choices = listOf("UPDATE", "CHANGE", "MODIFY", "ALTER"),
            correctAnswer = 0
        ),
        Question(
            id = 6,
            description = "SQL에서 테이블의 특정 레코드를 선택하려면 어떤 키워드를 사용하나요?",
            choices = listOf("GET", "SELECT", "FETCH", "PICK"),
            correctAnswer = 1
        ),
        Question(
            id = 7,
            description = "SQL에서 테이블의 이름을 변경하려면 어떤 키워드를 사용하나요?",
            choices = listOf("CHANGE", "ALTER", "MODIFY", "RENAME"),
            correctAnswer = 3
        ),
        Question(
            id = 8,
            description = "SQL에서 정렬을 내림차순으로 하려면 어떤 키워드를 사용하나요?",
            choices = listOf("DESC", "DOWN", "ASC", "UP"),
            correctAnswer = 0
        ),
        Question(
            id = 9,
            description = "SQL에서 'table_name' 테이블의 'column_name' 컬럼에서 유일한 값들만 선택하려면 어떻게 해야 하나요?",
            choices = listOf(
                "SELECT DISTINCT column_name FROM table_name",
                "SELECT UNIQUE column_name FROM table_name",
                "SELECT column_name FROM table_name DISTINCT",
                "SELECT column_name UNIQUE FROM table_name"
            ),
            correctAnswer = 0
        ),
        Question(
            id = 10,
            description = "SQL에서 테이블에 새 레코드를 삽입하려면 어떤 키워드를 사용하나요?",
            choices = listOf("INSERT INTO", "ADD TO", "APPEND TO", "PUT INTO"),
            correctAnswer = 0
        ),
        Question(
            id = 11,
            description = "SQL에서 테이블의 특정 레코드를 업데이트하려면 어떤 키워드를 사용하나요?",
            choices = listOf("UPDATE", "CHANGE", "MODIFY", "ALTER"),
            correctAnswer = 0
        ),
        Question(
            id = 12,
            description = "SQL에서 테이블의 이름을 변경하려면 어떤 키워드를 사용하나요?",
            choices = listOf("CHANGE", "ALTER", "MODIFY", "RENAME"),
            correctAnswer = 3
        ),
        Question(
            id = 13,
            description = "SQL에서 테이블의 특정 레코드를 선택하려면 어떤 키워드를 사용하나요?",
            choices = listOf("GET", "SELECT", "FETCH", "PICK"),
            correctAnswer = 1
        )
    )
}
