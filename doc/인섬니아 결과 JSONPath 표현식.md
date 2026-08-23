# 인섬니아(Insomnia) 결과 JSONPath 표현식 가이드

본 문서는 인섬니아(Insomnia) Response Preview 창 하단의 **Filter(JSONPath) 입력창**에서 응답 JSON 데이터를 필터링하고 원하는 속성명이나 값을 신속하게 추출하는 주요 JSONPath 표현식을 정리합니다.

---

## 1. 결과 최상위 속성명 확인

응답 결과 객체의 **최상위 Key(속성명) 목록만 배열로 추출**할 때 사용합니다.

```jsonpath
$.*~
```

### 설명
- **`$`**: 루트 객체(Root)
- **`.*`**: 직계 하위의 모든 속성
- **`~`**: 값(Value) 대신 **속성명(Property Key Name)**을 반환하는 JSONPath Plus 특수 연산자

### 출력 예시 (`OffsetElementList` 응답 기준)
```json
[
  "results",
  "totalCount",
  "offset",
  "limit",
  "page",
  "size",
  "totalPages",
  "hasNext",
  "hasPrevious"
]
```

---

## 2. 지정 속성의 value 확인

원하는 **특정 속성의 실제 값(Value)**만 단독으로 추출하여 확인할 때 사용합니다.

### 2.1. 단일 속성 값 확인
- **전체 데이터 건수 (`totalCount`) 확인**:
  ```jsonpath
  $.totalCount
  ```
  > 출력: `48`

- **현재 페이지 번호 (`page`) 확인**:
  ```jsonpath
  $.page
  ```
  > 출력: `1`

- **전체 페이지 수 (`totalPages`) 확인**:
  ```jsonpath
  $.totalPages
  ```
  > 출력: `5`

- **다음 페이지 존재 여부 (`hasNext`) 확인**:
  ```jsonpath
  $.hasNext
  ```
  > 출력: `true`

---

### 2.2. 배열 속성(`results`)의 세부 Value 확인

- **리턴된 데이터 목록(`results`) 전체만 확인**:
  ```jsonpath
  $.results
  ```

- **리턴된 데이터 건수(배열 길이) 확인**:
  ```jsonpath
  $.results.length
  ```
  > 출력: `10`

- **첫 번째(0번째) Row 데이터 객체만 확인**:
  ```jsonpath
  $.results[0]
  ```

- **모든 Row의 특정 필드 목록만 추출 (예: 장비 ID 목록)**:
  ```jsonpath
  $.results[*].equipmentId
  ```
  > 출력: `["PH-NIKON-04", "ETCH-LAM-01", ...]`

- **모든 Row의 다운 코드(`downCode`) 목록만 추출**:
  ```jsonpath
  $.results[*].downCode
  ```
  > 출력: `["HW-PRS-108", "VAC-LEAK-01", ...]`

---

## 3. 요약 치트시트

| 목적 | JSONPath 표현식 | 설명 |
| :--- | :--- | :--- |
| **최상위 속성명(Key) 목록** | `$.*~` | `["results", "totalCount", "page", ...]` |
| **전체 건수 값** | `$.totalCount` | `48` |
| **현재 페이지 번호** | `$.page` | `1` |
| **전체 페이지 수** | `$.totalPages` | `5` |
| **반환된 Row 개수** | `$.results.length` | `10` |
| **첫 번째 Row 객체** | `$.results[0]` | 1번째 데이터 단건 |
| **특정 컬럼 배열 추출** | `$.results[*].컬럼명` | 지정 컬럼 값들의 배열 |
