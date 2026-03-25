# P0 & P1 Fixes for UVA-main

## P0 — Critical

- [x] **1. Race condition in stock reduction** — Add atomic `@Modifying @Query` to [ProductRepository](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/addProduct/repository/ProductRepository.java#9-15) and update `InternalService.reduceStock()`
- [x] **2. No transactional boundary on orders** — Add `@Transactional` to `OrderService.placeOrder()`, save order as PENDING first
- [x] **3. Unsecured internal APIs** — Secure `/internal/**` with API key header validation in admin-service
- [x] **4. Admin security rule ordering bug** — Reorder matchers in admin [SecurityConfig](file:///Users/akashnandan/Documents/Programming/UVA-main/user-service/src/main/java/com/UrbanVogue/user/security/SecurityConfig.java#12-47)
- [x] **5. Payment called with null orderId** — Save order as PENDING before calling payment, use real orderId

## P1 — Important

- [x] **6. No input validation** — Add `@Valid` + JSR-380 annotations to all DTOs
- [x] **7. No global error handling** — Add `@RestControllerAdvice` to each service
- [x] **8. Hardcoded service URLs** — Externalize to [application.properties](file:///Users/akashnandan/Documents/Programming/UVA-main/api-gateway/target/classes/application.properties)
- [x] **9. No payment persistence** — Create [Payment](file:///Users/akashnandan/Documents/Programming/UVA-main/payment-service/src/main/java/com/UrbanVogue/payment/entity/Payment.java#10-34) entity + repository in payment-service
- [x] **10. No idempotency on payment** — Add idempotency key support
- [/] **11. Wrong HTTP status codes** — Use `ResponseEntity` with correct status codes

## Verification

- [ ] Build all 4 services with `mvn compile`
