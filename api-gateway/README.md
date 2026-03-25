# API Gateway Module

This module is the **single entry point** for UrbanVogue client traffic. It uses Spring Cloud Gateway to route requests to downstream services and applies JWT-based request filtering for protected paths.

The gateway runs on **port 8090**.

---

## 1) Responsibilities

`api-gateway` currently handles:

1. **Central request routing** to microservices (`user-service`, `admin-service`).
2. **Token validation for non-public paths** via a global filter.
3. **CORS handling** for frontend clients (configured for `http://localhost:3000`).
4. **Public/private path separation** (auth/catalog/product browsing are treated as public).

---

## 2) Module Structure

```text
api-gateway/
  src/main/java/com/UrbanVogue/gateway/
    ApiGatewayApplication.java
    config/CorsConfig.java
    filter/AuthFilter.java
    security/{SecurityConfig, JwtUtil}.java

  src/main/resources/
    application.yml
    application.properties
```

---

## 3) Route Configuration

Routes are defined in `application.yml`:

- `/auth/**` -> `http://localhost:8091` (`user-service` auth endpoints)
- `/user/**` -> `http://localhost:8091` (`user-service` business endpoints)
- `/admin/**` -> `http://localhost:8092` (`admin-service` protected admin endpoints)
- `/catalog/**` -> `http://localhost:8092` (`admin-service` public catalog endpoints)

So frontend/API clients should typically target the gateway URL (`:8090`) rather than calling services directly.

---

## 4) Security & JWT Behavior

Security is implemented in two layers:

1. **`SecurityConfig` (WebFlux Security)**
   - CSRF disabled.
   - CORS enabled.
   - `/auth/**`, `/user/getProducts/**`, and `OPTIONS` are explicitly permitted.
   - Current final rule is `.anyExchange().permitAll()`.

2. **`AuthFilter` (GlobalFilter)**
   - Runs for incoming requests at high priority (`getOrder() = -1`).
   - Skips token check for public paths:
     - `/auth...`
     - `/user/getProducts...`
     - `/catalog...`
   - For other paths:
     - Requires header `Authorization: Bearer <token>`.
     - Validates JWT signature/claims via `JwtUtil`.
     - Extracts `email` + `role` and writes auth into reactive security context.
     - Returns `401 Unauthorized` if token is missing/invalid.

### JWT configuration

In `application.properties`:
- `jwt.secret`
- `jwt.expiration=259200000` (3 days)

The JWT role claim is expected as `role` and is converted to `ROLE_<role>` authority in the filter.

---

## 5) CORS Configuration

`CorsConfig` currently allows:
- Origin: `http://localhost:3000`
- Methods: `*`
- Headers: `*`
- Credentials: `true`

This supports local frontend development against gateway endpoints.

---

## 6) Public vs Protected Paths (Current Behavior)

### Public (no token required)
- `/auth/**`
- `/user/getProducts/**`
- `/catalog/**`

### Protected (token expected by `AuthFilter`)
- `/user/**` except `/user/getProducts/**`
- `/admin/**`
- any other non-public path

> Important: while `SecurityConfig` currently permits all exchanges, authorization enforcement is effectively performed in `AuthFilter` for non-public endpoints.

---

## 7) End-to-End Request Flow

Example for protected request `POST /user/orders/place`:

1. Client calls gateway: `http://localhost:8090/user/orders/place` with Bearer token.
2. `AuthFilter` validates token.
3. Route predicate `/user/**` forwards request to `user-service` at `:8091`.
4. Downstream service processes request and returns response via gateway.

Example for public request `GET /catalog/getProducts`:

1. Client calls gateway: `http://localhost:8090/catalog/getProducts`.
2. `AuthFilter` skips validation (public path).
3. Gateway routes to `admin-service` at `:8092`.

---

## 8) Run Locally

From repo root:

```bash
cd api-gateway
mvn spring-boot:run
```

Expected startup:
- Gateway starts at `http://localhost:8090`
- Console prints `API-Gateway`

For full functionality, run dependent services:
- `user-service` on `8091`
- `admin-service` on `8092`
- (optional for order flows) `payment-service` on `8093`

---

## 9) Quick Manual Checks

### Public auth route (proxied to user-service)
```bash
curl -X POST "http://localhost:8090/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"pass@123"}'
```

### Public catalog route (proxied to admin-service)
```bash
curl "http://localhost:8090/catalog/getProducts"
```

### Protected user route (requires token)
```bash
curl -X POST "http://localhost:8090/user/orders/place" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":1,"address":"Delhi"}'
```

---

## 10) Dependencies

Key dependencies from `pom.xml`:
- `spring-cloud-starter-gateway`
- `spring-boot-starter-security`
- `spring-boot-starter-actuator`
- JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- `spring-boot-starter-test`

Java version: **21**.

---

## 11) Known Limitations / Improvements

1. **Authorization split across two layers**
   - `SecurityConfig` permits all exchanges; hard enforcement is in `AuthFilter`. Consider aligning rules to avoid confusion.
2. **Hardcoded downstream URIs**
   - Move service URLs to environment/config profiles or service discovery.
3. **Route coverage**
   - If new service paths are introduced, update route predicates and public-path skip logic together.
4. **Observability**
   - Add structured request logs, correlation IDs, and gateway metrics dashboards.
5. **Security hardening**
   - Reduce debug logs that print auth/token details; enforce stricter token claim validation.

---

## 12) Summary

`api-gateway` centralizes routing and request security checks for UrbanVogue microservices. It provides client-friendly single-host access while delegating business processing to user/admin services based on route predicates.
