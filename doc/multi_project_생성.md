##
### 1. muti project 생성
  * inform_note-boot 
    - boot application 위치
  * inform_note-base
    - Jackson 기반 범용 `JsonTypeHandler` 구현
    - List 컬럼 전용 TypeHandler
  * inform_note-domain
    - DownEventLog 클래스
    - DownContent 클래스
    - Value Object 클래스
    - Enum 클래스
    - domain 기본 CRUD logic 클래스
    - Mybatis Mapper 인터페이스
  * inform_note-feature
    - domain 기본 CRUD logic 조합, 복잡한 로직 추가 서비스 클래스 
  * inform_note-facade
    - API 엔트리 포인트 및 서비스 클래스 호출 
  * inform_note-message
    - consumer 이벤트 핸들러 정의
    - producer 이벤트 Proxy 정의