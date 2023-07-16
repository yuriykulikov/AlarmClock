package com.better.alarm.question

class QuestionListCodingInterview {

    fun getCodingInterviewQuestions(): List<Question> {
        return codingInterviewQuestions
    }

    private val codingInterviewQuestions = listOf(
        Question(
            id = 1,
            description = "Big O notation에서 O(n)은 무엇을 의미하는가?",
            choices = listOf("입력 크기에 따라 알고리즘의 실행 시간이 선형적으로 증가함", "알고리즘의 실행 시간이 항상 일정함", "입력 크기에 따라 알고리즘의 실행 시간이 지수적으로 증가함", "알고리즘의 실행 시간이 입력 크기의 로그에 비례함"),
            correctAnswer = 0
        ),
        Question(
            id = 2,
            description = "정렬되지 않은 배열에서 최빈값(mode)을 찾는 가장 효율적인 알고리즘의 시간 복잡도는 얼마인가?",
            choices = listOf("O(n log n)", "O(n)", "O(n^2)", "O(1)"),
            correctAnswer = 1
        ),
        Question(
            id = 3,
            description = "Binary Search 알고리즘이 가장 잘 작동하는 경우는 어떤 경우인가?",
            choices = listOf("정렬된 배열", "정렬되지 않은 배열", "연결 리스트", "트리"),
            correctAnswer = 2
        ),
        Question(
            id = 4,
            description = "다음 중 재귀 함수에 대한 올바른 설명은?",
            choices = listOf("재귀 함수는 무한한 반복 없이 자기 자신을 호출함", "재귀 함수는 항상 최적의 해결책을 제공함", "재귀 함수는 루프 없이 함수가 자기 자신을 호출함", "모든 함수는 재귀적으로 작성될 수 있음"),
            correctAnswer = 3
        ),
        Question(
            id = 5,
            description = "해시 테이블의 시간 복잡도는 얼마인가?",
            choices = listOf("O(1) for all operations", "O(n) for all operations", "O(n^2) for all operations", "O(log n) for all operations"),
            correctAnswer = 0
        ),
        Question(
            id = 6,
            description = "다음 중 정렬 알고리즘 중 시간 복잡도가 O(n log n)인 것은 무엇인가?",
            choices = listOf("Selection Sort", "Bubble Sort", "Quick Sort", "Insertion Sort"),
            correctAnswer = 1
        ),
        Question(
            id = 7,
            description = "재귀 함수를 사용하여 피보나치 수열을 계산하는 경우의 시간 복잡도는 얼마인가?",
            choices = listOf("O(2^n)", "O(n^2)", "O(n log n)", "O(n)"),
            correctAnswer = 2
        ),
        Question(
            id = 8,
            description = "BFS(Breadth First Search) 알고리즘의 주요 활용 분야는 무엇인가?",
            choices = listOf("데이터 정렬", "가장 짧은 경로 찾기", "이진 검색", "데이터 중복 제거"),
            correctAnswer = 3
        ),
        Question(
            id = 9,
            description = "시간 복잡도가 O(log n)인 알고리즘은 주로 어떤 종류의 문제를 풀기 위해 사용되는가?",
            choices = listOf("검색 알고리즘", "정렬 알고리즘", "다이나믹 프로그래밍 문제", "그래프 문제"),
            correctAnswer = 0
        ),
        Question(
            id = 10,
            description = "'힙 정렬'의 최악의 시간 복잡도는 얼마인가?",
            choices = listOf("O(n log n)", "O(n^2)", "O(n)", "O(1)"),
            correctAnswer = 1
        ),
        Question(
            id = 11,
            description = "다음 중 트리를 순회하는 방법이 아닌 것은 무엇인가?",
            choices = listOf("전위 순회 (Preorder)", "중위 순회 (Inorder)", "후위 순회 (Postorder)", "순환 순회 (Circulate)"),
            correctAnswer = 2
        ),
        Question(
            id = 12,
            description = "다익스트라 알고리즘은 무엇을 찾는데 사용되는가?",
            choices = listOf("최소 신장 트리", "최단 경로", "최대 플로우", "최대 부분 배열"),
            correctAnswer = 3
        ),
        Question(
            id = 13,
            description = "동적 프로그래밍(Dynamic Programming)의 핵심 개념은 무엇인가?",
            choices = listOf("분할 정복", "그리디 알고리즘", "재귀", "중복된 부분 문제의 해결"),
            correctAnswer = 0
        ),
        Question(
            id = 14,
            description = "'머지 소트'의 시간 복잡도는 얼마인가?",
            choices = listOf("O(n^2)", "O(n log n)", "O(n)", "O(1)"),
            correctAnswer = 1
        ),
        Question(
            id = 15,
            description = "이진 트리에서 어떤 노드의 '높이'는 무엇을 의미하는가?",
            choices = listOf("루트에서 노드까지의 경로의 길이", "노드에서 가장 가까운 잎까지의 경로의 길이", "노드에서 가장 먼 잎까지의 경로의 길이", "트리의 전체 노드 수"),
            correctAnswer = 2
        ),
        Question(
            id = 16,
            description = "해시 충돌(hash collision)을 해결하는 방법 중 하나는 무엇인가?",
            choices = listOf("해시 함수 변경", "선형 탐색", "다이나믹 프로그래밍", "체이닝"),
            correctAnswer = 3
        ),
        Question(
            id = 17,
            description = "다음 중 분할 정복 알고리즘에 대한 올바른 설명은?",
            choices = listOf("문제를 분할하고 각각을 독립적으로 해결한 후 결과를 합침", "문제를 분할하고 가장 작은 문제만 해결함", "문제를 분할하지 않고 그대로 해결함", "문제를 분할하고 가장 큰 문제만 해결함"),
            correctAnswer = 0
        ),
        Question(
            id = 18,
            description = "가장 빠른 정렬 알고리즘의 평균 시간 복잡도는 얼마인가?",
            choices = listOf("O(n log n)", "O(n^2)", "O(n)", "O(log n)"),
            correctAnswer = 1
        ),
        Question(
            id = 19,
            description = "그래프에서 '사이클'이란 무엇을 의미하는가?",
            choices = listOf("노드 사이의 모든 경로", "한 노드에서 시작하여 같은 노드로 돌아오는 경로", "가장 긴 경로", "가장 짧은 경로"),
            correctAnswer = 2
        ),
        Question(
            id = 20,
            description = "다음 중 가장 효율적인 정렬 알고리즘이 아닌 것은?",
            choices = listOf("퀵 정렬", "힙 정렬", "병합 정렬", "거품 정렬"),
            correctAnswer = 3
        ),
        Question(
            id = 21,
            description = "다음 중 웹페이지에 자바스크립트를 삽입하는 올바른 방법은?",
            choices = listOf("<script href='xxx.js'>", "<script src='xxx.js'>", "<script link='xxx.js'>", "<script file='xxx.js'>"),
            correctAnswer = 0
        ),
        Question(
            id = 22,
            description = "스택과 큐의 가장 큰 차이점은 무엇인가?",
            choices = listOf("스택은 선입선출(FIFO), 큐는 후입선출(LIFO)", "스택은 후입선출(LIFO), 큐는 선입선출(FIFO)", "스택은 배열 기반, 큐는 리스트 기반", "스택은 단일 연결 리스트, 큐는 이중 연결 리스트"),
            correctAnswer = 1
        ),
        Question(
            id = 23,
            description = "이진 검색 트리에서 최소값을 찾는 올바른 방법은?",
            choices = listOf("루트 노드부터 왼쪽 자식 노드를 따라 계속 이동한다.", "루트 노드부터 오른쪽 자식 노드를 따라 계속 이동한다.", "루트 노드의 값을 찾는다.", "모든 노드를 검색한다."),
            correctAnswer = 2
        ),
        Question(
            id = 24,
            description = "HTML 요소의 가운데 정렬을 위해 가장 흔히 사용되는 CSS 속성은?",
            choices = listOf("center: auto;", "text-align: center;", "align: center;", "margin: auto;"),
            correctAnswer = 3
        ),
        Question(
            id = 25,
            description = "변수의 범위를 결정하는 JavaScript의 키워드는?",
            choices = listOf("var", "let", "const", "모두 가능하다"),
            correctAnswer = 0
        ),
        Question(
            id = 26,
            description = "프로그래밍에서 'DRY 원칙'이란 무엇을 의미하는가?",
            choices = listOf("'Don't Repeat Yourself' - 반복을 피하라", "'Don't Repeat Yourself' - 재사용을 피하라", "'Do Repeat Yourself' - 반복을 하라", "'Do Repeat Yourself' - 재사용을 하라"),
            correctAnswer = 1
        ),
        Question(
            id = 27,
            description = "컴퓨터 프로그래밍에서 '메모리 누수'란 무엇을 의미하는가?",
            choices = listOf("사용하지 않는 메모리를 해제하지 않는 현상", "메모리가 너무 많이 사용되는 현상", "메모리를 너무 적게 사용하는 현상", "메모리가 물을 누설하는 현상"),
            correctAnswer = 2
        ),
        Question(
            id = 28,
            description = "'Callback' 함수란 무엇인가?",
            choices = listOf("특정 이벤트 후에 호출되는 함수", "특정 시간 후에 호출되는 함수", "함수 호출 시 바로 실행되는 함수", "특정 이벤트 이전에 호출되는 함수"),
            correctAnswer = 3
        ),
        Question(
            id = 29,
            description = "알고리즘의 '시간 복잡도'란 무엇을 나타내는가?",
            choices = listOf("알고리즘을 실행하는 데 필요한 공간", "알고리즘을 완료하는 데 필요한 최대 시간", "알고리즘을 작성하는 데 필요한 시간", "알고리즘을 이해하는 데 필요한 시간"),
            correctAnswer = 0
        ),
        Question(
            id = 30,
            description = "'Git'에서 'commit'이란 무엇을 의미하는가?",
            choices = listOf("코드 변경 사항을 로컬 저장소에 저장", "코드 변경 사항을 원격 저장소에 저장", "코드 변경 사항을 임시 저장", "원격 저장소를 로컬 저장소에 복사"),
            correctAnswer = 1
        ),
        Question(
            id = 31,
            description = "기본적인 데이터 구조 중 '리스트'는 어떤 특성을 가지고 있는가?",
            choices = listOf("데이터를 순차적으로 저장", "데이터를 키-값 쌍으로 저장", "데이터를 계층적으로 저장", "데이터를 그래프 형태로 저장"),
            correctAnswer = 0
        ),
        Question(
            id = 32,
            description = "다음 중 '정적 타이핑'을 사용하는 프로그래밍 언어는?",
            choices = listOf("JavaScript", "Python", "C++", "Ruby"),
            correctAnswer = 1
        ),
        Question(
            id = 33,
            description = "재귀 함수의 주요한 특징 중 하나는 무엇인가?",
            choices = listOf("함수가 자신을 호출한다.", "함수가 다른 함수를 호출한다.", "함수가 무한히 호출된다.", "함수가 한 번만 호출된다."),
            correctAnswer = 2
        ),
        Question(
            id = 34,
            description = "다음 중 '빅 오 표기법'으로 표현된 시간 복잡도가 가장 높은 것은?",
            choices = listOf("O(1)", "O(n)", "O(n log n)", "O(n^2)"),
            correctAnswer = 3
        ),
        Question(
            id = 35,
            description = "'MVC' 디자인 패턴에서 'C'는 무엇을 의미하는가?",
            choices = listOf("Component", "Control", "Construct", "Controller"),
            correctAnswer = 0
        ),
        Question(
            id = 36,
            description = "다음 중 '스케일 업(scale-up)'에 가장 가까운 설명은?",
            choices = listOf("하드웨어 성능을 향상시키는 방법", "서버의 수를 늘리는 방법", "데이터 분석의 범위를 넓히는 방법", "코드의 성능을 향상시키는 방법"),
            correctAnswer = 1
        ),
        Question(
            id = 37,
            description = "'REST'는 웹 서비스를 개발할 때 널리 사용되는 아키텍처 스타일이다. 'REST'에서 'R'은 무엇을 의미하는가?",
            choices = listOf("Resource", "Representational", "Relational", "Recursive"),
            correctAnswer = 2
        ),
        Question(
            id = 38,
            description = "다음 중 '함수형 프로그래밍'의 핵심 개념이 아닌 것은?",
            choices = listOf("고차 함수", "상태 변경 없음", "객체 지향", "불변성"),
            correctAnswer = 3
        ),
        Question(
            id = 39,
            description = "'테스트 주도 개발(TDD)'에서 '빨강 - 초록 - 리팩터' 사이클에서 '빨강'은 무엇을 의미하는가?",
            choices = listOf("코드 작성", "테스트 실패", "테스트 성공", "코드 개선"),
            correctAnswer = 0
        ),
        Question(
            id = 40,
            description = "다음 중 소프트웨어 개발 생명 주기(SDLC)의 단계가 아닌 것은?",
            choices = listOf("요구 사항 수집", "설계", "구현", "배포", "회고"),
            correctAnswer = 1
        ),
        Question(
            id = 48,
            description = "다음 중 '배열(Array)'과 '리스트(List)'의 차이점은 무엇인가?",
            choices = listOf("배열은 크기가 고정되지만, 리스트는 크기가 동적으로 조정될 수 있다.", "배열은 인덱스를 사용하여 요소에 접근하지만, 리스트는 포인터를 사용한다.", "배열은 한 종류의 데이터 타입만 저장할 수 있지만, 리스트는 다양한 타입을 저장할 수 있다.", "배열은 순차적인 데이터 저장에 사용되지만, 리스트는 임의의 데이터 저장에 사용된다."),
            correctAnswer = 0
        ),
        Question(
            id = 49,
            description = "다음 중 'BFS(Breadth First Search)' 알고리즘의 동작 방식을 올바르게 나열한 것은?",
            choices = listOf("큐를 사용하여 시작 노드에서부터 탐색을 시작한다.", "스택을 사용하여 탐색을 시작한 노드를 저장한다.", "깊이 우선으로 탐색하며 노드를 방문한다.", "탐색을 시작하는 노드와의 거리를 기준으로 탐색한다."),
            correctAnswer = 1
        ),
        Question(
            id = 50,
            description = "다음 중 '메모이제이션(Memoization)'의 개념에 대한 설명으로 올바른 것은?",
            choices = listOf("이전에 계산한 결과를 저장하여 중복 계산을 피하는 기술", "여러 개의 스레드가 동시에 접근할 수 있는 자원", "특정 객체가 다른 객체에 대해 알고 있는 관계", "소프트웨어 개발 과정에서 발생하는 결함 또는 오류"),
            correctAnswer = 2
        ),
        Question(
            id = 51,
            description = "다음 중 '이진 탐색(Binary Search)' 알고리즘의 시간 복잡도는?",
            choices = listOf("O(1)", "O(n)", "O(log n)", "O(n log n)"),
            correctAnswer = 3
        ),
        Question(
            id = 52,
            description = "다음 중 '트리(Tree)' 구조의 특징이 아닌 것은?",
            choices = listOf("루트 노드를 가지며 루트에서부터 잎까지의 경로가 존재한다.", "사이클을 가지지 않는다.", "계층적인 구조를 가지며 부모-자식 관계를 갖는다.", "중복된 요소를 가질 수 있다."),
            correctAnswer = 0
        ),
        Question(
            id = 53,
            description = "다음 중 '최소 신장 트리(Minimum Spanning Tree)' 알고리즘으로 올바른 것은?",
            choices = listOf("Kruskal 알고리즘", "Dijkstra 알고리즘", "Prim 알고리즘", "DFS 알고리즘"),
            correctAnswer = 1
        ),
        Question(
            id = 54,
            description = "다음 중 '스택(Stack)'의 동작 방식으로 올바른 것은?",
            choices = listOf("후입선출(LIFO)", "선입선출(FIFO)", "노드를 연결하여 순차적으로 데이터를 저장", "이진 탐색을 사용하여 데이터를 검색"),
            correctAnswer = 2
        ),
        Question(
            id = 55,
            description = "다음 중 '너비 우선 탐색(BFS)'과 '깊이 우선 탐색(DFS)'의 차이점은 무엇인가?",
            choices = listOf("BFS는 큐를 사용하고, DFS는 스택을 사용한다.", "BFS는 재귀적으로 동작하고, DFS는 반복적으로 동작한다.", "BFS는 루트 노드에서 시작하고, DFS는 임의의 노드에서 시작한다.", "BFS는 그래프에 사이클이 있으면 무한 루프에 빠진다."),
            correctAnswer = 3
        ),
        Question(
            id = 56,
            description = "다음 중 '버블 정렬(Bubble Sort)' 알고리즘의 시간 복잡도는?",
            choices = listOf("O(1)", "O(n)", "O(n log n)", "O(n^2)"),
            correctAnswer = 0
        ),
        Question(
            id = 57,
            description = "다음 중 '큐(Queue)'의 동작 방식으로 올바른 것은?",
            choices = listOf("선입선출(FIFO)", "후입선출(LIFO)", "노드를 연결하여 순차적으로 데이터를 저장", "이진 탐색을 사용하여 데이터를 검색"),
            correctAnswer = 1
        ),
        Question(
            id = 58,
            description = "다음 중 '버전 관리 시스템'으로 많이 사용되는 분산 버전 관리 시스템은?",
            choices = listOf("Git", "SVN", "Mercurial", "Perforce"),
            correctAnswer = 2
        ),
        Question(
            id = 59,
            description = "다음 중 '애자일(Agile) 개발 방법론'에 대한 설명으로 올바른 것은?",
            choices = listOf("계획을 강조하며 변화를 어렵게 한다.", "문서화를 최대화하여 팀 간 협력을 강조한다.", "고객과의 협력과 변화에 대한 대응을 강조한다.", "일정과 예산을 엄격하게 준수해야 한다."),
            correctAnswer = 3
        ),
        Question(
            id = 60,
            description = "다음 중 '동기식(Synchronous)'과 '비동기식(Asynchronous)'의 차이점은 무엇인가?",
            choices = listOf("동기식은 작업이 완료될 때까지 대기하지만, 비동기식은 작업이 완료되기를 기다리지 않는다.", "동기식은 작업을 동시에 처리하지만, 비동기식은 작업을 순차적으로 처리한다.", "동기식은 실시간 처리에 사용되지만, 비동기식은 배치 처리에 사용된다.", "동기식은 단일 스레드에서 동작하지만, 비동기식은 다중 스레드에서 동작한다."),
            correctAnswer = 0
        ),
        Question(
            id = 61,
            description = "다음 중 '자료구조'의 종류가 아닌 것은?",
            choices = listOf("스택(Stack)", "힙(Heap)", "그래프(Graph)", "알고리즘(Algorithm)"),
            correctAnswer = 3
        ),
        Question(
            id = 62,
            description = "다음 중 '데이터베이스 인덱스(Database Index)'에 대한 설명으로 올바른 것은?",
            choices = listOf("데이터베이스 테이블에서 중복된 값을 제거하는 기능", "데이터베이스에 저장된 데이터를 암호화하는 기능", "데이터베이스 테이블 간의 관계를 정의하는 기능", "데이터베이스의 검색 성능을 향상시키는 기능"),
            correctAnswer = 1
        ),
        Question(
            id = 63,
            description = "다음 중 '컴파일러(Compiler)'의 역할이 아닌 것은?",
            choices = listOf("프로그램 코드를 기계어로 변환한다.", "문법 오류를 검사하고 수정한다.", "프로그램을 실행하기 위해 필요한 런타임 환경을 제공한다.", "효율적인 기계어 코드를 생성한다."),
            correctAnswer = 2
        ),
        Question(
            id = 64,
            description = "다음 중 '조인(Join)'에 대한 설명으로 올바른 것은?",
            choices = listOf("데이터베이스에서 특정 조건을 만족하는 레코드를 선택한다.", "데이터베이스 테이블에서 특정 열을 선택한다.", "데이터베이스의 스키마를 수정한다.", "두 개 이상의 테이블을 연결하여 결과를 가져온다."),
            correctAnswer = 3
        ),
        Question(
            id = 65,
            description = "다음 중 '자연어 처리(Natural Language Processing)'에 대한 설명으로 올바른 것은?",
            choices = listOf("텍스트 데이터의 특징을 추출하여 데이터를 분류하는 기술", "컴퓨터 비전 기술을 사용하여 이미지를 분석하는 기술", "사용자의 음성을 인식하고 이해하여 명령을 수행하는 기술", "인간의 언어를 이해하고 생성하는 기술"),
            correctAnswer = 0
        ),
        Question(
            id = 66,
            description = "다음 중 '해시 함수(Hash Function)'의 특징이 아닌 것은?",
            choices = listOf("고정된 길이의 해시 값을 반환한다.", "동일한 입력에 대해 항상 동일한 해시 값을 반환한다.", "출력된 해시 값은 원래 데이터를 복구할 수 있다.", "다양한 입력에 대해 고르게 분포된 해시 값을 반환한다."),
            correctAnswer = 2
        ),
        Question(
            id = 67,
            description = "다음 중 '배열(Array)'과 '링크드 리스트(Linked List)'의 차이점은 무엇인가?",
            choices = listOf("배열은 삽입과 삭제에 비용이 크지만, 링크드 리스트는 작다.", "배열은 메모리를 연속적으로 사용하지만, 링크드 리스트는 비연속적으로 사용한다.", "배열은 임의의 위치에 빠른 접근이 가능하지만, 링크드 리스트는 불가능하다.", "배열은 동일한 데이터 타입만 저장할 수 있지만, 링크드 리스트는 다양한 타입을 저장할 수 있다."),
            correctAnswer = 3
        ),
        Question(
            id = 68,
            description = "다음 중 '데이터베이스 트랜잭션(Database Transaction)'에 대한 설명으로 올바른 것은?",
            choices = listOf("데이터베이스 테이블에서 특정 행을 선택한다.", "데이터베이스 스키마를 변경한다.", "한 개 이상의 작업을 묶어서 원자적으로 실행하는 것", "데이터베이스의 인덱스를 생성한다."),
            correctAnswer = 0
        ),
        Question(
            id = 69,
            description = "다음 중 '정렬 알고리즘(Sorting Algorithm)' 중 '퀵 정렬(Quick Sort)'의 평균 시간 복잡도는?",
            choices = listOf("O(1)", "O(n)", "O(n log n)", "O(n^2)"),
            correctAnswer = 1
        ),
        Question(
            id = 70,
            description = "다음 중 'HTTP(HyperText Transfer Protocol)'의 역할이 아닌 것은?",
            choices = listOf("웹 페이지의 구조와 스타일을 정의한다.", "웹 서버와 클라이언트 간의 통신을 담당한다.", "웹 리소스의 요청과 응답을 처리한다.", "웹 페이지의 데이터를 전송한다."),
            correctAnswer = 2
        ),
        Question(
            id = 71,
            description = "다음 중 '스코프(Scope)'에 대한 설명으로 올바른 것은?",
            choices = listOf("변수가 정의되어 사용될 수 있는 범위", "함수가 호출되는 순서에 따라 실행되는 영역", "객체의 메모리 할당과 해제를 관리하는 영역", "변수의 유효한 값을 저장하는 공간"),
            correctAnswer = 3
        ),
        Question(
            id = 72,
            description = "다음 중 'UDP(User Datagram Protocol)'에 대한 설명으로 올바른 것은?",
            choices = listOf("신뢰성 있는 데이터 전송을 보장한다.", "순서대로 데이터를 전송한다.", "데이터를 작은 패킷 단위로 전송한다.", "데이터 전송 시 확인 응답을 기다리지 않는다."),
            correctAnswer = 0
        ),
        Question(
            id = 73,
            description = "다음 중 '스프린트(Sprint)'에 대한 설명으로 올바른 것은?",
            choices = listOf("애자일 개발 방법론에서 일정 기간 동안 진행되는 개발 단위", "웹 사이트의 디자인과 사용자 경험을 개선하는 작업", "프로그래밍 언어로 작성된 소프트웨어의 기능을 검증하는 작업", "프로젝트의 초기 단계에서 요구 사항을 수집하는 작업"),
            correctAnswer = 1
        ),
        Question(
            id = 74,
            description = "다음 중 '피보나치 수열(Fibonacci Sequence)'을 계산하는 재귀 함수의 시간 복잡도는?",
            choices = listOf("O(1)", "O(n)", "O(n log n)", "O(2^n)"),
            correctAnswer = 2
        ),
        Question(
            id = 75,
            description = "다음 중 '오버로딩(Overloading)'에 대한 설명으로 올바른 것은?",
            choices = listOf("하나의 클래스에서 여러 개의 메소드를 정의하는 것", "하나의 메소드가 다른 타입의 매개변수를 사용하는 것", "상속 관계에 있는 클래스 간에 메소드를 공유하는 것", "하나의 메소드가 다른 메소드를 호출하는 것"),
            correctAnswer = 3
        ),
        Question(
            id = 76,
            description = "다음 중 '버전 관리 시스템' 중 분산 버전 관리 시스템은?",
            choices = listOf("Git", "SVN", "Mercurial", "Perforce"),
            correctAnswer = 0
        ),
        Question(
            id = 77,
            description = "다음 중 '소프트웨어 개발 생명 주기(SDLC)'의 단계가 아닌 것은?",
            choices = listOf("요구 사항 수집", "설계", "구현", "배포", "회고"),
            correctAnswer = 1
        ),
        Question(
            id = 78,
            description = "다음 중 '링크드 리스트(Linked List)'의 장점이 아닌 것은?",
            choices = listOf("크기가 동적으로 조정될 수 있다.", "중간에 요소를 삽입하거나 삭제하기 용이하다.", "메모리를 효율적으로 사용한다.", "데이터의 연속적인 접근이 가능하다."),
            correctAnswer = 3
        ),
        Question(
            id = 79,
            description = "다음 중 '빅 오 표기법(Big O Notation)'에서 O(1)은 무엇을 의미하는가?",
            choices = listOf("최악의 경우에도 일정한 실행 시간을 갖는다.", "입력 크기에 따라 실행 시간이 선형적으로 증가한다.", "입력 크기에 상관없이 일정한 실행 시간을 갖는다.", "실행 시간이 입력 크기의 제곱에 비례하여 증가한다."),
            correctAnswer = 2
        ),
        Question(
            id = 80,
            description = "다음 중 '재귀 함수(Recursive Function)'의 특징으로 올바른 것은?",
            choices = listOf("메모리를 더 많이 사용하며 실행 속도가 느리다.", "반복문을 사용하지 않고 자기 자신을 호출한다.", "함수의 호출 순서에 따라 결과가 달라진다.", "다른 함수를 호출하여 처리하는 것보다 간단하다."),
            correctAnswer = 1
        ),
        Question(
            id = 81,
            description = "다음 중 '최소 공통 조상(Lowest Common Ancestor)'에 대한 설명으로 올바른 것은?",
            choices = listOf("두 개의 트리에서 동일한 값을 가지는 노드를 찾는 것", "그래프에서 두 개의 노드 사이의 가장 가까운 공통 조상을 찾는 것", "두 개의 문자열에서 공통으로 나타나는 가장 긴 부분 문자열을 찾는 것", "두 개의 정렬된 배열에서 공통된 요소를 찾는 것"),
            correctAnswer = 1
        ),
        Question(
            id = 82,
            description = "다음 중 '스레드(Thread)'에 대한 설명으로 올바른 것은?",
            choices = listOf("하나의 프로세스 내에서 동시에 실행되는 여러 개의 코드 단위", "각 스레드는 독립적인 메모리 공간을 가진다.", "스레드 간에는 데이터를 공유할 수 없다.", "스레드는 항상 동기적으로 실행된다."),
            correctAnswer = 0
        ),
        Question(
            id = 83,
            description = "다음 중 '해시 테이블(Hash Table)'의 장점이 아닌 것은?",
            choices = listOf("빠른 검색 속도", "메모리를 효율적으로 사용", "데이터의 순서를 보장", "고정된 시간 복잡도"),
            correctAnswer = 2
        ),
        Question(
            id = 84,
            description = "다음 중 '정규 표현식(Regular Expression)'에서 사용되는 기호로 올바른 것은?",
            choices = listOf("&, *, +, -", "@, $, !, =", "^, |, ~, ?", "[, ], {, }"),
            correctAnswer = 1
        ),
        Question(
            id = 85,
            description = "다음 중 '데드락(Deadlock)'에 대한 설명으로 올바른 것은?",
            choices = listOf("하나의 프로세스가 다른 프로세스의 자원을 독점하는 상태", "두 개 이상의 프로세스가 서로 상호 작용하여 작업을 수행하는 상태", "프로세스가 실행을 완료하거나 다른 프로세스에 의해 강제로 중단되는 상태", "프로세스가 서로의 작업이 끝날 때까지 무한히 대기하는 상태"),
            correctAnswer = 3
        ),
        Question(
            id = 86,
            description = "다음 중 '프로세스(Process)'의 특징이 아닌 것은?",
            choices = listOf("하나 이상의 스레드로 구성된다.", "독립적인 메모리 공간을 가진다.", "자원을 사용하고 관리한다.", "다른 프로세스의 자원에 접근할 수 있다."),
            correctAnswer = 3
        ),
        Question(
            id = 87,
            description = "다음 중 '맵(Map)'과 '셋(Set)'의 차이점은 무엇인가?",
            choices = listOf("맵은 키와 값의 쌍으로 데이터를 저장하고, 셋은 단일 값을 저장한다.", "맵은 중복된 키를 허용하지 않고, 셋은 중복된 값을 허용한다.", "맵은 순서를 보장하지 않고, 셋은 정렬된 순서로 저장된다.", "맵은 해시 테이블로 구현되고, 셋은 이진 검색 트리로 구현된다."),
            correctAnswer = 0
        ),
        Question(
            id = 88,
            description = "다음 중 '디자인 패턴(Design Pattern)'에 대한 설명으로 올바른 것은?",
            choices = listOf("일반적인 문제에 대한 해결 방법을 제시하는 표준화된 설계 방식", "프로그램의 구조와 동작을 정의하는 기술적인 문서", "소프트웨어 개발 과정에서 발생하는 결함 또는 오류", "코드를 구조화하여 재사용성과 유지 보수성을 향상시키는 기술"),
            correctAnswer = 3
        ),
        Question(
            id = 89,
            description = "다음 중 '스택 오버플로우(Stack Overflow)'에 대한 설명으로 올바른 것은?",
            choices = listOf("스택 영역에 할당된 메모리를 넘어서는 데이터를 저장하려고 할 때 발생한다.", "힙 영역에 할당된 메모리를 넘어서는 데이터를 저장하려고 할 때 발생한다.", "메소드 호출이 너무 깊게 중첩되어 스택 영역을 초과할 때 발생한다.", "프로그램의 실행 시간이 너무 길어져 스택 영역이 꽉 찰 때 발생한다."),
            correctAnswer = 2
        ),
        Question(
            id = 90,
            description = "다음 중 'ORM(Object-Relational Mapping)'에 대한 설명으로 올바른 것은?",
            choices = listOf("객체 지향 프로그래밍 언어와 관계형 데이터베이스 간의 변환을 자동화하는 기술", "웹 애플리케이션의 사용자 인터페이스를 개발하는 기술", "데이터베이스에서 데이터를 읽고 쓰는 기능을 제공하는 API", "소프트웨어의 버전을 관리하고 배포하는 기술"),
            correctAnswer = 0
        ),
        Question(
            id = 91,
            description = "다음 중 '소켓(Socket)'에 대한 설명으로 올바른 것은?",
            choices = listOf("네트워크 상에서 컴퓨터 간에 데이터를 전송하기 위한 연결점", "웹 페이지의 구조와 스타일을 정의하는 기술", "데이터베이스의 스키마를 변경하는 기능", "웹 애플리케이션의 사용자 인터페이스를 개발하는 기능"),
            correctAnswer = 0
        ),
        Question(
            id = 92,
            description = "다음 중 '백트래킹(Backtracking)' 알고리즘의 동작 방식으로 올바른 것은?",
            choices = listOf("재귀적으로 모든 가능한 해를 탐색하고, 조건을 만족하는 해를 찾는다.", "데이터를 작은 단위로 분할하여 순차적으로 처리한다.", "탐색을 시작하는 노드와의 거리를 기준으로 탐색한다.", "탐색 과정에서 유망하지 않은 분기를 가지치기하여 탐색 시간을 줄인다."),
            correctAnswer = 3
        ),
        Question(
            id = 93,
            description = "다음 중 '소프트웨어 테스트(Software Testing)'의 목적이 아닌 것은?",
            choices = listOf("프로그램의 버그를 찾아서 수정하는 것", "프로그램이 요구사항을 충족하는지 확인하는 것", "프로그램이 안정적으로 동작하는지 확인하는 것", "프로그램의 성능을 측정하고 향상시키는 것"),
            correctAnswer = 3
        ),
        Question(
            id = 94,
            description = "다음 중 '스프링 프레임워크(Spring Framework)'의 특징이 아닌 것은?",
            choices = listOf("경량 컨테이너로서 자바 객체의 생성과 관리를 담당한다.", "AOP(Aspect-Oriented Programming)를 지원하여 관점 지향 프로그래밍을 구현할 수 있다.", "DI(Dependency Injection)를 사용하여 객체 간의 의존 관계를 해결한다.", "ORM(Object-Relational Mapping) 기능을 제공하여 데이터베이스와의 연동을 간편하게 처리할 수 있다."),
            correctAnswer = 3
        ),
        Question(
            id = 95,
            description = "다음 중 '빌더 패턴(Builder Pattern)'의 특징으로 올바른 것은?",
            choices = listOf("복잡한 객체의 생성 과정을 단순화하고 객체를 조립하여 생성하는 방식", "객체 간의 상속 관계를 통해 유연한 설계를 구현하는 방식", "실행 시점에 객체의 타입을 결정하여 동작을 다르게 하는 방식", "한 객체의 상태 변화에 따라 다른 객체의 동작을 변경하는 방식"),
            correctAnswer = 0
        ),
        Question(
            id = 96,
            description = "다음 중 '소프트웨어 아키텍처(Software Architecture)'의 특징이 아닌 것은?",
            choices = listOf("소프트웨어 시스템의 구성 요소를 정의하고 조직화하는 것", "시스템의 전체적인 동작을 제어하고 조율하는 것", "개발된 소프트웨어의 성능을 측정하고 최적화하는 것", "시스템의 유연성, 확장성, 유지 보수성을 고려하는 것"),
            correctAnswer = 2
        ),
        Question(
            id = 97,
            description = "다음 중 '클로저(Closure)'에 대한 설명으로 올바른 것은?",
            choices = listOf("내부 함수에서 외부 함수의 변수를 참조하는 것", "클래스를 인스턴스화하여 객체를 생성하는 것", "객체의 상태를 외부에서 변경하는 것", "객체 간의 관계를 정의하는 것"),
            correctAnswer = 0
        ),
        Question(
            id = 98,
            description = "다음 중 '웹 서버(Web Server)'의 역할이 아닌 것은?",
            choices = listOf("웹 페이지의 구조와 스타일을 정의한다.", "웹 페이지의 데이터를 요청하고 응답한다.", "웹 애플리케이션과의 통신을 담당한다.", "정적인 웹 콘텐츠를 제공한다."),
            correctAnswer = 0
        ),
        Question(
            id = 99,
            description = "다음 중 '빅 엔드(Big-endian)'와 '리틀 엔드(Little-endian)'의 차이점은 무엇인가?",
            choices = listOf("데이터를 나타내는 비트의 순서", "데이터를 저장하는 메모리 공간의 크기", "데이터의 압축 방식", "데이터를 처리하는 방식"),
            correctAnswer = 0
        ),
        Question(
            id = 100,
            description = "다음 중 '스레드 풀(Thread Pool)'에 대한 설명으로 올바른 것은?",
            choices = listOf("작업을 순차적으로 처리하는 스레드의 집합", "스레드 간의 동기화를 관리하는 기술", "스레드의 생성과 소멸을 자동으로 관리하는 기술", "멀티코어 시스템에서 스레드의 실행을 동시에 처리하는 기술"),
            correctAnswer = 2
        )



    )
}
