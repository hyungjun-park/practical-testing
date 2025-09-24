# Practical Testing 저장소 빠른 레퍼런스

실무에서 테스트를 작성하다가 “이런 경우 어떻게 검증했더라?” 하고 떠올릴 수 있도록, 저장소에 이미 작성된 테스트를 상황별로 묶어서 정리했습니다. 각 항목은 **언제(상황)**, **어떤 테스트 타입을 선택했는지**, **검증 포인트/패턴**을 중심으로 설명합니다.

## 1. 한눈에 보는 테스트 맵
| 상황 | 사용한 테스트 | 핵심 포인트 | 참고 테스트 |
| --- | --- | --- | --- |
| 주문 도메인 합계·상태 계산 | 순수 JUnit 단위 테스트 | `Order.create` 결과 필드 단언 | `OrderTest`【F:src/test/java/simple/cafekiosk/spring/domain/order/OrderTest.java†L16-L65】 |
| 재고 차감 로직 | 순수 JUnit + `@TestFactory` | 성공/실패 시나리오 동적 테스트 | `StockTest`【F:src/test/java/simple/cafekiosk/spring/domain/stock/StockTest.java†L16-L78】 |
| 판매 상품/상품번호 조회 | `@SpringBootTest` 리포지토리 통합 | 슬라이스 대신 실제 빈 로딩, `tuple` 검증 | `ProductRepositoryTest`【F:src/test/java/simple/cafekiosk/spring/domain/product/ProductRepositoryTest.java†L17-L88】 |
| 상품 재고 조회 | `@DataJpaTest` 슬라이스 | `findAllBy...In` 쿼리 결과 추출 비교 | `StockRepositoryTest`【F:src/test/java/simple/cafekiosk/spring/domain/stock/StockRepositoryTest.java†L13-L38】 |
| 주문 생성 서비스 | `@SpringBootTest` + 실 DB | 재고 차감/예외, 중복 상품 케이스 | `OrderServiceTest`【F:src/test/java/simple/cafekiosk/spring/api/service/order/OrderServiceTest.java†L35-L161】 |
| 상품 등록 서비스 | `@SpringBootTest` + `@Transactional` | 신규 상품번호 할당, 빈 저장소 케이스 | `ProductServiceTest`【F:src/test/java/simple/cafekiosk/spring/api/service/product/ProductServiceTest.java†L23-L94】 |
| 메일 발송 협력 객체 | Mockito 단위 테스트 | `@Spy` + `doReturn`, `verify`로 부수효과 확인 | `MailServiceTest`【F:src/test/java/simple/cafekiosk/spring/api/service/mail/MailServiceTest.java†L19-L65】 |
| 매출 통계 집계 | `@SpringBootTest` + `@MockBean` | 기간 필터링, 저장 이력 검증 | `OrderStatisticsServiceTest`【F:src/test/java/simple/cafekiosk/spring/api/service/order/OrderStatisticsServiceTest.java†L24-L116】 |
| 제품/주문 API 검증 | `@WebMvcTest` | JSON 요청/응답, 필드별 검증 | `ProductControllerTest`, `OrderControllerTest`【F:src/test/java/simple/cafekiosk/spring/api/controller/product/ProductControllerTest.java†L26-L142】【F:src/test/java/simple/cafekiosk/spring/api/controller/order/OrderControllerTest.java†L23-L74】 |
| enum 분기 검증 | JUnit 파라미터화 테스트 | `@CsvSource`, `@MethodSource` | `ProductTypeTest`【F:src/test/java/simple/cafekiosk/spring/domain/product/ProductTypeTest.java†L16-L70】 |

## 2. 도메인 로직 테스트 방법
- **주문 합계/상태 확인**: 빌더로 만든 `Product` 리스트를 `Order.create`에 전달한 뒤 총 금액·상태·등록 시간을 AssertJ로 검증합니다.【F:src/test/java/simple/cafekiosk/spring/domain/order/OrderTest.java†L18-L63】
- **재고 차감 시나리오**: `Stock.create`로 기본 상태를 만들고, 성공/실패 케이스를 각각 `@Test`로, 여러 조건을 묶을 땐 `@TestFactory`로 동적 테스트를 구성합니다.【F:src/test/java/simple/cafekiosk/spring/domain/stock/StockTest.java†L18-L78】
- **enum 분기**: 동일 로직을 다양한 입력으로 확인해야 할 땐 `@CsvSource`, `@MethodSource`와 같은 파라미터화 도구를 활용합니다.【F:src/test/java/simple/cafekiosk/spring/domain/product/ProductTypeTest.java†L33-L67】

## 3. 데이터 접근 계층(JPA)
- **재고 조회만 필요한 경우** `@DataJpaTest`를 사용하여 슬라이스로 빠르게 검증하고, `extracting` + `tuple` 로 복합 필드를 비교합니다.【F:src/test/java/simple/cafekiosk/spring/domain/stock/StockRepositoryTest.java†L13-L38】
- **다양한 쿼리 동작/트랜잭션이 필요한 경우** `@SpringBootTest`와 `@Transactional`을 조합해 실제 빈을 사용합니다. 판매 상태 필터, 상품번호 리스트 조회, 최근 상품번호 조회 등의 시나리오를 각각 독립 테스트로 분리합니다.【F:src/test/java/simple/cafekiosk/spring/domain/product/ProductRepositoryTest.java†L17-L88】

## 4. 서비스 레이어 통합 시나리오
- **주문 생성 플로우**: `OrderServiceTest`는 등록 요청을 만들고, 중복 상품, 재고 차감, 재고 부족 예외까지 순차적으로 커버합니다. 테스트 후 `@AfterEach`에서 `deleteAllInBatch`로 테이블을 정리합니다.【F:src/test/java/simple/cafekiosk/spring/api/service/order/OrderServiceTest.java†L35-L161】
- **상품 등록 규칙**: 최신 상품번호를 기준으로 증가시키는 로직과, 상품이 없을 때의 기본값(`001`)을 각각 케이스로 나눠 검증합니다.【F:src/test/java/simple/cafekiosk/spring/api/service/product/ProductServiceTest.java†L35-L90】

## 5. 외부 협력 & 배치성 로직
- **메일 전송과 기록**: Mockito 확장을 적용하고, 실제 구현을 절반만 쓰고 싶은 경우 `@Spy`와 `doReturn`으로 일부 동작을 스텁 합니다. 이후 `verify`로 저장소 호출 횟수를 검증합니다.【F:src/test/java/simple/cafekiosk/spring/api/service/mail/MailServiceTest.java†L19-L65】
- **매출 통계 집계**: 통합 테스트로 주문과 상품을 저장한 뒤, 통계 대상 기간을 걸러내고 `@MockBean`으로 외부 메일 발송을 대체합니다. 실행 후 `MailSendHistory` 저장 여부와 메시지를 확인합니다.【F:src/test/java/simple/cafekiosk/spring/api/service/order/OrderStatisticsServiceTest.java†L24-L116】

## 6. 웹 계층 테스트 패턴
- **`@WebMvcTest` 구성**: `MockMvc`/`ObjectMapper`를 주입받고, 서비스는 `@MockBean`으로 대체합니다.【F:src/test/java/simple/cafekiosk/spring/api/controller/product/ProductControllerTest.java†L26-L40】
- **요청 검증**: 필수 필드 누락, 값 범위 오류 등을 JSON 응답의 `code`, `message`, `data` 필드까지 세밀하게 단언합니다.【F:src/test/java/simple/cafekiosk/spring/api/controller/product/ProductControllerTest.java†L64-L140】
- **정상 플로우 확인**: 주문 API는 성공 응답 코드·메시지를 중심으로 검증하고, 유효성 실패 시 적절한 에러 메시지를 검사합니다.【F:src/test/java/simple/cafekiosk/spring/api/controller/order/OrderControllerTest.java†L41-L72】

## 7. 테스트 픽스처 관리 팁
- 각 테스트 클래스는 `Product.builder()` 등 빌더 메서드를 활용해 읽기 쉬운 픽스처를 구성합니다.【F:src/test/java/simple/cafekiosk/spring/api/service/order/OrderServiceTest.java†L118-L161】
- 반복되는 픽스처는 private 헬퍼 메서드로 추출하여 다른 테스트에서도 재사용합니다.【F:src/test/java/simple/cafekiosk/spring/api/service/product/ProductServiceTest.java†L70-L92】
- 통합 테스트에서는 `@AfterEach` 혹은 `deleteAllInBatch`로 데이터 정리를 습관화하여 테스트 간 간섭을 방지합니다.【F:src/test/java/simple/cafekiosk/spring/api/service/order/OrderServiceTest.java†L44-L50】

필요한 상황을 위 표에서 찾은 뒤, 해당 테스트 파일의 구현을 바로 열어보면 유사한 실무 시나리오를 빠르게 재현할 수 있습니다.
