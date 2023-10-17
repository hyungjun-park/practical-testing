# Practical Testing: 실용적인 테스트

## 단위 테스트
작은 코드 단위를 독립적으로 검증하는 테스트 (클래스 or 메서드)
- 검증 속도가 빠르고, 안정적이다.

## JUnit 5
단위 테스트를 위한 테스트 프레임워크

## AssertJ
테스트 코드 작성을 원활하게 돕는 테스트 라이브러리

## 테스트 케이스 세분화하기
### 해피 케이스
요구 사항에 대한 테스트
### 예외 케이스
요구 사항에서 발생 할 수 있는 예외에 대한 테스트

## Test Layer
### Persistence Layer (Repository)
- Data Access의 역할
- 비즈니스 가공 로직이 포함되어서는 안 된다.
- Data에 대한 CRUD에만 집중한 레이어

### Business Layer (Service)
- 비즈니스 로직을 구현하는 역할
- Persistence Layer와의 상호작용(Data를 읽고 쓰는 행위)을 통해 비즈니스 로직을 전개시킨다.
- **트랜잭션**을 보장해야 한다.

### PresentationLayer
- 외부 요청을 가장 먼저 받는 계층
- 파라미터에 대한 최소한의 검증을 수행한다.

#### MockMvc
- Mock(가짜) 객체를 사용해 스프링 MVC 동작을 재현할 수 있는 테스트 프레임워크