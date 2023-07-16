package com.better.alarm.model

import kotlin.random.Random

class QuestionList {

    fun getRandomQuestion(questionType: QuestionType): Question {
        return when (questionType) {
            QuestionType.ALL -> {
                (javaQuestions + sqlQuestions).random()
            }
            QuestionType.JAVA -> {
                javaQuestions.random()
            }
            else -> {
                sqlQuestions.random()
            }
        }
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

    private val javaQuestions = listOf(
        Question(
            id = 0,
            description = "Java에서 변수의 값을 조건부로 할당하기 위해 사용하는 연산자는 무엇인가요?",
            choices = listOf("?:", "??", "&&", "||"),
            correctAnswer = 0
        ),
        Question(
            id = 1,
            description = "Java에서 변경할 수 없는 변수를 선언하려면 어떤 키워드를 사용해야 하나요?",
            choices = listOf("final", "const", "immutable", "fixed"),
            correctAnswer = 0
        ),
        Question(
            id = 2,
            description = "Java에서 새 객체를 생성하기 위해 어떤 키워드를 사용하나요?",
            choices = listOf("new", "this", "object", "class"),
            correctAnswer = 0
        ),
        Question(
            id = 3,
            description = "Java에서 다른 패키지의 클래스를 가져오기 위해 어떤 키워드를 사용하나요?",
            choices = listOf("include", "import", "require", "use"),
            correctAnswer = 1
        ),
        Question(
            id = 4,
            description = "Java에서 추상 메서드를 선언하려면 어떤 키워드를 사용하나요?",
            choices = listOf("abstract", "virtual", "interface", "unimplemented"),
            correctAnswer = 0
        ),
        Question(
            id = 5,
            description = "Java에서 현재 클래스의 메서드를 오버라이딩하려면 어떤 키워드를 사용하나요?",
            choices = listOf("override", "super", "this", "extends"),
            correctAnswer = 0
        ),
        Question(
            id = 6,
            description = "Java에서 부모 클래스의 메서드를 호출하려면 어떤 키워드를 사용하나요?",
            choices = listOf("parent", "super", "this", "base"),
            correctAnswer = 1
        ),
        Question(
            id = 7,
            description = "Java에서 스레드를 생성하려면 어떤 클래스를 확장해야 하나요?",
            choices = listOf("Thread", "Runnable", "Process", "Execution"),
            correctAnswer = 0
        ),
        Question(
            id = 8,
            description = "Java에서 특정 메서드가 더이상 수정되지 않도록 하려면 어떤 키워드를 사용하나요?",
            choices = listOf("final", "fixed", "static", "immutable"),
            correctAnswer = 0
        ),
        Question(
            id = 9,
            description = "Java에서 현재 인스턴스에 접근하는 데 사용하는 키워드는 무엇인가요?",
            choices = listOf("self", "this", "me", "current"),
            correctAnswer = 1
        ),
        Question(
            id = 10,
            description = "Java에서 정적 변수를 선언하는 데 사용하는 키워드는 무엇인가요?",
            choices = listOf("static", "const", "final", "var"),
            correctAnswer = 0
        ),
        Question(
            id = 11,
            description = "Java에서 메서드 오버로딩이란 무엇을 의미하나요?",
            choices = listOf(
                "메서드의 이름은 같지만 매개변수의 유형과 개수가 다른 메서드를 여러 개 가질 수 있다",
                "하위 클래스에서 부모 클래스의 메서드를 재정의하는 것",
                "메서드의 반환 타입을 변경하는 것",
                "메서드 내부의 로직을 변경하는 것"
            ),
            correctAnswer = 0
        ),
        Question(
            id = 12,
            description = "Java에서 생성자 오버로딩이란 무엇을 의미하나요?",
            choices = listOf(
                "하나의 클래스에 여러 생성자를 제공하는 것",
                "생성자의 로직을 변경하는 것",
                "생성자를 상속하는 것",
                "생성자를 오버라이드하는 것"
            ),
            correctAnswer = 0
        ),
        Question(
            id = 13,
            description = "Java에서 접근 제한자 중 클래스 외부에서 접근할 수 없는 것은 무엇인가요?",
            choices = listOf("public", "protected", "private", "default"),
            correctAnswer = 2
        ),
        Question(
            id = 14,
            description = "Java에서 추상 메서드를 선언하려면 어떤 키워드를 사용하나요?",
            choices = listOf("abstract", "virtual", "interface", "unimplemented"),
            correctAnswer = 0
        ),
        Question(
            id = 15,
            description = "Java에서 다른 패키지의 클래스를 가져오기 위해 어떤 키워드를 사용하나요?",
            choices = listOf("include", "import", "require", "use"),
            correctAnswer = 1
        ),
        Question(
            id = 16,
            description = "Java에서 현재 클래스의 메서드를 오버라이딩하려면 어떤 키워드를 사용하나요?",
            choices = listOf("override", "super", "this", "extends"),
            correctAnswer = 0
        ),
        Question(
            id = 17,
            description = "Java에서 부모 클래스의 메서드를 호출하려면 어떤 키워드를 사용하나요?",
            choices = listOf("parent", "super", "this", "base"),
            correctAnswer = 1
        ),
        Question(
            id = 18,
            description = "Java에서 특정 예외를 처리하려면 어떤 키워드를 사용하나요?",
            choices = listOf("throw", "throws", "try", "catch"),
            correctAnswer = 3
        ),
        Question(
            id = 19,
            description = "Java에서 클래스의 상수를 선언하려면 어떤 키워드를 사용하나요?",
            choices = listOf("const", "static", "final", "immutable"),
            correctAnswer = 2
        ),
        Question(
            id = 20,
            description = "Java에서 인터페이스를 구현하는 키워드는 무엇인가요?",
            choices = listOf("implements", "extends", "using", "inherits"),
            correctAnswer = 0
        ),
        Question(
            id = 21,
            description = "Java에서 상속을 위해 사용하는 키워드는 무엇인가요?",
            choices = listOf("extends", "inherits", "implements", "uses"),
            correctAnswer = 0
        ),
        Question(
            id = 22,
            description = "Java에서 현재 인스턴스에 접근하는 데 사용하는 키워드는 무엇인가요?",
            choices = listOf("self", "this", "me", "current"),
            correctAnswer = 1
        ),
        Question(
            id = 23,
            description = "Java에서 정적 변수를 선언하는 데 사용하는 키워드는 무엇인가요?",
            choices = listOf("static", "const", "final", "var"),
            correctAnswer = 0
        ),
        Question(
            id = 24,
            description = "Java에서 메서드 오버로딩이란 무엇을 의미하나요?",
            choices = listOf("메서드의 이름은 같지만 매개변수의 유형과 개수가 다른 메서드를 여러 개 가질 수 있다", "하위 클래스에서 부모 클래스의 메서드를 재정의하는 것", "메서드의 반환 타입을 변경하는 것", "메서드 내부의 로직을 변경하는 것"),
            correctAnswer = 0
        ),
        Question(
            id = 25,
            description = "Java에서 생성자 오버로딩이란 무엇을 의미하나요?",
            choices = listOf("하나의 클래스에 여러 생성자를 제공하는 것", "생성자의 로직을 변경하는 것", "생성자를 상속하는 것", "생성자를 오버라이드하는 것"),
            correctAnswer = 0
        ),
        Question(
            id = 26,
            description = "Java에서 접근 제한자 중 클래스 외부에서 접근할 수 없는 것은 무엇인가요?",
            choices = listOf("public", "protected", "private", "default"),
            correctAnswer = 2
        ),
        Question(
            id = 27,
            description = "Java에서 클래스 메서드를 선언하기 위해 사용하는 키워드는 무엇인가요?",
            choices = listOf("static", "final", "public", "void"),
            correctAnswer = 0
        ),
        Question(
            id = 28,
            description = "Java에서 배열의 길이를 얻는 키워드는 무엇인가요?",
            choices = listOf("length()", "length", "size()", "size"),
            correctAnswer = 1
        ),
        Question(
            id = 29,
            description = "Java에서 부동소수점 숫자를 선언하는 타입은 무엇인가요?",
            choices = listOf("int", "float", "double", "long"),
            correctAnswer = 2
        ),
        Question(
            id = 30,
            description = "Java에서 문자열 연결을 위해 사용하는 연산자는 무엇인가요?",
            choices = listOf("+", "concat()", "join()", "append()"),
            correctAnswer = 0
        ),
        Question(
            id = 31,
            description = "Java에서 동일한 타입의 여러 변수를 한 번에 선언하는 방법은 무엇인가요?",
            choices = listOf("int a = 1, b = 2, c = 3;", "int a = 1; int b = 2; int c = 3;", "int a, b, c = 1, 2, 3;", "int a; int b; int c;"),
            correctAnswer = 0
        ),
        Question(
            id = 32,
            description = "Java에서 예외를 직접 발생시키는 키워드는 무엇인가요?",
            choices = listOf("throw", "throws", "raise", "try"),
            correctAnswer = 0
        ),
        Question(
            id = 33,
            description = "Java에서 메서드에서 발생 가능한 예외를 선언하는 키워드는 무엇인가요?",
            choices = listOf("throw", "throws", "raise", "try"),
            correctAnswer = 1
        ),
        Question(
            id = 34,
            description = "Java에서 인스턴스의 메모리를 회수하기 위해 시스템에게 요청하는 메서드는 무엇인가요?",
            choices = listOf("collect()", "delete()", "remove()", "finalize()"),
            correctAnswer = 3
        ),
        Question(
            id = 35,
            description = "Java에서 현재 날짜와 시간을 얻기 위해 사용하는 클래스는 무엇인가요?",
            choices = listOf("Date", "Calendar", "LocalDateTime", "Time"),
            correctAnswer = 2
        ),
        Question(
            id = 36,
            description = "Java에서 정규표현식을 처리하는데 사용하는 클래스는 무엇인가요?",
            choices = listOf("Regex", "Pattern", "Matcher", "RegularExpression"),
            correctAnswer = 1
        ),
        Question(
            id = 37,
            description = "Java에서 애플리케이션의 실행을 일시 중지시키는 메서드는 무엇인가요?",
            choices = listOf("wait()", "stop()", "pause()", "sleep()"),
            correctAnswer = 3
        ),
        Question(
            id = 38,
            description = "Java에서 리스트의 마지막 요소를 제거하는 메서드는 무엇인가요?",
            choices = listOf("delete()", "removeLast()", "remove()", "pop()"),
            correctAnswer = 2
        ),
        Question(
            id = 39,
            description = "Java에서 연관된 값을 저장하는데 사용하는 클래스는 무엇인가요?",
            choices = listOf("ArrayList", "LinkedList", "HashMap", "TreeSet"),
            correctAnswer = 2
        ),
        Question(
            id = 40,
            description = "Java에서 문자열의 길이를 얻는 메서드는 무엇인가요?",
            choices = listOf("length", "size", "length()", "size()"),
            correctAnswer = 2
        ),
        Question(
            id = 41,
            description = "Java에서 문자열에서 특정 위치의 문자를 얻는 메서드는 무엇인가요?",
            choices = listOf("charAt()", "at()", "getChar()", "index()"),
            correctAnswer = 0
        ),
        Question(
            id = 42,
            description = "Java에서 문자열에서 특정 문자나 문자열이 시작되는 인덱스를 반환하는 메서드는 무엇인가요?",
            choices = listOf("indexOf()", "find()", "search()", "locate()"),
            correctAnswer = 0
        ),
        Question(
            id = 43,
            description = "Java에서 문자열을 소문자로 변환하는 메서드는 무엇인가요?",
            choices = listOf("toLowerCase()", "toLower()", "lower()", "downcase()"),
            correctAnswer = 0
        ),
        Question(
            id = 44,
            description = "Java에서 문자열을 대문자로 변환하는 메서드는 무엇인가요?",
            choices = listOf("toUpperCase()", "toUpper()", "upper()", "upcase()"),
            correctAnswer = 0
        ),
        Question(
            id = 45,
            description = "Java에서 문자열의 앞뒤 공백을 제거하는 메서드는 무엇인가요?",
            choices = listOf("trim()", "strip()", "cut()", "remove()"),
            correctAnswer = 0
        ),
        Question(
            id = 46,
            description = "Java에서 문자열을 분리하여 배열로 반환하는 메서드는 무엇인가요?",
            choices = listOf("split()", "divide()", "break()", "separate()"),
            correctAnswer = 0
        ),
        Question(
            id = 47,
            description = "Java에서 두 문자열이 같은지 비교하는 메서드는 무엇인가요?",
            choices = listOf("compare()", "equals()", "==", "isEqual()"),
            correctAnswer = 1
        ),
        Question(
            id = 48,
            description = "Java에서 문자열에서 특정 부분 문자열을 다른 문자열로 대체하는 메서드는 무엇인가요?",
            choices = listOf("replace()", "swap()", "exchange()", "change()"),
            correctAnswer = 0
        ),
        Question(
            id = 49,
            description = "Java에서 문자열을 특정 포맷으로 변환하는 메서드는 무엇인가요?",
            choices = listOf("format()", "style()", "template()", "layout()"),
            correctAnswer = 0
        ),
        Question(
            id = 50,
            description = "Java에서 문자열이 특정 문자열로 시작하는지 검사하는 메서드는 무엇인가요?",
            choices = listOf("startsWith()", "beginsWith()", "commencesWith()", "opensWith()"),
            correctAnswer = 0
        ),
        Question(
            id = 51,
            description = "Java에서 문자열이 특정 문자열로 끝나는지 검사하는 메서드는 무엇인가요?",
            choices = listOf("endsWith()", "finishesWith()", "closesWith()", "terminatesWith()"),
            correctAnswer = 0
        ),
        Question(
            id = 52,
            description = "Java에서 문자열의 문자들을 거꾸로 하는 메서드는 무엇인가요?",
            choices = listOf("StringBuilder's reverse()", "String's reverse()", "StringBuffer's reverse()", "None of the above"),
            correctAnswer = 2
        ),
        Question(
            id = 53,
            description = "Java에서 문자열에서 특정 문자나 문자열이 마지막으로 나타나는 위치의 인덱스를 반환하는 메서드는 무엇인가요?",
            choices = listOf("lastIndexOf()", "finalIndexOf()", "endIndexOf()", "terminateIndexOf()"),
            correctAnswer = 0
        ),
        Question(
            id = 54,
            description = "Java에서 문자열을 byte 배열로 변환하는 메서드는 무엇인가요?",
            choices = listOf("getBytes()", "toBytes()", "byteArray()", "asBytes()"),
            correctAnswer = 0
        ),
        Question(
            id = 55,
            description = "Java에서 두 문자열의 순서를 비교하는 메서드는 무엇인가요?",
            choices = listOf("compareTo()", "order()", "sequence()", "sort()"),
            correctAnswer = 0
        ),
        Question(
            id = 56,
            description = "Java에서 문자열에서 특정 인덱스의 부분 문자열을 얻는 메서드는 무엇인가요?",
            choices = listOf("substring()", "subpart()", "part()", "sub()"),
            correctAnswer = 0
        ),
        Question(
            id = 57,
            description = "Java에서 문자열에서 특정 문자열이 포함되어 있는지 확인하는 메서드는 무엇인가요?",
            choices = listOf("contains()", "includes()", "holds()", "consists()"),
            correctAnswer = 0
        ),
        Question(
            id = 59,
            description = "Java에서 문자열을 특정 문자열로 연결하는 메서드는 무엇인가요?",
            choices = listOf("concat()", "join()", "merge()", "link()"),
            correctAnswer = 0
        ),
        Question(
            id = 60,
            description = "Java에서 문자열을 char 배열로 변환하는 메서드는 무엇인가요?",
            choices = listOf("toCharArray()", "toChars()", "asCharArray()", "asChars()"),
            correctAnswer = 0
        ),
        Question(
            id = 61,
            description = "Java에서 절대값을 계산하는 메서드는 무엇인가요?",
            choices = listOf("Math.absolute()", "Math.abs()", "Math.absoluteValue()", "Math.absVal()"),
            correctAnswer = 1
        ),
        Question(
            id = 62,
            description = "Java에서 제곱근을 계산하는 메서드는 무엇인가요?",
            choices = listOf("Math.sqrt()", "Math.squareRoot()", "Math.root()", "Math.sqr()"),
            correctAnswer = 0
        ),
        Question(
            id = 63,
            description = "Java에서 두 값 중 최대값을 반환하는 메서드는 무엇인가요?",
            choices = listOf("Math.max()", "Math.maximum()", "Math.largest()", "Math.biggest()"),
            correctAnswer = 0
        ),
        Question(
            id = 64,
            description = "Java에서 두 값 중 최소값을 반환하는 메서드는 무엇인가요?",
            choices = listOf("Math.min()", "Math.minimum()", "Math.smallest()", "Math.least()"),
            correctAnswer = 0
        ),
        Question(
            id = 65,
            description = "Java에서 제곱 값을 계산하는 메서드는 무엇인가요?",
            choices = listOf("Math.pow()", "Math.power()", "Math.sqr()", "Math.square()"),
            correctAnswer = 0
        ),
        Question(
            id = 66,
            description = "Java에서 랜덤한 숫자를 생성하는 메서드는 무엇인가요?",
            choices = listOf("Math.rand()", "Math.random()", "Math.randomNumber()", "Math.randNum()"),
            correctAnswer = 1
        ),
        Question(
            id = 67,
            description = "Java에서 반올림을 수행하는 메서드는 무엇인가요?",
            choices = listOf("Math.round()", "Math.rnd()", "Math.nearest()", "Math.roundOff()"),
            correctAnswer = 0
        ),
        Question(
            id = 68,
            description = "Java에서 주어진 값의 부호를 반환하는 메서드는 무엇인가요?",
            choices = listOf("Math.signum()", "Math.sign()", "Math.sgn()", "Math.signVal()"),
            correctAnswer = 0
        ),
        Question(
            id = 69,
            description = "Java에서 수학적 상수 π(파이) 값을 얻기 위해 사용하는 속성은 무엇인가요?",
            choices = listOf("Math.PI", "Math.pi", "Math.Pi", "Math.PHI"),
            correctAnswer = 0
        ),
        Question(
            id = 70,
            description = "Java에서 수학적 상수 e(자연 로그의 밑) 값을 얻기 위해 사용하는 속성은 무엇인가요?",
            choices = listOf("Math.E", "Math.e", "Math.exp", "Math.Exp"),
            correctAnswer = 0
        ),
        Question(
            id = 71,
            description = "Java에서 주어진 값이 자연수 인지 판별하는 표준 라이브러리 메서드는 무엇인가요?",
            choices = listOf("Math.isInteger()", "Math.isNatural()", "Math.isWhole()", "None of the above"),
            correctAnswer = 3
        ),
        Question(
            id = 72,
            description = "Java에서 반올림한 결과를 long 타입으로 반환하는 메서드는 무엇인가요?",
            choices = listOf("Math.roundLong()", "Math.longRound()", "Math.roundToLong()", "None of the above"),
            correctAnswer = 3
        ),
        Question(
            id = 73,
            description = "Java에서 주어진 두 수의 차이를 절대값으로 반환하는 메서드는 무엇인가요?",
            choices = listOf("Math.difference()", "Math.absDiff()", "Math.diffAbs()", "None of the above"),
            correctAnswer = 3
        ),
        Question(
            id = 74,
            description = "Java에서 주어진 두 수의 평균을 반환하는 메서드는 무엇인가요?",
            choices = listOf("Math.average()", "Math.avg()", "Math.mean()", "None of the above"),
            correctAnswer = 3
        ),
        Question(
            id = 75,
            description = "Java에서 주어진 숫자를 특정 범위 내로 제한하는 메서드는 무엇인가요?",
            choices = listOf("Math.limit()", "Math.bound()", "Math.clamp()", "None of the above"),
            correctAnswer = 3
        ),
        Question(
            id = 76,
            description = "Java에서 주어진 숫자를 정수로 반내림하는 메서드는 무엇인가요?",
            choices = listOf("Math.floor()", "Math.roundDown()", "Math.intDown()", "None of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 77,
            description = "Java에서 주어진 숫자를 정수로 반올림하는 메서드는 무엇인가요?",
            choices = listOf("Math.ceil()", "Math.roundUp()", "Math.intUp()", "None of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 78,
            description = "Java에서 10의 거듭제곱을 반환하는 메서드는 무엇인가요?",
            choices = listOf("Math.pow10()", "Math.tenPow()", "Math.power10()", "None of the above"),
            correctAnswer = 3
        ),
        Question(
            id = 79,
            description = "Java에서 주어진 숫자를 각도로 변환하는 메서드는 무엇인가요?",
            choices = listOf("Math.toDegrees()", "Math.degrees()", "Math.angle()", "None of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 80,
            description = "Java에서 주어진 숫자를 라디안으로 변환하는 메서드는 무엇인가요?",
            choices = listOf("Math.toRadians()", "Math.radians()", "Math.rad()", "None of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 81,
            description = "다음 중 변수의 이름으로 사용 가능한 것은 무엇인가요?",
            choices = listOf("_myVariable", "123variable", "my-Variable", "public"),
            correctAnswer = 0
        ),
        Question(
            id = 82,
            description = "다음 중 변수의 이름으로 사용할 수 없는 것은 무엇인가요?",
            choices = listOf("myVariable123", "variable_my", "my_variable", "my Variable"),
            correctAnswer = 3
        ),
        Question(
            id = 83,
            description = "Java에서 변수의 타입 중 기본 데이터 타입은 무엇인가요?",
            choices = listOf("String", "Integer", "Double", "Boolean"),
            correctAnswer = 2
        ),
        Question(
            id = 84,
            description = "Java에서 변수의 타입 중 참조 데이터 타입은 무엇인가요?",
            choices = listOf("int", "double", "boolean", "String"),
            correctAnswer = 3
        ),
        Question(
            id = 85,
            description = "Java에서 변수의 타입 중 숫자를 표현하는 데이터 타입은 무엇인가요?",
            choices = listOf("String", "Boolean", "Double", "Integer"),
            correctAnswer = 3
        ),
        Question(
            id = 86,
            description = "Java에서 변수의 타입 중 논리값을 표현하는 데이터 타입은 무엇인가요?",
            choices = listOf("String", "Boolean", "Double", "Integer"),
            correctAnswer = 1
        ),
        Question(
            id = 87,
            description = "Java에서 변수의 타입 중 문자열을 표현하는 데이터 타입은 무엇인가요?",
            choices = listOf("String", "Boolean", "Double", "Integer"),
            correctAnswer = 0
        ),
        Question(
            id = 88,
            description = "Java에서 변수를 선언하고 초기값을 할당하기 위해 사용하는 키워드는 무엇인가요?",
            choices = listOf("val", "let", "var", "set"),
            correctAnswer = 2
        ),
        Question(
            id = 89,
            description = "Java에서 변수를 선언할 때, 상수로 사용할 변수에 사용하는 키워드는 무엇인가요?",
            choices = listOf("final", "const", "static", "constant"),
            correctAnswer = 0
        ),
        Question(
            id = 90,
            description = "Java에서 변수를 선언할 때, 변경할 수 없는 변수에 사용하는 키워드는 무엇인가요?",
            choices = listOf("val", "let", "var", "final"),
            correctAnswer = 3
        ),
        Question(
            id = 91,
            description = "Java에서 변수를 선언할 때, 값을 할당하지 않은 상태로 변수를 선언하기 위해 사용하는 키워드는 무엇인가요?",
            choices = listOf("null", "void", "undefined", "None of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 92,
            description = "Java에서 변수를 선언하고 값을 할당할 때, 값을 변경할 수 있는 변수에 사용하는 키워드는 무엇인가요?",
            choices = listOf("val", "let", "var", "const"),
            correctAnswer = 2
        ),
        Question(
            id = 93,
            description = "Java에서 변수의 값을 출력하기 위해 사용하는 메서드는 무엇인가요?",
            choices = listOf("System.in()", "System.out.println()", "System.print()", "System.out()"),
            correctAnswer = 1
        ),
        Question(
            id = 94,
            description = "Java에서 변수를 선언하고 값을 할당할 때, 기본적으로 값을 할당하지 않으면 변수에 어떤 값이 할당되나요?",
            choices = listOf("0", "1", "null", "undefined"),
            correctAnswer = 2
        ),
        Question(
            id = 95,
            description = "Java에서 변수의 값을 변경하기 위해 사용하는 연산자는 무엇인가요?",
            choices = listOf("+=", "-=", "*=", "="),
            correctAnswer = 3
        ),
        Question(
            id = 96,
            description = "Java에서 변수의 값을 증가시키기 위해 사용하는 연산자는 무엇인가요?",
            choices = listOf("+=", "-=", "++", "--"),
            correctAnswer = 2
        ),
        Question(
            id = 97,
            description = "Java에서 변수의 값을 감소시키기 위해 사용하는 연산자는 무엇인가요?",
            choices = listOf("+=", "-=", "++", "--"),
            correctAnswer = 3
        ),
        Question(
            id = 98,
            description = "Java에서 변수의 값을 비교하기 위해 사용하는 연산자는 무엇인가요?",
            choices = listOf("==", "!=", ">", "<"),
            correctAnswer = 0
        ),
        Question(
            id = 99,
            description = "Java에서 변수의 값을 논리 연산하기 위해 사용하는 연산자는 무엇인가요?",
            choices = listOf("&&", "||", "!", "&"),
            correctAnswer = 0
        )
    )
}



