# UVA-main — Improvement Suggestions & Edge Cases

A prioritized list of improvements for the UrbanVogue e-commerce backend, organized by severity.

---

## 🔴 Critical Issues (Must Fix)

### 1. Race Condition in Stock Reduction

**File:** [InternalService.java](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/orderHandling/service/InternalService.java#L36-L41)

**Problem:** [reduceStock()](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/ProductClient.java#65-79) reads stock, subtracts, and saves — this is not atomic. Two concurrent orders for the last item will both succeed, causing **negative inventory**.

```java
// CURRENT: not thread-safe
public void reduceStock(Long id, Integer qty) {
    Product product = getProduct(id);
    product.setNumberOfPieces(product.getNumberOfPieces() - qty);
    productRepository.save(product);
}
```

**Fix:** Use `@Transactional` + pessimistic locking, or a native `UPDATE` query:
```java
@Modifying
@Query("UPDATE Product p SET p.numberOfPieces = p.numberOfPieces - :qty WHERE p.id = :id AND p.numberOfPieces >= :qty")
int reduceStockAtomically(@Param("id") Long id, @Param("qty") Integer qty);
```
If the return value is `0`, the stock was insufficient — reject the order.

---

### 2. No Transactional Boundary on Order Flow

**File:** [OrderService.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/service/OrderService.java#L29-L93)

**Problem:** [placeOrder()](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/controller/OrderController.java#16-26) performs multiple steps (check stock → pay → reduce stock → save order) without `@Transactional`. If [reduceStock()](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/ProductClient.java#65-79) succeeds but `orderRepository.save()` fails, stock is deducted but no order is recorded.

**Fix:** Add `@Transactional` and implement a **compensation/rollback** pattern:
```java
@Transactional
public OrderResponseDTO placeOrder(OrderRequestDTO request, String token) {
    // ... existing flow ...
    // If orderRepository.save() throws, the @Transactional rolls back local DB changes.
    // For cross-service rollback, implement a restore-stock call to admin-service.
}
```

---

### 3. Unsecured Internal APIs

**File:** [SecurityConfig.java (admin)](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/security/SecurityConfig.java#L33)

**Problem:** `/internal/**` endpoints are `permitAll()`. Anyone can call `PUT /internal/products/reduce/{id}?qty=1000` and zero out all inventory.

```java
.requestMatchers("/internal/**").permitAll()  // ← DANGEROUS
```

**Fix options:**
1. **API key authentication** — Require a shared secret header for internal calls
2. **Network-level restriction** — Only allow calls from `localhost` / internal network
3. **Service-to-service JWT** — Issue a special service JWT with a service role

---

### 4. Admin Security Rule Ordering Bug

**File:** [SecurityConfig.java (admin)](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/security/SecurityConfig.java#L27-L34)

**Problem:** `.requestMatchers("/admin/**").hasRole("ADMIN")` comes **before** `.requestMatchers("/admin/products/**").hasAnyRole("ADMIN", "USER")`. Spring Security matches the **first** rule, so the second rule is **dead code** — USERs can never access `/admin/products/**`.

```java
// CURRENT: second rule is unreachable
.requestMatchers("/admin/**").hasRole("ADMIN")           // catches everything
.requestMatchers("/admin/products/**").hasAnyRole(...)    // never reached
```

**Fix:** Put more specific matchers first:
```java
.requestMatchers("/admin/products/**").hasAnyRole("ADMIN", "USER")
.requestMatchers("/admin/**").hasRole("ADMIN")
```

---

### 5. Payment Called Before Order Has an ID

**File:** [OrderService.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/service/OrderService.java#L61-L62)

**Problem:** The payment client is called with `order.getId()`, but the order hasn't been saved yet — so [getId()](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/entity/Order.java#42-45) returns `null`. The payment service receives `orderId: null`.

```java
Order order = new Order();
// ... set fields but don't save ...
PaymentResponseDTO paymentResponse =
    paymentClient.processPayment(order.getId(), totalAmount);  // ← null
```

**Fix:** Either save the order first with `PENDING` status, or generate a UUID before persistence.

---

## 🟠 Important Improvements

### 6. No Input Validation

**Problem:** No `@Valid` annotations on any `@RequestBody` parameters. Users can submit:
- Negative quantities, zero / negative prices
- Empty product names, blank emails
- SQL injection via string fields (less risk with JPA, but still bad practice)

**Fix:** Add `@Valid` to controllers and JSR-380 annotations to DTOs:
```java
public class OrderRequestDTO {
    @NotNull @Positive
    private Long productId;
    
    @NotNull @Min(1)
    private Integer quantity;
    
    @NotBlank
    private String address;
}
```

---

### 7. No Global Error Handling

**Problem:** `RuntimeException("Product not found")` in [InternalService](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/orderHandling/service/InternalService.java#8-43) returns a raw 500 error with a stack trace to the client. No `@ControllerAdvice` exists anywhere.

**Fix:** Add a `GlobalExceptionHandler` to each service:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        return new ErrorResponse("INTERNAL_ERROR", "Something went wrong");
    }
}
```

---

### 8. Hardcoded Service URLs

**Files:** [ProductClient.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/ProductClient.java#L47), [PaymentClient.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/PaymentClient.java#L20)

**Problem:** URLs like `"http://localhost:8092/internal/products/"` are hardcoded. This breaks in any non-local environment.

**Fix:** Externalize to [application.properties](file:///Users/akashnandan/Documents/Programming/UVA-main/api-gateway/target/classes/application.properties):
```properties
service.admin.url=http://localhost:8092
service.payment.url=http://localhost:8093
```
Then inject with `@Value("${service.admin.url}")`. Better yet, use **Spring Cloud Service Discovery** (Eureka/Consul).

---

### 9. Payment Service Has No Persistence

**Problem:** Payment results are never stored. There's no [Payment](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/PaymentClient.java#9-26) entity, no transaction log. If the user-service fails after receiving a `SUCCESS` response, there's no record the payment ever happened — auditing or debugging becomes impossible.

**Fix:** Create a [Payment](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/PaymentClient.java#9-26) entity in payment-service:
```java
@Entity
public class Payment {
    @Id @GeneratedValue
    private Long id;
    private Long orderId;
    private Double amount;
    private String status;          // SUCCESS / FAILED
    private String transactionId;   // UUID
    private LocalDateTime createdAt;
}
```

---

### 10. No Idempotency on Payment

**Problem:** If the network call to payment-service times out, the user-service has no way to check if the payment already went through. Retrying creates a **duplicate charge** risk.

**Fix:** Implement idempotency keys:
1. Generate a UUID (`idempotencyKey`) before calling payment
2. Pass it with the request
3. Payment service checks if that key was already processed; if so, returns the cached result

---

### 11. No Proper HTTP Status Codes

**Problem:** All endpoints return `200 OK` even on errors. The `AuthService.login()` returns `200` with body `{"message": "Invalid password", "token": null}` instead of `401`.

**Fix:** Use `ResponseEntity<>` with appropriate status codes:
```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    if (response.getToken() == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    return ResponseEntity.ok(response);
}
```

---

## 🟡 Performance & Architecture

### 12. Redundant DB Calls in InternalService

**File:** [InternalService.java](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/orderHandling/service/InternalService.java)

**Problem:** `InternalController.getProduct()` calls [getPrice()](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/addProduct/entity/Product.java#75-78), [checkAvailability()](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/orderHandling/service/InternalService.java#19-26), and [getProductName()](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/entity/Order.java#54-57) — each one executes `productRepository.findById(id)`. That's **3 identical DB queries** for one request.

```java
Double price = internalService.getPrice(id);           // query 1
Integer quantity = internalService.checkAvailability(id, qty); // query 2
String productName = internalService.getProductName(id);       // query 3
```

**Fix:** Fetch once and return all data:
```java
public ProductInternalDTO getProductDetails(Long id, Integer qty) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    Integer available = (product.getNumberOfPieces() >= qty) ? qty : null;
    return new ProductInternalDTO(product.getPrice(), available, product.getName());
}
```

---

### 13. Missing Circuit Breaker / Resilience

**Problem:** If payment-service is down, `OrderService.placeOrder()` throws an unhandled `RestClientException`, returning a 500 to the user.

**Fix:** Add **Resilience4j** circuit breaker:
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
public PaymentResponseDTO processPayment(Long orderId, Double amount) { ... }

public PaymentResponseDTO paymentFallback(Long orderId, Double amount, Exception e) {
    return new PaymentResponseDTO("FAILED");
}
```

---

### 14. No Pagination on Product Listing

**Problem:** `GET /catalog/getProducts` returns ALL products in a single response. With thousands of products, this will cause OOM errors and slow response times.

**Fix:** Use Spring Data's `Pageable`:
```java
@GetMapping("/getProducts")
public Page<CatalogProductDTO> getProducts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    return catalogService.getProducts(PageRequest.of(page, size));
}
```

---

### 15. Session Management Missing in User Service

**File:** [SecurityConfig.java (user)](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/security/SecurityConfig.java)

**Problem:** Admin-service sets `SessionCreationPolicy.STATELESS`, but user-service doesn't. This means Spring creates HTTP sessions unnecessarily, wasting memory.

**Fix:** Add to user-service SecurityConfig:
```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

---

## 🔵 New Features to Implement

### 16. Order History Endpoint

**Current state:** Orders are saved but there's no way for users to view their order history.

**Suggested endpoint:**
```
GET /user/orders/history
Authorization: Bearer <token>
```
Query: `orderRepository.findByCustomerEmail(email)` with pagination.

---

### 17. Product Search & Filtering

**Current state:** Only `getProducts()` (all) and `getProducts/{id}` (by ID) exist.

**Suggested endpoints:**
```
GET /catalog/search?q=tee&category=T-Shirt&minPrice=500&maxPrice=2000&size=M
GET /catalog/products?sortBy=price&order=asc
```
Use Spring Data JPA `Specification` or `@Query` with dynamic parameters.

---

### 18. Admin Dashboard Statistics

**Current state:** No analytics endpoints.

**Suggested endpoints:**
```
GET /admin/dashboard/stats
→ { totalProducts, totalOrders, revenue, topProducts, ordersByStatus }
```

---

### 19. Order Cancellation

**Current state:** Once placed, orders can never be cancelled. Stock is permanently reduced.

**Suggested flow:**
1. `PUT /user/orders/{id}/cancel`
2. Check `orderStatus != "CANCELLED"` and is within cancellation window
3. Restore stock via internal API
4. Update order status to "CANCELLED"
5. Issue refund (future payment gateway integration)

---

### 20. Rate Limiting

**Problem:** No rate limiting on any endpoint. A single client can spam `POST /auth/login` for brute-force attacks or flood orders.

**Fix:** Add Spring Cloud Gateway rate limiting:
```yaml
spring.cloud.gateway.routes:
  - id: auth-service
    uri: http://localhost:8091
    predicates:
      - Path=/auth/**
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 5
          redis-rate-limiter.burstCapacity: 10
```

---

## 🟣 Code Quality & Best Practices

### 21. Replace `System.out.println` with SLF4J Logging

**Files:** Multiple — [AuthFilter.java](file:///Users/akashnandan/Documents/Programming/UVA-main/api-gateway/src/main/java/com/UrbanVogue/gateway/filter/AuthFilter.java), [ProductClient.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/ProductClient.java)

```java
// BAD
System.out.println(" FILTER HIT ");

// GOOD
private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
log.debug("Gateway filter invoked for path: {}", path);
```

---

### 22. Use Lombok Consistently

**Problem:** [User.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/AuthModule/entity/User.java), [Product.java](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/addProduct/entity/Product.java), [Order.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/entity/Order.java) have hand-written getters/setters despite Lombok being on the classpath.

**Fix:** Replace with `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`.

---

### 23. Use Constructor Injection Instead of `@Autowired`

**Problem:** Most services use field injection (`@Autowired`), which is discouraged — it makes testing harder and hides dependencies.

```java
// BAD
@Autowired private OrderRepository orderRepository;
@Autowired private ProductClient productClient;

// GOOD
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
}
```

---

### 24. Add API Documentation (Swagger/OpenAPI)

**Problem:** No API documentation. Team members rely on reading source code.

**Fix:** Add `springdoc-openapi-starter-webmvc-ui` dependency and annotate controllers:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.7.0</version>
</dependency>
```
Access Swagger UI at `http://localhost:{port}/swagger-ui.html`.

---

### 25. Environment-Specific Configuration

**Problem:** Only one [application.properties](file:///Users/akashnandan/Documents/Programming/UVA-main/api-gateway/target/classes/application.properties) per service. The JWT secret and DB URLs are hardcoded for local dev.

**Fix:** Use Spring profiles (`application-dev.properties`, `application-prod.properties`):
```properties
# application-prod.properties
spring.datasource.url=jdbc:mysql://prod-db:3306/urbanvogue
jwt.secret=${JWT_SECRET}   # from environment variable
```

---

## Summary Priority Matrix

| Priority | Issue | Impact |
|---|---|---|
| 🔴 P0 | Race condition in stock reduction | Data corruption |
| 🔴 P0 | No transactional boundary on orders | Inconsistent state |
| 🔴 P0 | Unsecured internal APIs | Security vulnerability |
| 🔴 P0 | Admin security rule ordering bug | Authorization bypass |
| 🔴 P0 | Payment called with null orderId | Runtime bug |
| 🟠 P1 | No input validation | Bad data in DB |
| 🟠 P1 | No global error handling | 500 errors leak stack traces |
| 🟠 P1 | Hardcoded service URLs | Environment inflexibility |
| 🟠 P1 | No payment persistence | Audit trail missing |
| 🟠 P1 | No idempotency on payment | Duplicate charges |
| 🟠 P1 | Wrong HTTP status codes | Poor API contract |
| 🟡 P2 | 3x redundant DB queries | Performance waste |
| 🟡 P2 | No circuit breaker | Cascading failures |
| 🟡 P2 | No pagination | Memory/performance issues at scale |
| 🟡 P2 | Missing stateless session policy | Memory leak |
| 🔵 P3 | Order history, search, dashboard, cancellation | Feature gaps |
| 🟣 P4 | Logging, Lombok, DI pattern, Swagger, profiles | Code quality |
