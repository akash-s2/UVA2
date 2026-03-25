# Postman Testing Guide - UVA Microservices

This guide details exactly how to verify all the newly implemented fixes using Postman. Make sure all your services are compiled, running, and interacting over your local port setup.

---

## 1. Input Validation (P1 Fix)

We added `@Valid` and constraints to prevent bad data. Let's test the User API.

### Test A: Empty Email Format
*   **Method**: `POST`
*   **URL**: `http://localhost:8091/auth/register`
*   **Body (JSON)**:
    ```json
    {
       "name": "Jane",
       "email": "not-an-email",
       "password": "123",
       "phoneNumber": "9000000000",
       "address": "123 St"
    }
    ```
*   **Expected Result**: You should immediately receive a `400 Bad Request` thanks to our Global Error Handler. The response body should include a message stating `"Validation error: Invalid email format"` and `"Password must be at least 6 characters"`.

---

## 2. Standardized HTTP Status Codes (P1 Fix)

### Test B: Successful Registration
*   **Method**: `POST`
*   **URL**: `http://localhost:8091/auth/register`
*   **Body (JSON)**: Valid credentials.
*   **Expected Result**: `201 Created` instead of `200 OK`.

### Test C: Invalid Login
*   **Method**: `POST`
*   **URL**: `http://localhost:8091/auth/login`
*   **Body (JSON)**: Bad password.
*   **Expected Result**: `401 Unauthorized` instead of a 200 or 500 error.

---

## 3. Secure Internal APIs (P0 Fix)

We blocked direct outside access to the Admin's internal API, which user-service calls to reduce stock.

### Test D: Attempt Direct Access
*   **Method**: `GET`
*   **URL**: `http://localhost:8092/internal/products/1` (Port 8092 is the Admin service)
*   **Headers**: None.
*   **Expected Result**: `401 Unauthorized`. The [InternalApiKeyFilter](file:///Users/akashnandan/Documents/Programming/UVA-main/admin-service/src/main/java/com/UrbanVogue/admin/filter/InternalApiKeyFilter.java#13-34) blocked your request.

### Test E: Provide the API Key
*   **Method**: `GET`
*   **URL**: `http://localhost:8092/internal/products/1`
*   **Headers**:
    *   `X-Internal-Api-Key`: `uva-internal-secret-2026`
*   **Expected Result**: `200 OK`. The data is retrieved because you provided the shared server key.

---

## 4. Transactional Order Flow & Payment Idempotency (P0 / P1)

We updated the system to explicitly create a `PENDING` order, pass the real Order ID to Payment, and implement idempotency keys to prevent double charging.

### Test F: E2E Order Flow
*   **Method**: `POST`
*   **URL**: `http://localhost:8091/orders/place`
*   **Headers**:
    *   `Authorization`: `Bearer <Add your valid user JWT here>`
*   **Body (JSON)**:
    ```json
    {
      "productId": 1,
      "quantity": 1,
      "address": "789 Main St"
    }
    ```
*   **Expected Result**: This takes a few steps under the hood:
    1.  User-service asks Admin-service for stock/details *(Passing the internal key)*.
    2.  User-service saves the order as `PENDING`.
    3.  User-service generates a UUID request to Payment-service *(Idempotency key).*
    4.  Payment-service creates a transaction record (`SUCCESS` or `FAILED`).
    5.  User-service flips order to `BOOKED` or `FAILED`.
    *   **Response**: You should get a `201 Created` with the final status, real transaction ID, and real order ID attached.

---

## 5. Atomic Stock Reduction (Race Condition)

This is tricky to test on exactly one Postman tab. To verify:
1.  Check the database or UI so a product has exactly `numberOfPieces: 1`.
2.  Open **two** Postman request tabs configured to place an order for that same product.
3.  Execute them at practically the identical time.
4.  **Expected Result**: Only one request can successfully reduce stock to `0`. The second request will hit the new `@Modifying` query, see 0 rows updated, and appropriately return the `409 Conflict` (Insufficient Stock) through the Global Handler. Neither user ends up in a bad DB state.
