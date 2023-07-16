package com.better.alarm.question

class QuestionListPython {
    fun getPythonQuestions(): List<Question> {
        return pythonQuestions
    }

    private val pythonQuestions = listOf(
        Question(
            id = 0,
            description = "Python의 생성자 함수 이름은?",
            choices = listOf("Python", "__init__", "__new__", "__create__"),
            correctAnswer = 1
        ),
        Question(
            id = 1,
            description = "Python에서 불변의 리스트를 무엇이라고 하는가?",
            choices = listOf("List", "Tuple", "Set", "Dictionary"),
            correctAnswer = 1
        ),
        Question(
            id = 2,
            description = "Python의 'None'은 어떤 종류의 값인가?",
            choices = listOf("False", "0", "Null", "Empty string"),
            correctAnswer = 2
        ),
        Question(
            id = 3,
            description = "Python에서 반복 가능한 객체를 반환하는 함수는?",
            choices = listOf("Enum", "Loop", "Iter", "Repeat"),
            correctAnswer = 2
        ),
        Question(
            id = 4,
            description = "Python에서 문자열 형식을 지정하는 방법은?",
            choices = listOf("Using .format() method", "Using + operator", "Using % operator", "All of the above"),
            correctAnswer = 3
        ),
        Question(
            id = 5,
            description = "Python의 주석은 어떤 문자로 시작하나요?",
            choices = listOf("#", "//", "/*", "--"),
            correctAnswer = 0
        ),
        Question(
            id = 6,
            description = "Python에서 복사본이 아닌 리스트의 참조를 전달하려면 어떻게 해야하나요?",
            choices = listOf("copy()", "[]", "=", "clone()"),
            correctAnswer = 2
        ),
        Question(
            id = 7,
            description = "Python에서 나눗셈의 결과를 정수로 얻으려면 어떤 연산자를 사용해야하나요?",
            choices = listOf("/", "//", "%", "**"),
            correctAnswer = 1
        ),
        Question(
            id = 8,
            description = "Python에서 리스트를 정렬하려면 어떤 메소드를 사용해야하나요?",
            choices = listOf("sort()", "sorted()", "order()", "arrange()"),
            correctAnswer = 0
        ),
        Question(
            id = 9,
            description = "Python에서 집합의 항목을 추가하려면 어떤 메소드를 사용해야하나요?",
            choices = listOf("append()", "push()", "add()", "put()"),
            correctAnswer = 2
        ),
        Question(
            id = 10,
            description = "Python에서 모듈을 임포트하는 키워드는?",
            choices = listOf("import", "require", "include", "use"),
            correctAnswer = 0
        ),
        Question(
            id = 11,
            description = "Python에서 예외를 처리하는 키워드는?",
            choices = listOf("catch", "try", "handle", "except"),
            correctAnswer = 3
        ),
        Question(
            id = 12,
            description = "Python에서 임의의 숫자를 생성하는 라이브러리는?",
            choices = listOf("math", "random", "rand", "num"),
            correctAnswer = 1
        ),
        Question(
            id = 13,
            description = "Python에서 딕셔너리의 모든 키를 얻으려면 어떤 메소드를 사용해야 하나요?",
            choices = listOf("get()", "keys()", "items()", "values()"),
            correctAnswer = 1
        ),
        Question(
            id = 14,
            description = "Python에서 두 리스트를 합치려면 어떤 연산자를 사용해야 하나요?",
            choices = listOf("+", "*", "-", "/"),
            correctAnswer = 0
        ),
        Question(
            id = 15,
            description = "Python에서 무한 반복을 표현하는 키워드는?",
            choices = listOf("for", "while", "do", "until"),
            correctAnswer = 1
        ),
        Question(
            id = 16,
            description = "Python에서 JSON 데이터를 처리하는 모듈은?",
            choices = listOf("pickle", "json", "xml", "csv"),
            correctAnswer = 1
        ),
        Question(
            id = 17,
            description = "Python에서 클래스를 정의하는 키워드는?",
            choices = listOf("struct", "type", "class", "prototype"),
            correctAnswer = 2
        ),
        Question(
            id = 18,
            description = "Python에서 날짜와 시간을 처리하는 모듈은?",
            choices = listOf("time", "datetime", "calendar", "date"),
            correctAnswer = 1
        ),
        Question(
            id = 19,
            description = "Python에서 부모 클래스의 메소드를 호출하는 키워드는?",
            choices = listOf("parent", "super", "base", "above"),
            correctAnswer = 1
        ),
        Question(
            id = 20,
            description = "Python에서 리스트의 마지막 요소를 제거하는 메소드는?",
            choices = listOf("delete()", "remove()", "pop()", "clear()"),
            correctAnswer = 2
        ),
        Question(
            id = 21,
            description = "Python에서 'elif'는 어떤 키워드의 약자인가?",
            choices = listOf("else if", "elseif", "elif", "elseif if"),
            correctAnswer = 0
        ),
        Question(
            id = 22,
            description = "Python에서 정규 표현식을 사용하려면 어떤 모듈을 임포트해야 하나요?",
            choices = listOf("regex", "re", "regexp", "expression"),
            correctAnswer = 1
        ),
        Question(
            id = 23,
            description = "Python에서 함수를 정의하는 키워드는?",
            choices = listOf("function", "define", "def", "func"),
            correctAnswer = 2
        ),
        Question(
            id = 24,
            description = "Python에서 함수 내부에서 전역 변수를 사용하려면 어떤 키워드를 사용해야 하나요?",
            choices = listOf("global", "public", "extern", "super"),
            correctAnswer = 0
        ),
        Question(
            id = 25,
            description = "Python에서 불변 집합을 생성하는 키워드는?",
            choices = listOf("set", "dict", "frozenset", "tuple"),
            correctAnswer = 2
        ),
        Question(
            id = 26,
            description = "Python에서 부동소수점 숫자를 반올림하는 함수는?",
            choices = listOf("ceil()", "floor()", "round()", "truncate()"),
            correctAnswer = 2
        ),
        Question(
            id = 27,
            description = "Python에서 리스트에서 주어진 값의 첫 번째 발생 위치를 찾는 메소드는?",
            choices = listOf("find()", "search()", "locate()", "index()"),
            correctAnswer = 3
        ),
        Question(
            id = 28,
            description = "Python에서 'and', 'or', 'not'은 어떤 종류의 연산자인가?",
            choices = listOf("Arithmetic operators", "Comparison operators", "Assignment operators", "Logical operators"),
            correctAnswer = 3
        ),
        Question(
            id = 29,
            description = "Python에서 사용하는 가비지 컬렉션 기술은?",
            choices = listOf("Manual garbage collection", "Reference counting", "Automatic garbage collection", "No garbage collection"),
            correctAnswer = 1
        ),
        Question(
            id = 30,
            description = "Python에서 HTTP 요청을 보내려면 어떤 모듈을 사용하나요?",
            choices = listOf("http", "requests", "urllib", "socket"),
            correctAnswer = 1
        ),
        Question(
            id = 31,
            description = "Python에서 특정 코드 블록이 실행 시간이 얼마나 걸리는지 측정하려면 어떤 모듈을 사용하나요?",
            choices = listOf("time", "timer", "datetime", "timeit"),
            correctAnswer = 3
        ),
        Question(
            id = 32,
            description = "Python에서 작업을 지연시키려면 어떤 함수를 사용하나요?",
            choices = listOf("delay()", "wait()", "hold()", "sleep()"),
            correctAnswer = 3
        ),
        Question(
            id = 33,
            description = "Python에서 객체 지향 프로그래밍에서 '다형성'이란 무엇을 의미하나요?",
            choices = listOf("Same function name but different signatures", "Different function name and different signatures", "Same function name and same signatures", "None of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 34,
            description = "Python에서 실행 중인 프로그램에서 종료 코드를 반환하려면 어떤 모듈을 사용하나요?",
            choices = listOf("exit", "quit", "stop", "sys"),
            correctAnswer = 3
        ),
        Question(
            id = 35,
            description = "Python에서 SQL 데이터베이스에 연결하려면 어떤 모듈을 사용하나요?",
            choices = listOf("sql", "mysql", "sqlite3", "database"),
            correctAnswer = 2
        ),
        Question(
            id = 36,
            description = "Python에서 디렉토리를 생성하려면 어떤 모듈을 사용하나요?",
            choices = listOf("os", "sys", "path", "dir"),
            correctAnswer = 0
        ),
        Question(
            id = 37,
            description = "Python에서 문자열에서 모든 공백을 제거하려면 어떤 메소드를 사용하나요?",
            choices = listOf("trim()", "strip()", "cut()", "remove()"),
            correctAnswer = 1
        ),
        Question(
            id = 38,
            description = "Python에서 람다 함수는 무엇을 의미하나요?",
            choices = listOf("Long function", "Named function", "Anonymous function", "Recursive function"),
            correctAnswer = 2
        ),
        Question(
            id = 39,
            description = "Python에서 생성된 객체의 유형을 확인하려면 어떤 함수를 사용하나요?",
            choices = listOf("check()", "verify()", "type()", "instance()"),
            correctAnswer = 2
        ),
        Question(
            id = 40,
            description = "Python의 대화형 쉘을 무엇이라고 하는가?",
            choices = listOf("IDLE", "PyDev", "Jupyter", "Spyder"),
            correctAnswer = 0
        ),
        Question(
            id = 41,
            description = "Python에서 빈 리스트를 생성하는 코드는?",
            choices = listOf("[]", "{}", "()", "None"),
            correctAnswer = 0
        ),
        Question(
            id = 42,
            description = "Python에서 복소수를 지원하는 자료형은?",
            choices = listOf("int", "float", "complex", "str"),
            correctAnswer = 2
        ),
        Question(
            id = 43,
            description = "Python에서 문자열 길이를 반환하는 함수는?",
            choices = listOf("len", "size", "length", "count"),
            correctAnswer = 0
        ),
        Question(
            id = 44,
            description = "Python에서 문자열을 대문자로 변환하는 메소드는?",
            choices = listOf("uppercase()", "toUpper()", "upper()", "capitalize()"),
            correctAnswer = 2
        ),
        Question(
            id = 45,
            description = "Python의 논리적인 NOT 연산자는?",
            choices = listOf("!", "not", "~", "none"),
            correctAnswer = 1
        ),
        Question(
            id = 46,
            description = "Python에서 elif 이후에 명령문이 없으면 어떤 에러가 발생하는가?",
            choices = listOf("SyntaxError", "TypeError", "ValueError", "IndentationError"),
            correctAnswer = 0
        ),
        Question(
            id = 47,
            description = "Python에서 문자열 내에 있는 부분 문자열의 개수를 반환하는 메소드는?",
            choices = listOf("count()", "find()", "index()", "contains()"),
            correctAnswer = 0
        ),
        Question(
            id = 48,
            description = "Python에서 'is' 연산자는 무엇을 검사하는가?",
            choices = listOf("Value", "Data Type", "Identity", "None of the above"),
            correctAnswer = 2
        ),
        Question(
            id = 49,
            description = "Python에서 'while' 루프를 종료하는 키워드는?",
            choices = listOf("stop", "end", "break", "exit"),
            correctAnswer = 2
        ),
        Question(
            id = 50,
            description = "Python에서 'for' 루프를 종료하고 다음 반복을 시작하는 키워드는?",
            choices = listOf("skip", "next", "continue", "repeat"),
            correctAnswer = 2
        ),
        Question(
            id = 51,
            description = "Python에서 모든 항목이 참인지 검사하는 내장 함수는?",
            choices = listOf("all()", "any()", "bool()", "true()"),
            correctAnswer = 0
        ),
        Question(
            id = 52,
            description = "Python에서 파일을 열 때 사용하는 함수는?",
            choices = listOf("file()", "open()", "read()", "load()"),
            correctAnswer = 1
        ),
        Question(
            id = 53,
            description = "Python에서 실수를 정수로 변환하는 함수는?",
            choices = listOf("int()", "float()", "num()", "dec()"),
            correctAnswer = 0
        ),
        Question(
            id = 54,
            description = "Python에서 가비지 컬렉션을 수동으로 실행하려면 어떤 모듈을 사용해야 하는가?",
            choices = listOf("os", "sys", "gc", "memory"),
            correctAnswer = 2
        ),
        Question(
            id = 55,
            description = "Python에서 사용되는 객체지향 프로그래밍 용어 'self'는 무엇을 의미하는가?",
            choices = listOf("Current instance", "Class definition", "Method definition", "None of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 56,
            description = "Python에서 문자열을 정수로 변환하려면 어떤 함수를 사용하는가?",
            choices = listOf("int()", "str()", "char()", "ord()"),
            correctAnswer = 0
        ),
        Question(
            id = 57,
            description = "Python에서 딕셔너리에서 키와 값을 가져오려면 어떤 메소드를 사용하는가?",
            choices = listOf("get()", "items()", "fetch()", "extract()"),
            correctAnswer = 1
        ),
        Question(
            id = 58,
            description = "Python에서 제곱 연산을 수행하는 연산자는?",
            choices = listOf("^^", "**", "pow", "square"),
            correctAnswer = 1
        ),
        Question(
            id = 59,
            description = "Python에서 무작위 숫자를 생성하려면 어떤 모듈을 사용하는가?",
            choices = listOf("math", "random", "rand", "numbers"),
            correctAnswer = 1
        ),
        Question(
            id = 86,
            description = "Python에서 사용되는 논리 연산자는 무엇인가?",
            choices = listOf("and, or, not", "&&, ||, !", "&, |, !", "All of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 87,
            description = "Python에서 리스트의 길이를 구하는 방법은 무엇인가?",
            choices = listOf("len(list)", "list.len()", "list.length()", "list.size()"),
            correctAnswer = 1
        ),
        Question(
            id = 88,
            description = "Python에서 현재 시간을 얻는 방법은 무엇인가?",
            choices = listOf("time.current()", "time.now()", "time.time()", "All of the above"),
            correctAnswer = 2
        ),
        Question(
            id = 89,
            description = "Python에서 어떤 것이 예약어(reserved word)가 아닌가?",
            choices = listOf("if", "for", "print", "perform"),
            correctAnswer = 3
        ),
        Question(
            id = 90,
            description = "Python에서 'None' 은 무엇을 의미하는가?",
            choices = listOf("Null value", "False", "Zero", "Empty String"),
            correctAnswer = 0
        ),
        Question(
            id = 91,
            description = "Python에서 'is' 연산자는 무엇을 체크하는가?",
            choices = listOf("Value Equality", "Value Inequality", "Identity", "All of the above"),
            correctAnswer = 1
        ),
        Question(
            id = 92,
            description = "Python에서 'set' 은 어떤 종류의 데이터 구조인가?",
            choices = listOf("Ordered Collection", "Unordered Collection", "Key-Value Pairs", "None of the above"),
            correctAnswer = 2
        ),
        Question(
            id = 93,
            description = "Python에서 'elif' 키워드는 어떤 키워드의 조합인가?",
            choices = listOf("'else if'", "'elseif'", "'elif'", "None of the above"),
            correctAnswer = 3
        ),
        Question(
            id = 94,
            description = "Python에서 각 요소를 제곱하는 람다 함수는 어떻게 정의하는가?",
            choices = listOf("lambda x: x^2", "lambda x: x**2", "lambda x: pow(x, 2)", "All of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 95,
            description = "Python에서 제너레이터를 생성하는 방법 중 하나는?",
            choices = listOf("Using Generator Expressions", "Using yield keyword", "Using Generator function", "All of the above"),
            correctAnswer = 1
        ),
        Question(
            id = 96,
            description = "Python에서 리스트의 마지막 요소를 얻는 방법은?",
            choices = listOf("list[-1]", "list[len(list)]", "list[end]", "list.last()"),
            correctAnswer = 0
        ),
        Question(
            id = 97,
            description = "Python에서 실행 중에 변수 타입을 변경할 수 있는지?",
            choices = listOf("Yes", "No", "Depends on the variable", "Not Sure"),
            correctAnswer = 1
        ),
        Question(
            id = 98,
            description = "Python에서 비어있는 리스트를 어떻게 생성하는가?",
            choices = listOf("list = ()", "list = {}", "list = []", "list = None"),
            correctAnswer = 2
        ),
        Question(
            id = 99,
            description = "Python에서 정수를 부동 소수점으로 변환하는 함수는?",
            choices = listOf("int()", "float()", "str()", "None of the above"),
            correctAnswer = 3
        ),
        Question(
            id = 100,
            description = "Python에서 문자열을 대문자로 변환하는 메소드는?",
            choices = listOf("to_upper()", "uppercase()", "upper()", "toupper()"),
            correctAnswer = 0
        ),
        Question(
            id = 101,
            description = "Python에서 문자열의 길이를 얻는 함수는?",
            choices = listOf("length()", "len()", "strlen()", "size()"),
            correctAnswer = 1
        ),
        Question(
            id = 102,
            description = "Python에서 특정 값으로 리스트를 초기화하는 방법은?",
            choices = listOf("[0]*n", "[n]*0", "[n, n, n, ...]", "None of the above"),
            correctAnswer = 2
        ),
        Question(
            id = 103,
            description = "Python에서 사용할 수 없는 변수 이름은?",
            choices = listOf("_var", "var1", "1var", "var_name"),
            correctAnswer = 3
        ),
        Question(
            id = 104,
            description = "Python에서 문자열에서 문자를 제거하는 메소드는?",
            choices = listOf("delete()", "remove()", "strip()", "None of the above"),
            correctAnswer = 0
        ),
        Question(
            id = 105,
            description = "Python에서 리스트를 정렬하는 메소드는?",
            choices = listOf("sort()", "sorted()", "order()", "All of the above"),
            correctAnswer = 1
        ),
        Question(
            id = 106,
            description = "Python에서 'Hello World'를 출력하는 코드는?",
            choices = listOf("print('Hello World')", "System.out.println('Hello World')", "console.log('Hello World')", "printf('Hello World')"),
            correctAnswer = 2
        ),
        Question(
            id = 107,
            description = "Python에서 비어있는 딕셔너리를 생성하는 방법은?",
            choices = listOf("dict = {}", "dict = []", "dict = ()", "dict = None"),
            correctAnswer = 3
        ),
        Question(
            id = 108,
            description = "Python에서 'abc'.isdigit() 의 결과는?",
            choices = listOf("True", "False", "Depends on Python version", "Error"),
            correctAnswer = 0
        ),
        Question(
            id = 109,
            description = "Python에서 문자열을 분할하는 메소드는?",
            choices = listOf("divide()", "split()", "part()", "break()"),
            correctAnswer = 1
        ),
        Question(
            id = 110,
            description = "Python에서 논리적인 참과 거짓을 표현하는 값은?",
            choices = listOf("True/False", "1/0", "Yes/No", "All of the above"),
            correctAnswer = 2
        ),
        Question(
            id = 111,
            description = "Python에서 불변한 순서 있는 항목들의 모음을 표현하는 데이터 타입은?",
            choices = listOf("Set", "List", "Tuple", "Dictionary"),
            correctAnswer = 3
        ),
        Question(
            id = 112,
            description = "Python에서 문자열을 정수로 변환하는 함수는?",
            choices = listOf("float()", "str()", "int()", "char()"),
            correctAnswer = 0
        ),
        Question(
            id = 113,
            description = "Python에서 '5' + 3 의 결과는?",
            choices = listOf("8", "53", "TypeError", "None of the above"),
            correctAnswer = 1
        ),
        Question(
            id = 114,
            description = "Python에서 list.append('value') 의 동작은?",
            choices = listOf("'value' 를 리스트의 시작에 추가", "'value' 를 리스트의 끝에 추가", "리스트의 'value'를 제거", "리스트의 'value' 위치를 반환"),
            correctAnswer = 2
        ),
        Question(
            id = 115,
            description = "Python에서 반복문을 종료하는 명령어는?",
            choices = listOf("exit", "end", "break", "stop"),
            correctAnswer = 3
        )
    )

}
