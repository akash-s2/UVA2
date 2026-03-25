# User Service Module

This module is the **user-facing backend service** in UrbanVogue. It handles:
- user authentication (register/login + JWT issuance),
- product exploration APIs for end users,
- order placement flow,
- orchestration with `admin-service` (product/stock) and `payment-service` (payment processing).

The service runs as a standalone Spring Boot microservice on **port 8091**.

---

## 1) Responsibilities

The `user-service` currently owns three functional areas:

1. **Auth Module**
   - User registration and login.
   - Password hashing with BCrypt.
   - JWT token generation with user role.

2. **Product Module (User-side read APIs)**
   - Fetch product list for explore screens.
   - Fetch product details by ID.
   - Delegates data retrieval to `admin-service`.

3. **Order Module**
   - Accept order placement requests from authenticated users.
   - Validates/loads product via `admin-service` internal APIs.
   - Calls `payment-service` for payment status.
   - Updates stock on successful payment.
   - Persists order state in local DB.

---

## 2) Module Structure

```text
user-service/
  src/main/java/com/UrbanVogue/user/
    UserServiceApplication.java

    AuthModule/
      controller/AuthController.java
      service/AuthService.java
      service/AdminInitializer.java
      dto/{RegisterRequest, LoginRequest, AuthResponse}.java
      entity/User.java
      repository/UserRepository.java

    ProductModule/
      controller/GetProductController.java
      service/GetProductService.java
      dto/GetProductDTO.java

    OrderModule/
      controller/OrderController.java
      service/OrderService.java
      client/{ProductClient, PaymentClient}.java
      dto/{OrderRequestDTO, OrderResponseDTO, PaymentRequestDTO, PaymentResponseDTO, ProductResponseDTO}.java
      entity/Order.java
      repository/OrderRepository.java
      config/RestTemplateConfig.java

    security/{SecurityConfig, JwtUtil}.java
    filter/JwtAuthFilter.java

  src/main/resources/application.properties
```

---

## 3) Authentication & Authorization

### Public auth endpoints

- `POST /auth/register`
- `POST /auth/login`

### Security behavior

- JWT filter (`JwtAuthFilter`) inspects `Authorization: Bearer <token>` on protected routes.
- Authenticated principal uses email as identity and JWT role as authority (`ROLE_<role>`).
- Access rules from `SecurityConfig`:
  - `/auth/**` -> public
  - `/h2-console/**` -> public
  - `/user/orders/**` -> requires role `USER`
  - all other endpoints -> authenticated

### JWT details

Configured in `application.properties`:
- `jwt.secret`
- `jwt.expiration=259200000` (milliseconds, i.e. 3 days)

Token contains:
- `sub` = email
- custom claim `role`

---

## 4) API Endpoints

## 4.1 Auth APIs

### Register
- **POST** `/auth/register`

Request example:
```json
{
  "name": "Asha",
  "email": "asha@example.com",
  "password": "pass@123",
  "phoneNumber": "9999999999",
  "address": "Mumbai"
}
```

Response example:
```json
{
  "message": "User registered successfully",
  "token": null
}
```

### Login
- **POST** `/auth/login`

Request example:
```json
{
  "email": "asha@example.com",
  "password": "pass@123"
}
```

Response example:
```json
{
  "message": "Login successful",
  "token": "<jwt-token>"
}
```

---

## 4.2 Product APIs (User-side)

### Get all products
- **GET** `/user/getProducts`

### Get product by ID
- **GET** `/user/getProducts/{id}`

These APIs are served by `GetProductController`, but data is fetched through `RestTemplate` from `admin-service` endpoints:
- `GET http://localhost:8092/catalog/getProducts`
- `GET http://localhost:8092/catalog/getProducts/{id}`

---

## 4.3 Order API

### Place order
- **POST** `/user/orders/place`
- **Auth required:** `Authorization: Bearer <jwt>`

Request example:
```json
{
  "productId": 10,
  "quantity": 2,
  "address": "221B Baker Street"
}
```

Success response example:
```json
{
  "message": "Order placed successfully",
  "paymentStatus": "SUCCESS",
  "orderStatus": "BOOKED"
}
```

Failure response example:
```json
{
  "message": "Payment failed, order not booked",
  "paymentStatus": "FAILED",
  "orderStatus": "FAILED"
}
```

---

## 5) Order Processing Flow

`OrderService#placeOrder` performs this sequence:

1. Extract user email from JWT token.
2. Fetch product + stock context from `admin-service` internal endpoint.
3. Reject if product unavailable/out of stock.
4. Compute total amount (`product.price * quantity`).
5. Build `Order` entity.
6. Call `payment-service` at `http://localhost:8093/payment/process`.
7. If payment succeeds:
   - reduce stock in `admin-service`,
   - mark order as `BOOKED` with payment `SUCCESS`,
   - persist order.
8. If payment fails:
   - mark order/order payment status as `FAILED`,
   - persist order.

---

## 6) Inter-Service Dependencies

This service is coupled to:

- **admin-service (`:8092`)**
  - Catalog read APIs (`/catalog/...`) for product browsing.
  - Internal product APIs (`/internal/products/...`) for stock-aware order placement and stock reduction.

- **payment-service (`:8093`)**
  - Payment processing API: `POST /payment/process`.

> Deployment note: host/port URLs are currently hardcoded in client classes. For production, externalize to configuration/service discovery.

---

## 7) Database & Runtime Configuration

From `application.properties`:

- `spring.application.name=user-service`
- `server.port=8091`
- H2 file DB: `jdbc:h2:file:../data/UrbanVogueDB1;AUTO_SERVER=TRUE;`
- JPA: `ddl-auto=update`
- H2 console enabled at `/h2-console`

`Order` entity stores order details and statuses with `createdAt` populated in `@PrePersist`.

---

## 8) Default Admin Seeder

On startup, `AdminInitializer` checks if `admin@urbanvogue.com` exists.
If absent, it creates a default admin user:
- email: `admin@urbanvogue.com`
- password: `admin123` (BCrypt encoded before save)
- role: `ADMIN`

This bootstrap behavior is intended for development initialization.

---

## 9) Key Dependencies

From `pom.xml`:
- Spring Web MVC
- Spring Data JPA
- Spring Security
- H2 Database
- Spring Actuator
- Spring Validation
- Spring Cloud OpenFeign (available)
- JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) for JWT creation/parsing

Java version: **21**.

---

## 10) Run Locally

From repo root:

```bash
cd user-service
mvn spring-boot:run
```

Expected:
- Service starts at `http://localhost:8091`
- Console prints `User service`

For full flow testing, start dependent services too:
- `admin-service` on `8092`
- `payment-service` on `8093`

---

## 11) Quick Manual Test Flow

1. Register user (`/auth/register`)
2. Login (`/auth/login`) and capture `token`
3. Browse products (`/user/getProducts`)
4. Place order:

```bash
curl -X POST "http://localhost:8091/user/orders/place" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":1,"address":"Delhi"}'
```

---

## 12) Known Limitations / Improvements

1. **Hardcoded downstream URLs**
   - Move to config/env + service discovery.
2. **Error handling**
   - Add centralized exception mapping and clear HTTP status codes.
3. **Validation**
   - Add bean validation annotations to request DTOs.
4. **Observability**
   - Structured logging, tracing, and metrics for cross-service calls.
5. **Order/payment correctness**
   - Ensure deterministic idempotent behavior across retries.
6. **Security hardening**
   - Rotate JWT secrets, secure storage, and token revocation strategy.

---

## 13) Summary

`user-service` is the primary customer interaction backend for authentication, product browsing, and order orchestration. It integrates business flow across admin and payment microservices while maintaining local user/order data and JWT-based route protection.
