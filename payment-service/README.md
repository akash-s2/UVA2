# Payment Service Module

This module is the **payment microservice** in the UrbanVogue backend. It exposes a simple HTTP endpoint that receives payment requests for an order and returns a payment status.

At the moment, payment is intentionally simulated (mocked) using random success/failure logic:
- ~70% chance of `SUCCESS`
- ~30% chance of `FAILED`

---

## 1) Purpose and Responsibilities

The `payment-service` module is responsible for:
- Accepting payment requests from upstream services (primarily `user-service`).
- Returning a normalized payment response (`SUCCESS` or `FAILED`).
- Acting as a standalone microservice running on port **8093**.

What it does **not** currently do:
- No integration with external payment gateways (Stripe, Razorpay, PayPal, etc.).
- No transaction persistence/entity model.
- No idempotency/retry handling.
- No authentication/authorization around payment endpoint.

---

## 2) High-Level Architecture

The payment module follows a simple layered flow:

1. `PaymentController` receives HTTP request at `/payment/process`.
2. Controller forwards request body to `PaymentService`.
3. `PaymentService` simulates payment outcome with random logic.
4. `PaymentResponseDTO` is returned to caller.

### Main package structure

```text
payment-service/
  src/main/java/com/UrbanVogue/payment/
    PaymentServiceApplication.java
    controller/
      PaymentController.java
    service/
      PaymentService.java
    dto/
      PaymentRequestDTO.java
      PaymentResponseDTO.java
  src/main/resources/
    application.properties
```

---

## 3) API Contract

## Endpoint

- **Method:** `POST`
- **Path:** `/payment/process`
- **Content-Type:** `application/json`

### Request JSON (`PaymentRequestDTO`)

```json
{
  "orderId": 123,
  "amount": 1499.99
}
```

Fields:
- `orderId` (`Long`): Logical order identifier sent by caller.
- `amount` (`Double`): Total order amount to be charged.

### Response JSON (`PaymentResponseDTO`)

```json
{
  "status": "SUCCESS"
}
```

or

```json
{
  "status": "FAILED"
}
```

---

## 4) Current Payment Decision Logic

Inside `PaymentService#processPayment`:
- A random integer `0..99` is generated.
- If number is `< 70`, response status is `SUCCESS`.
- Otherwise status is `FAILED`.

This is useful for early-stage integration testing where order flow needs variable outcomes without real gateway setup.

---

## 5) Integration with Other Modules

`user-service` calls this endpoint from its `PaymentClient` using `RestTemplate`:

- URL used by caller: `http://localhost:8093/payment/process`
- Request body maps to same payload (`orderId`, `amount`)
- Response consumed as status (`SUCCESS` / `FAILED`)

In order placement flow:
- `SUCCESS` => order is marked booked and stock is reduced.
- `FAILED` => order is stored with failed status.

> Note: In current `OrderService`, `order.getId()` may still be null before persistence when sent to payment service. Since payment is mock-based now, this does not break processing, but should be fixed when moving to real payment processing.

---

## 6) Configuration

`application.properties` defines runtime behavior:

- `spring.application.name=payment-service`
- `server.port=8093`
- H2 datasource file DB path: `jdbc:h2:file:./data/UrbanVogueDB1`
- JPA auto-update schema enabled
- H2 console enabled at `/h2-console`

Even though DB/JPA are configured, current payment flow does not persist payment records.

---

## 7) Dependencies (Maven)

Key dependencies used in this module:
- `spring-boot-starter-webmvc` for REST endpoint support
- `spring-boot-starter-data-jpa` + `h2` for persistence setup
- `spring-boot-h2console` for local DB console
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-cloud-starter-openfeign` (present but not actively used in payment flow)

Java version is set to **21**.

---

## 8) Run the Module Locally

From repository root:

```bash
cd payment-service
./mvnw spring-boot:run
```

Expected startup:
- Service starts on `http://localhost:8093`
- Console prints: `Payment service`

---

## 9) Quick Manual Test

Use `curl`:

```bash
curl -X POST "http://localhost:8093/payment/process" \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"amount":999.0}'
```

Example output:

```json
{"status":"SUCCESS"}
```

(You may also get `FAILED` due to simulated randomness.)

---

## 10) Limitations and Recommended Next Steps

To make this production-ready, consider implementing:

1. **Persistent payment model**
   - Add `Payment` entity + repository and store transaction attempts/results.
2. **Validation and error handling**
   - Add bean validation (`@NotNull`, `@Positive`) and controller advice.
3. **Idempotency keys**
   - Avoid duplicate charges on retries/timeouts.
4. **Gateway abstraction**
   - Introduce interface for payment providers and concrete strategy implementations.
5. **Security**
   - Protect endpoint using JWT/service-to-service authentication.
6. **Observability**
   - Add structured logs, trace IDs, and richer actuator metrics.
7. **Contract hardening**
   - Use explicit enums/status codes and standardized response envelope.

---

## 11) Summary

The current payment module is a lightweight, mock payment engine designed to unblock and test end-to-end order placement. It is simple, integration-friendly, and suitable for development/demo environments, but it needs persistence, security, and real gateway integration for production use.
