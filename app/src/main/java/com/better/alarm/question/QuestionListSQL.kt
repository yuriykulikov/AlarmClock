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
        ),
        Question(
            id = 116,
            description = "SQL에서 데이터베이스 테이블에서 모든 레코드를 선택하는 명령어는 무엇인가?",
            choices = listOf("SELECT * FROM table_name", "GET ALL FROM table_name", "FETCH ALL FROM table_name", "PICK * FROM table_name"),
            correctAnswer = 0
        ),
        Question(
            id = 117,
            description = "SQL에서 테이블을 생성하는 명령어는 무엇인가?",
            choices = listOf("CREATE DATABASE table_name", "CREATE TABLE table_name", "NEW TABLE table_name", "BUILD TABLE table_name"),
            correctAnswer = 1
        ),
        Question(
            id = 118,
            description = "SQL에서 특정 조건에 맞는 레코드를 선택하는 명령어는 무엇인가?",
            choices = listOf("SELECT * FROM table_name WHERE condition", "GET * FROM table_name WITH condition", "FETCH * FROM table_name IF condition", "PICK * FROM table_name WHERE condition"),
            correctAnswer = 2
        ),
        Question(
            id = 119,
            description = "SQL에서 테이블에서 특정 레코드를 삭제하는 명령어는 무엇인가?",
            choices = listOf("REMOVE FROM table_name WHERE condition", "DROP FROM table_name WHERE condition", "DELETE FROM table_name WHERE condition", "CUT FROM table_name WHERE condition"),
            correctAnswer = 3
        ),
        Question(
            id = 120,
            description = "SQL에서 테이블의 이름을 바꾸는 명령어는 무엇인가?",
            choices = listOf("RENAME TABLE old_name TO new_name", "CHANGE TABLE old_name TO new_name", "ALTER TABLE old_name TO new_name", "MODIFY TABLE old_name TO new_name"),
            correctAnswer = 0
        ),
        Question(
            id = 121,
            description = "SQL에서 NULL 값을 검사하는 데 어떤 연산자를 사용하는가?",
            choices = listOf("=", "==", "IS NULL", "NULL"),
            correctAnswer = 1
        ),
        Question(
            id = 122,
            description = "SQL에서 여러 테이블에서 데이터를 결합하는 SQL 명령어는 무엇인가?",
            choices = listOf("COMBINE", "MERGE", "JOIN", "BIND"),
            correctAnswer = 2
        ),
        Question(
            id = 123,
            description = "SQL에서 테이블에 새 레코드를 추가하는 SQL 명령어는 무엇인가?",
            choices = listOf("INSERT INTO table_name", "ADD TO table_name", "APPEND TO table_name", "INCLUDE INTO table_name"),
            correctAnswer = 3
        ),
        Question(
            id = 124,
            description = "SQL에서 테이블에서 특정 컬럼의 중복 값 없이 모든 값 선택하는 SQL 명령어는 무엇인가?",
            choices = listOf("SELECT DISTINCT column_name FROM table_name", "SELECT UNIQUE column_name FROM table_name", "SELECT column_name FROM table_name UNIQUE", "SELECT DISTINCTIVE column_name FROM table_name"),
            correctAnswer = 0
        ),
        Question(
            id = 125,
            description = "SQL에서 ORDER BY 명령어는 무엇을 하는가?",
            choices = listOf("특정 컬럼을 기준으로 테이블 재구성", "특정 컬럼을 기준으로 테이블 정렬", "테이블의 크기를 순서대로 변경", "테이블의 행 순서를 임의로 변경"),
            correctAnswer = 1
        ),
        Question(
            id = 126,
            description = "SQL에서 'LIKE' 연산자는 어떤 목적으로 사용되는가?",
            choices = listOf("패턴 검색", "수치 비교", "논리 연산", "중복 제거"),
            correctAnswer = 0
        ),
        Question(
            id = 127,
            description = "SQL에서 'COUNT' 함수는 어떤 값을 반환하는가?",
            choices = listOf("열의 합계", "테이블의 크기", "특정 행의 수", "테이블의 평균값"),
            correctAnswer = 1
        ),
        Question(
            id = 128,
            description = "SQL에서 테이블의 특정 열을 수정하는 명령어는 무엇인가?",
            choices = listOf("REPLACE INTO", "UPDATE table_name SET column_name = new_value WHERE condition", "CHANGE column_name = new_value", "MODIFY column_name = new_value"),
            correctAnswer = 2
        ),
        Question(
            id = 129,
            description = "SQL에서 'NOT NULL' 제약 조건이 의미하는 것은?",
            choices = listOf("값이 0이 아니어야 함", "값이 False가 아니어야 함", "값이 비어 있지 않아야 함", "값이 음수가 아니어야 함"),
            correctAnswer = 3
        ),
        Question(
            id = 130,
            description = "SQL에서 'INNER JOIN'은 무엇을 하는가?",
            choices = listOf("두 테이블의 교집합을 반환", "두 테이블을 순차적으로 결합", "두 테이블의 합집합을 반환", "두 테이블의 차집합을 반환"),
            correctAnswer = 0
        ),
        Question(
            id = 131,
            description = "'FOREIGN KEY'는 무엇을 위한 SQL 구문인가?",
            choices = listOf("데이터 유형을 지정", "테이블 간의 관계를 지정", "테이블에 새 열을 추가", "테이블에서 열을 제거"),
            correctAnswer = 1
        ),
        Question(
            id = 132,
            description = "SQL에서 'GROUP BY'는 어떤 목적으로 사용되는가?",
            choices = listOf("정렬", "그룹화", "중복 제거", "테이블 결합"),
            correctAnswer = 2
        ),
        Question(
            id = 133,
            description = "SQL에서 서브 쿼리는 어떤 목적으로 사용되는가?",
            choices = listOf("복잡한 쿼리를 단순화", "다른 쿼리 내부에서 쿼리를 실행", "쿼리의 성능 향상", "데이터베이스 연결 설정"),
            correctAnswer = 3
        ),
        Question(
            id = 134,
            description = "SQL에서 'HAVING'절은 어떤 용도로 사용되는가?",
            choices = listOf("그룹화된 결과에 조건을 적용", "결과를 정렬", "특정 행 선택", "결과에 제한 적용"),
            correctAnswer = 0
        ),
        Question(
            id = 135,
            description = "SQL에서 'LEFT JOIN'의 결과는 무엇인가?",
            choices = listOf("오른쪽 테이블의 모든 행과 왼쪽 테이블의 일치하는 행", "왼쪽 테이블의 모든 행과 오른쪽 테이블의 일치하는 행", "두 테이블의 일치하지 않는 행", "두 테이블의 일치하는 행"),
            correctAnswer = 1
        ),
        Question(
            id = 136,
            description = "SQL에서 'UNION' 연산자는 무엇을 하는가?",
            choices = listOf("두 쿼리의 결과를 합침", "두 테이블의 교집합을 찾음", "두 쿼리의 결과를 비교", "두 테이블의 차집합을 찾음"),
            correctAnswer = 2
        ),
        Question(
            id = 137,
            description = "SQL에서 'AS' 키워드는 어떤 목적으로 사용되는가?",
            choices = listOf("테이블의 이름 변경", "연산 결과에 별칭 부여", "테이블 복제", "데이터 형변환"),
            correctAnswer = 3
        ),
        Question(
            id = 138,
            description = "SQL에서 'AVG' 함수는 어떤 값을 반환하는가?",
            choices = listOf("열의 평균값", "열의 최대값", "열의 최소값", "열의 합계"),
            correctAnswer = 0
        ),
        Question(
            id = 139,
            description = "SQL에서 'BETWEEN' 연산자는 어떤 용도로 사용되는가?",
            choices = listOf("두 값 사이에 있는 값 비교", "두 값 사이에 있는 값 검색", "두 값을 비교", "두 값을 더함"),
            correctAnswer = 1
        ),
        Question(
            id = 140,
            description = "SQL에서 'UNIQUE' 제약 조건은 무엇을 의미하는가?",
            choices = listOf("열의 모든 값이 고유해야 함", "열의 모든 값이 NULL이어야 함", "열의 모든 값이 숫자여야 함", "열의 모든 값이 문자여야 함"),
            correctAnswer = 2
        ),
        Question(
            id = 141,
            description = "SQL에서 'TRUNCATE' 명령어는 무엇을 하는가?",
            choices = listOf("테이블의 모든 행 제거", "테이블의 모든 열 제거", "테이블 제거", "테이블 복제"),
            correctAnswer = 3
        ),
        Question(
            id = 142,
            description = "SQL에서 'MAX' 함수는 어떤 값을 반환하는가?",
            choices = listOf("열의 최대값", "열의 최소값", "열의 합계", "열의 평균값"),
            correctAnswer = 0
        ),
        Question(
            id = 143,
            description = "SQL에서 'MIN' 함수는 어떤 값을 반환하는가?",
            choices = listOf("열의 합계", "열의 최소값", "열의 최대값", "열의 평균값"),
            correctAnswer = 1
        ),
        Question(
            id = 144,
            description = "SQL에서 'RIGHT JOIN'의 결과는 무엇인가?",
            choices = listOf("왼쪽 테이블의 모든 행과 오른쪽 테이블의 일치하는 행", "오른쪽 테이블의 모든 행과 왼쪽 테이블의 일치하는 행", "두 테이블의 일치하지 않는 행", "두 테이블의 일치하는 행"),
            correctAnswer = 2
        ),
        Question(
            id = 145,
            description = "SQL에서 'FULL JOIN'의 결과는 무엇인가?",
            choices = listOf("두 테이블의 일치하는 행", "두 테이블의 일치하지 않는 행", "왼쪽 테이블의 모든 행과 오른쪽 테이블의 일치하는 행", "두 테이블의 모든 행"),
            correctAnswer = 3
        ),
        Question(
            id = 146,
            description = "다음 중 SQL의 SELECT 문을 올바르게 작성한 것은 무엇인가?",
            choices = listOf("SELECT * FROM table", "SELECT ALL FROM table", "ALL SELECT FROM table", "FROM table SELECT *"),
            correctAnswer = 0
        ),
        Question(
            id = 147,
            description = "'WHERE'절은 어떤 용도로 사용되는가?",
            choices = listOf("데이터 정렬", "조건에 따른 행 필터링", "데이터 그룹화", "데이터 집계"),
            correctAnswer = 1
        ),
        Question(
            id = 148,
            description = "테이블에 행을 추가하는 SQL 명령어는 무엇인가?",
            choices = listOf("ADD", "INSERT INTO", "UPDATE", "APPEND"),
            correctAnswer = 2
        ),
        Question(
            id = 149,
            description = "'ORDER BY'절은 어떤 용도로 사용되는가?",
            choices = listOf("데이터 그룹화", "데이터 집계", "데이터 정렬", "데이터 필터링"),
            correctAnswer = 3
        ),
        Question(
            id = 150,
            description = "테이블의 이름을 변경하는 SQL 명령어는 무엇인가?",
            choices = listOf("RENAME", "ALTER", "CHANGE", "REPLACE"),
            correctAnswer = 0
        ),
        Question(
            id = 151,
            description = "'DISTINCT' 키워드의 용도는 무엇인가?",
            choices = listOf("중복된 행 제거", "특정 행 선택", "결과 정렬", "결과 그룹화"),
            correctAnswer = 1
        ),
        Question(
            id = 152,
            description = "SQL에서 'NULL' 값은 어떤 것을 나타내는가?",
            choices = listOf("숫자 0", "빈 문자열", "값이 없음", "False"),
            correctAnswer = 2
        ),
        Question(
            id = 153,
            description = "'PRIMARY KEY'의 역할은 무엇인가?",
            choices = listOf("테이블의 모든 행에 고유한 ID 부여", "데이터를 암호화", "테이블을 다른 테이블에 연결", "데이터를 정렬"),
            correctAnswer = 3
        ),
        Question(
            id = 154,
            description = "다음 중 SQL에서 데이터베이스를 생성하는 명령어는 무엇인가?",
            choices = listOf("CREATE DATABASE dbname", "NEW DATABASE dbname", "ADD DATABASE dbname", "INSERT DATABASE dbname"),
            correctAnswer = 0
        ),
        Question(
            id = 155,
            description = "'LIMIT' 키워드의 역할은 무엇인가?",
            choices = listOf("조건에 따른 행 필터링", "결과 정렬", "조회된 행의 수 제한", "결과 그룹화"),
            correctAnswer = 1
        ),
        Question(
            id = 156,
            description = "다음 중 SQL에서 테이블을 삭제하는 명령어는 무엇인가?",
            choices = listOf("DROP TABLE table_name", "DELETE TABLE table_name", "REMOVE TABLE table_name", "CLEAR TABLE table_name"),
            correctAnswer = 2
        ),
        Question(
            id = 157,
            description = "SQL에서 특정 행을 삭제하는 명령어는 무엇인가?",
            choices = listOf("DELETE FROM table_name WHERE condition", "REMOVE FROM table_name WHERE condition", "DROP FROM table_name WHERE condition", "CLEAR FROM table_name WHERE condition"),
            correctAnswer = 3
        ),
        Question(
            id = 158,
            description = "SQL에서 'SUM' 함수는 어떤 값을 반환하는가?",
            choices = listOf("열의 합계", "열의 평균값", "열의 최대값", "열의 최소값"),
            correctAnswer = 0
        ),
        Question(
            id = 159,
            description = "다음 중 SQL에서 테이블의 구조를 확인하는 명령어는 무엇인가?",
            choices = listOf("SHOW STRUCTURE table_name", "DESCRIBE table_name", "CHECK table_name", "REVEAL table_name"),
            correctAnswer = 1
        ),
        Question(
            id = 160,
            description = "SQL에서 테이블의 모든 데이터를 삭제하는데, 테이블 구조는 유지하고 싶다면 어떤 명령어를 사용해야 하는가?",
            choices = listOf("DELETE table_name", "DROP TABLE table_name", "TRUNCATE TABLE table_name", "REMOVE table_name"),
            correctAnswer = 2
        ),
        Question(
            id = 161,
            description = "'AND' 및 'OR' 연산자는 SQL에서 어떻게 사용되는가?",
            choices = listOf("산술 연산", "문자열 결합", "조건 결합", "정렬 기준 설정"),
            correctAnswer = 3
        ),
        Question(
            id = 162,
            description = "SQL에서 'SELECT DISTINCT'는 어떤 결과를 반환하는가?",
            choices = listOf("중복된 값을 가진 행", "중복되지 않은 값을 가진 행", "정렬된 행", "테이블의 모든 행"),
            correctAnswer = 0
        ),
        Question(
            id = 163,
            description = "'IN' 연산자는 SQL에서 어떻게 사용되는가?",
            choices = listOf("범위 검사", "일치하는 값 검사", "조건에 따른 행 필터링", "여러 값 중 하나와 일치하는 행 찾기"),
            correctAnswer = 1
        ),
        Question(
            id = 164,
            description = "SQL에서 'ALTER TABLE' 명령어는 무엇을 하는가?",
            choices = listOf("테이블의 구조 변경", "테이블의 데이터 변경", "테이블의 이름 변경", "테이블 삭제"),
            correctAnswer = 2
        ),
        Question(
            id = 165,
            description = "SQL에서 'INNER JOIN'의 결과는 무엇인가?",
            choices = listOf("두 테이블의 모든 행", "두 테이블의 일치하지 않는 행", "두 테이블의 일치하는 행", "왼쪽 테이블의 모든 행과 오른쪽 테이블의 일치하는 행"),
            correctAnswer = 3
        )
    )
}
