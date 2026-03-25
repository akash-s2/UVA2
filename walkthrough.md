# UrbanVogue Apparel - Implementation Walkthrough

This document outlines all the performance, security, and architectural improvements implemented across the [UVA-main](file:///Users/akashnandan/Documents/Programming/UVA-main) microservices (User, Admin, and Payment services).

## 1. P0 Critical Fixes

### 1.1 Atomic Stock Reduction (Race Condition Fix)
*   **Problem**: Concurrent orders could buy the same item due to non-atomic stock checks in the admin service.
*   **Changes**:
    *   **[ProductRepository.java](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/addProduct/repository/ProductRepository.java)**: Added an atomic update query `@Modifying @Query("UPDATE Product p SET p.numberOfPieces = p.numberOfPieces - :qty WHERE p.id = :id AND p.numberOfPieces >= :qty")`.
    *   **[InternalService.java](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/orderHandling/service/InternalService.java)**: Consolidated repetitive DB lookups into a single [getProductDetails()](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/orderHandling/service/InternalService.java#21-26) method. Replaced the manual stock reduction with the atomic query. Wrapped in `@Transactional`.

### 1.2 Transactional Order Flow
*   **Problem**: Order service crashed mid-process leaving orphaned payments or orders. `orderId` was null when making the payment call.
*   **Changes**:
    *   **[OrderService.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/service/OrderService.java)**: Added `@Transactional` to [placeOrder](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/controller/OrderController.java#17-27). The order is now saved first with a `PENDING` status. The generated `orderId` is explicitly passed to `paymentService`. Depending on the payment result, the status flips to `BOOKED` or `FAILED`.

### 1.3 Secure Internal APIs
*   **Problem**: Internal service endpoints like `/internal/reduceStock` were exposed publicly.
*   **Changes**:
    *   **[SecurityConfig.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/security/SecurityConfig.java) (Admin)**: Removed `permitAll()` from `/internal/**` and relied on authentication.
    *   **[InternalApiKeyFilter.java](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/filter/InternalApiKeyFilter.java) (Admin)**: Introduced a custom `OncePerRequestFilter` to intercept `/internal/**` and demand an `X-Internal-Api-Key` header. Fails with `401 Unauthorized` if mismatched.
    *   **[application.properties](file:///Users/akashnandan/Documents/Programming/UVA-main/api-gateway/target/classes/application.properties)**: Placed the shared secret `uva-internal-secret-2026` in both user and admin properties.
    *   **[ProductClient.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/ProductClient.java) (User)**: Configured Feign/RestTemplate equivalent to automatically inject `X-Internal-Api-Key` on outgoing requests.

### 1.4 Security Rule Shadowing
*   **Problem**: In [SecurityConfig.java](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/security/SecurityConfig.java), `/admin/**` intercepted requests meant for `/admin/products/**`.
*   **Changes**: Reordered the HTTP security matchers to evaluate the more specific path first.

---

## 2. P1 Important Improvements

### 2.1 Missing Input Validation
*   **Problem**: Malformed requests led to deep application errors and 500s.
*   **Changes**:
    *   Updated [OrderRequestDTO](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/dto/OrderRequestDTO.java#5-40), [RegisterRequest](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/AuthModule/dto/RegisterRequest.java#5-43), [LoginRequest](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/AuthModule/dto/LoginRequest.java#4-20), [ProductRequestDTO](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/addProduct/dto/ProductRequestDTO.java#5-102), and [PaymentRequestDTO](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/dto/PaymentRequestDTO.java#3-25) with Java Bean Constraints: `@NotNull`, `@NotBlank`, `@Min`, `@Email`, `@Size`.
    *   Annotated all controller request bodies with `@Valid` ([AuthController](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/AuthModule/controller/AuthController.java#9-32), [OrderController](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/controller/OrderController.java#10-28), [ProductController](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/addProduct/controller/ProductController.java#10-22), [PaymentController](file:///Users/akashnandan/Documents/Programming/UVA-main/payment-service/src/main/java/com/UrbanVogue/payment/controller/PaymentController.java#10-22)).

### 2.2 Global Error Handling
*   **Problem**: Every generic failure returned ugly empty responses or stack traces.
*   **Changes**:
    *   Created [GlobalExceptionHandler](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/exception/GlobalExceptionHandler.java#12-42) with `@RestControllerAdvice` in the User, Admin, and Payment services.
    *   Captures `MethodArgumentNotValidException` to cleanly return validation errors in an [ErrorResponse](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/exception/ErrorResponse.java#5-23) structured object. Map custom runtime exceptions (e.g., [ResourceNotFoundException](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/exception/ResourceNotFoundException.java#3-8), [InsufficientStockException](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/exception/InsufficientStockException.java#3-8)) to 404 and 409 status codes respectively.

### 2.3 Hardcoded Service URLs
*   **Problem**: URLs were strictly `http://localhost:8092`, preventing easy deployment to Docker or Kubernetes.
*   **Changes**:
    *   Added external properties `service.admin.url` and `service.payment.url` to the user service properties file.
    *   Updated [PaymentClient](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/PaymentClient.java#12-33) and [ProductClient](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/ProductClient.java#16-90) to read these values dynamically via `@Value`.

### 2.4 Payment Persistence & Idempotency
*   **Problem**: Payment service had no database, making crash recoveries impossible. Duplicate network retries could charge the user twice.
*   **Changes**:
    *   **Entity & Repo**: Created [Payment.java](file:///Users/akashnandan/Documents/Programming/UVA-main/payment-service/src/main/java/com/UrbanVogue/payment/entity/Payment.java) entity (storing orderId, amount, status, idempotencyKey, transactionId) and [PaymentRepository.java](file:///Users/akashnandan/Documents/Programming/UVA-main/payment-service/src/main/java/com/UrbanVogue/payment/repository/PaymentRepository.java).
    *   **Idempotency**: [PaymentClient](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/OrderModule/client/PaymentClient.java#12-33) generates a `UUID` idempotency key. [PaymentService](file:///Users/akashnandan/Documents/Programming/UVA-main/payment-service/src/main/java/com/UrbanVogue/payment/service/PaymentService.java#14-49) checks its database for this key. If the key exists, it returns the previously cached response preventing duplicate logic. If not, it persists the new payment details.

### 2.5 Correct HTTP Status Codes
*   **Problem**: Successful POST updates were wrongly returning `200 OK` or missing the target.
*   **Changes**: Controllers wrap responses in `ResponseEntity`: `HttpStatus.CREATED (201)` for `addProducts`, [register](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/AuthModule/controller/AuthController.java#17-21), and [processPayment](file:///Users/akashnandan/Documents/Programming/UVA-main/payment-service/src/main/java/com/UrbanVogue/payment/controller/PaymentController.java#17-21). And cleanly mapping authentication failures to `HttpStatus.UNAUTHORIZED (401)`.
