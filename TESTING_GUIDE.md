# Practical Testing 저장소 빠른 레퍼런스

실무에서 테스트를 작성할 때 "이럴 때 어떻게 검증했더라?" 하고 떠올릴 수 있도록, 저장소의 대표 테스트를 상황별로 요약했습니다. 각 항목은 **언제 이 패턴을 고르는지**, **테스트 구성**과 **핵심 검증 코드**를 함께 제공합니다.

## 1. 한눈에 보는 테스트 시나리오 맵
| 상황 | 선택한 테스트 | 핵심 포인트 | 대표 코드 |
| --- | --- | --- | --- |
| 주문 도메인 합계·상태 계산 | 순수 JUnit 단위 테스트 | 빌더로 픽스처 구성 후 값 단언 | [Order 도메인 단위 테스트](#order-도메인-단위-테스트) |
| 재고 차감 로직 | 순수 JUnit + `@TestFactory` | 성공/실패 케이스를 동적 테스트로 묶기 | [Stock 동적 테스트](#stock-동적-테스트) |
| enum 분기 검증 | JUnit 파라미터화 테스트 | `@CsvSource`, `@MethodSource`로 다양한 입력 확인 | [ProductType 파라미터화 테스트](#producttype-파라미터화-테스트) |
| 상품 재고 조회 | `@DataJpaTest` | 슬라이스 구성 + `tuple` 단언 | [재고 리포지토리 슬라이스 테스트](#재고-리포지토리-슬라이스-테스트) |
| 판매 상품/상품번호 조회 | `@SpringBootTest` + `@Transactional` | 실제 빈 주입, 여러 쿼리 검증 | [상품 리포지토리 통합 테스트](#상품-리포지토리-통합-테스트) |
| 주문 생성 서비스 | `@SpringBootTest` | 재고 차감, 예외 흐름까지 한 번에 검증 | [주문 서비스 통합 테스트](#주문-서비스-통합-테스트) |
| 상품 등록 서비스 | `@SpringBootTest` | 상품번호 증가/빈 저장소 케이스 | [상품 서비스 통합 테스트](#상품-서비스-통합-테스트) |
| 메일 발송 협력 객체 | Mockito 단위 테스트 | `@Spy` + `doReturn`, `verify` 조합 | [메일 서비스 단위 테스트](#메일-서비스-단위-테스트) |
| 매출 통계 집계 | `@SpringBootTest` + `@MockBean` | 기간 필터 + 외부 의존성 대체 | [주문 통계 배치 테스트](#주문-통계-배치-테스트) |
| 제품/주문 API 검증 | `@WebMvcTest` | `MockMvc` 요청/응답, JSON 필드 단언 | [웹 계층 테스트](#웹-계층-테스트) |

## 2. Order 도메인 단위 테스트
- **언제 사용?** 순수 도메인 객체의 계산/상태 변화를 외부 의존성 없이 검증할 때.
- **구성 팁**: 빌더로 읽기 쉬운 픽스처를 만들고, `Order.create` 호출 결과를 AssertJ로 단언합니다.

```java
@DisplayName("주문 생성 시 상품 리스트에서 주문의 총 금액을 계산한다.")
@Test
void calculateTotalPrice() {
    // given
    List<Product> products = List.of(
            createProduct("001", 1000),
            createProduct("002", 2000)
    );

    // when
    Order order = Order.create(products, LocalDateTime.now());

    // then
    assertThat(order.getTotalPrice()).isEqualTo(3000);
}
```

## 3. Stock 동적 테스트
- **언제 사용?** 동일한 준비 상태에서 성공/실패 흐름을 한 번에 검증하고 싶을 때.
- **구성 팁**: 정적 테스트로 기본 동작을 확인한 뒤, `@TestFactory`로 다양한 시나리오를 `DynamicTest` 목록에 담습니다.

```java
@DisplayName("재고 차감 시나리오")
@TestFactory
Collection<DynamicTest> stockDeductionDynamicTest() {
    // given
    Stock stock = Stock.create("001", 1);

    return List.of(
            DynamicTest.dynamicTest("재고를 주어진 개수만큼 차감할 수 있다.", () -> {
                int quantity = 1;
                stock.deductQuantity(quantity);
                assertThat(stock.getQuantity()).isZero();
            }),
            DynamicTest.dynamicTest("재고보다 많은 수의 수량으로 차감 시도하는 경우 예외가 발생한다.", () -> {
                int quantity = 1;
                assertThatThrownBy(() -> stock.deductQuantity(quantity))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("차감할 재고 수량이 없습니다.");
            })
    );
}
```

## 4. ProductType 파라미터화 테스트
- **언제 사용?** 입력값이 여러 개지만 로직은 동일한 enum 분기/유틸 메서드를 검증할 때.
- **구성 팁**: `@CsvSource` 나 `@MethodSource` 를 사용하면 한 메서드에서 다양한 케이스를 커버할 수 있습니다.

```java
@DisplayName("상품 타입이 재고 관련 타입인지를 체크한다.")
@CsvSource({"HANDMADE,false", "BOTTLE,true", "BAKERY,true"})
@ParameterizedTest
void containsStockType(ProductType productType, boolean expected) {
    boolean result = ProductType.containsStockType(productType);
    assertThat(result).isEqualTo(expected);
}
```

## 5. 재고 리포지토리 슬라이스 테스트
- **언제 사용?** 단일 리포지토리의 JPA 쿼리만 빠르게 검증하고 싶을 때.
- **구성 팁**: `@DataJpaTest`로 슬라이스를 올리고, `tuple`을 활용해 복합 필드를 한 번에 비교합니다.

```java
@DataJpaTest
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @DisplayName("상품번호 리스트로 재고를 조회한다.")
    @Test
    void findAllByProductNumberIn() {
        stockRepository.saveAll(List.of(
                Stock.create("001", 1),
                Stock.create("002", 2),
                Stock.create("003", 3)
        ));

        List<Stock> stocks = stockRepository.findAllByProductNumberIn(List.of("001", "002"));

        assertThat(stocks).hasSize(2)
                .extracting("productNumber", "quantity")
                .containsExactlyInAnyOrder(
                        tuple("001", 1),
                        tuple("002", 2)
                );
    }
}
```

## 6. 상품 리포지토리 통합 테스트
- **언제 사용?** 여러 엔티티/트랜잭션 경계를 포함하는 실제 쿼리 동작을 확인해야 할 때.
- **구성 팁**: `@SpringBootTest` + `@Transactional`로 스프링 컨텍스트를 전부 올리고, 필요한 시나리오를 테스트 메서드 단위로 분리합니다.

```java
@ActiveProfiles("test")
@Transactional
@SpringBootTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("원하는 판매상태를 가진 상품들을 조회한다.")
    @Test
    void findAllBySellingStatusIn() {
        productRepository.saveAll(List.of(
                createProduct("001", HANDMADE, SELLING, "아메리카노", 4000),
                createProduct("002", HANDMADE, HOLD, "카페라떼", 4500),
                createProduct("003", HANDMADE, STOP_SELLING, "팥빙수", 7000)
        ));

        List<Product> products = productRepository.findAllBySellingStatusIn(List.of(SELLING, HOLD));

        assertThat(products).hasSize(2)
                .extracting("productNumber", "name", "sellingStatus")
                .containsExactlyInAnyOrder(
                        tuple("001", "아메리카노", SELLING),
                        tuple("002", "카페라떼", HOLD)
                );
    }
}
```

## 7. 주문 서비스 통합 테스트
- **언제 사용?** 서비스 계층에서 여러 리포지토리·도메인 객체가 협력하며 동작하는 흐름을 검증할 때.
- **구성 팁**: 픽스처 저장 → 서비스 호출 → 응답/DB 상태를 모두 검증합니다. `@AfterEach` 로 데이터를 정리해 테스트 간 간섭을 없앱니다.

```java
@AfterEach
void tearDown() {
    orderProductRepository.deleteAllInBatch();
    productRepository.deleteAllInBatch();
    orderRepository.deleteAllInBatch();
    stockRepository.deleteAllInBatch();
}

@DisplayName("재고가 부족한 상품으로 주문을 생성하려는 경우 예외가 발생한다.")
@Test
void createOrderWithNoStock() {
    productRepository.saveAll(List.of(
            createProduct(BOTTLE, "001", 1000),
            createProduct(BAKERY, "002", 3000),
            createProduct(HANDMADE, "003", 5000)
    ));
    stockRepository.saveAll(List.of(
            Stock.create("001", 1),
            Stock.create("002", 1)
    ));

    OrderCreateServiceRequest request = OrderCreateServiceRequest.builder()
            .productNumbers(List.of("001", "001", "002", "003"))
            .build();

    LocalDateTime registeredDateTime = LocalDateTime.now();

    assertThatThrownBy(() -> orderService.createOrder(request, registeredDateTime))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("재고가 부족한 상품이 있습니다.");
}
```

## 8. 상품 서비스 통합 테스트
- **언제 사용?** 서비스가 리포지토리를 조합해 새로운 비즈니스 규칙(상품번호 증가 등)을 수행할 때.
- **구성 팁**: 저장소에 선행 데이터를 넣고, 서비스 호출 후 결과 리스트를 `extracting` 으로 비교합니다.

```java
@DisplayName("상품이 하나도 없는 경우 신규 상품을 등록하면 상품번호는 001 이다.")
@Test
void createProductWhenProductIsEmpty() {
    ProductCreateServiceRequest request = ProductCreateServiceRequest.builder()
            .type(HANDMADE)
            .sellingStatus(SELLING)
            .name("카푸치노")
            .price(5000)
            .build();

    productService.createProduct(request);

    List<Product> products = productRepository.findAll();
    assertThat(products).hasSize(1)
            .extracting("productNumber", "type", "sellingStatus", "name", "price")
            .containsExactly(tuple("001", HANDMADE, SELLING, "카푸치노", 5000));
}
```

## 9. 메일 서비스 단위 테스트
- **언제 사용?** 외부 협력 객체(메일 전송 등)를 부분적으로 스텁하고, 부수 효과(저장 호출)를 검증해야 할 때.
- **구성 팁**: `@ExtendWith(MockitoExtension.class)` 를 사용하고, 일부 동작만 대체하려면 `@Spy` + `doReturn` 을 활용합니다.

```java
@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Spy
    private MailSendClient mailSendClient;

    @Mock
    private MailSendHistoryRepository mailSendHistoryRepository;

    @InjectMocks
    private MailService mailService;

    @DisplayName("메일 전송 테스트")
    @Test
    void sendMail() {
        doReturn(true)
                .when(mailSendClient)
                .sendMail(anyString(), anyString(), anyString(), anyString());

        boolean result = mailService.sendMail("", "", "", "");

        assertThat(result).isTrue();
        verify(mailSendHistoryRepository, times(1)).save(any(MailSendHistory.class));
    }
}
```

## 10. 주문 통계 배치 테스트
- **언제 사용?** 배치성 로직에서 기간 필터링과 외부 호출(메일 발송 등)을 동시에 확인해야 할 때.
- **구성 팁**: `@MockBean` 으로 외부 의존성을 대체하고, 결과 저장소에 기록된 이력을 검증합니다.

```java
@SpringBootTest
class OrderStatisticsServiceTest {

    @MockBean
    private MailSendClient mailSendClient;

    @DisplayName("결제완료 주문들을 조회하여 매출 통계 메일을 전송한다.")
    @Test
    void sendOrderStatisticsMail() {
        when(mailSendClient.sendMail(any(), any(), any(), any())).thenReturn(true);

        boolean result = orderStatisticsService.sendOrderStatisticsMail(
                LocalDate.of(2023, 11, 6), "test@test.com"
        );

        assertThat(result).isTrue();
        List<MailSendHistory> histories = mailSendHistoryRepository.findAll();
        assertThat(histories).hasSize(1)
                .extracting("content")
                .contains("총 매출 합계는 12000원 입니다.");
    }
}
```

## 11. 웹 계층 테스트
- **언제 사용?** 컨트롤러의 요청/응답 포맷과 검증 메시지를 확인하고 싶을 때.
- **구성 팁**: `@WebMvcTest`로 컨트롤러만 올리고, 서비스는 `@MockBean`으로 교체합니다. `MockMvc`로 요청을 보내고 `jsonPath`로 응답을 검증합니다.

```java
@WebMvcTest(controllers = ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @DisplayName("신규 상품을 등록할 때 상품 타입은 필수값이다.")
    @Test
    void createProductWithoutType() throws Exception {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .sellingStatus(ProductSellingStatus.SELLING)
                .name("아메리카노")
                .price(4000)
                .build();

        mockMvc.perform(
                        post("/api/v1/products/new")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("상품 타입은 필수입니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
```

```java
@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @DisplayName("신규 주문을 등록할 때 상품번호가 1개 이상이여야 한다.")
    @Test
    void createOrderWithEmptyProductNumbers() throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .productNumbers(List.of())
                .build();

        mockMvc.perform(
                        post("/api/v1/orders/new")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("상품 번호 리스트는 필수입니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
```

## 12. 테스트 픽스처 & 정리 습관
- 빌더/정적 팩토리(`Product.builder()`, `Stock.create`)로 의도가 드러나는 픽스처를 작성합니다.
- 반복되는 픽스처는 private 메서드(`createProduct` 등)로 추출해 다른 테스트에서도 재사용합니다.
- 통합 테스트에서는 `@AfterEach` 혹은 `deleteAllInBatch()`를 활용해 데이터 간섭을 막습니다.

필요한 상황을 위 표에서 찾아 해당 섹션의 코드와 설명을 참고하면, 실무 테스트 작성 시 빠르게 패턴을 적용할 수 있습니다.
