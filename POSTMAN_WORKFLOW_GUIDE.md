# Postman End-to-End Testing Guide (UrbanVogue Workflow)

This guide explains how to test the **full project workflow** using Postman, including:
- authentication,
- admin product setup,
- catalog browsing,
- order placement,
- payment simulation,
- stock verification.

It is designed for the current local microservice setup in this repository.

---

## 1) What You Will Test

By the end of this guide, you will verify the complete user journey:

1. Login as admin.
2. Add a product and set inventory.
3. Browse catalog as a user.
4. Register/login a normal user.
5. Place an order.
6. Confirm stock reduction after successful order.
7. (Optional) Test payment service directly.

---

## 2) Services and Ports

Start these services first:

- `api-gateway` -> `http://localhost:8090`
- `user-service` -> `http://localhost:8091`
- `admin-service` -> `http://localhost:8092`
- `payment-service` -> `http://localhost:8093`

Run each from repository root in separate terminals:

```bash
cd api-gateway && mvn spring-boot:run
cd user-service && mvn spring-boot:run
cd admin-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
```

> Note: gateway routes `/auth`, `/user`, `/admin`, and `/catalog`. There is currently no gateway route for `/payment`, so direct payment tests use `:8093`.

---

## 3) Create a Postman Environment

Create a Postman Environment named `UrbanVogue Local` with these variables:

| Variable | Initial Value |
|---|---|
| `gateway_url` | `http://localhost:8090` |
| `payment_url` | `http://localhost:8093` |
| `admin_token` | *(empty)* |
| `user_token` | *(empty)* |
| `product_id` | *(empty)* |
| `user_email` | `customer1@example.com` |
| `user_password` | `pass@123` |

---

## 4) Create Collection Structure

Create a collection named `UrbanVogue E2E` with folders:

1. `00 - Smoke`
2. `01 - Admin Setup`
3. `02 - User Auth`
4. `03 - Catalog`
5. `04 - Order`
6. `05 - Verify`
7. `06 - Payment Direct (Optional)`

---

## 5) Request-by-Request Workflow

## 00 - Smoke

### Request 1: Gateway route smoke (public catalog)
- **Method:** `GET`
- **URL:** `{{gateway_url}}/catalog/getProducts`
- **Expected:** `200 OK` with JSON array (possibly empty initially).

---

## 01 - Admin Setup

### Request 2: Admin login (get admin token)
- **Method:** `POST`
- **URL:** `{{gateway_url}}/auth/login`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "email": "admin@urbanvogue.com",
  "password": "admin123"
}
```

**Tests (Postman Tests tab):**
```javascript
pm.test("Admin login success", function () {
  pm.response.to.have.status(200);
});
const json = pm.response.json();
pm.expect(json.token).to.be.a("string").and.not.empty;
pm.environment.set("admin_token", json.token);
```

### Request 3: Add product (admin)
- **Method:** `POST`
- **URL:** `{{gateway_url}}/admin/products/add`
- **Headers:**
  - `Authorization: Bearer {{admin_token}}`
  - `Content-Type: application/json`
- **Body:**
```json
{
  "name": "Classic Tee",
  "brand": "UrbanVogue",
  "category": "T-Shirt",
  "size": "M",
  "color": "Black",
  "price": 799.0,
  "imageUrl": "https://example.com/tee.jpg",
  "numberOfPieces": 30,
  "description": "Cotton regular fit"
}
```

**Tests:**
```javascript
pm.test("Product created", function () {
  pm.response.to.have.status(200);
});
const json = pm.response.json();
pm.environment.set("product_id", json.id);
```

### Request 4: (Optional) Update inventory for product
- **Method:** `PUT`
- **URL:** `{{gateway_url}}/admin/inventory/{{product_id}}?numberOfPieces=50`
- **Headers:** `Authorization: Bearer {{admin_token}}`
- **Expected:** `200 OK`

---

## 02 - User Auth

### Request 5: Register normal user
- **Method:** `POST`
- **URL:** `{{gateway_url}}/auth/register`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "name": "Customer One",
  "email": "{{user_email}}",
  "password": "{{user_password}}",
  "phoneNumber": "9999999999",
  "address": "Delhi"
}
```

> If already registered, API may return a message indicating email exists. Continue to login.

### Request 6: User login (get user token)
- **Method:** `POST`
- **URL:** `{{gateway_url}}/auth/login`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "email": "{{user_email}}",
  "password": "{{user_password}}"
}
```

**Tests:**
```javascript
pm.test("User login success", function () {
  pm.response.to.have.status(200);
});
const json = pm.response.json();
pm.expect(json.token).to.be.a("string").and.not.empty;
pm.environment.set("user_token", json.token);
```

---

## 03 - Catalog

### Request 7: Get products via gateway
- **Method:** `GET`
- **URL:** `{{gateway_url}}/catalog/getProducts`
- **Expected:** `200 OK` and includes the product you added.

### Request 8: Get user-facing products API
- **Method:** `GET`
- **URL:** `{{gateway_url}}/user/getProducts`
- **Expected:** `200 OK` and list of product cards.

---

## 04 - Order

### Request 9: Place order (authenticated user)
- **Method:** `POST`
- **URL:** `{{gateway_url}}/user/orders/place`
- **Headers:**
  - `Authorization: Bearer {{user_token}}`
  - `Content-Type: application/json`
- **Body:**
```json
{
  "productId": {{product_id}},
  "quantity": 1,
  "address": "221B Baker Street"
}
```

**Expected response:**
- `paymentStatus` = `SUCCESS` or `FAILED`
- `orderStatus` = `BOOKED` on success, `FAILED` on payment failure

**Tests:**
```javascript
pm.test("Order request accepted", function () {
  pm.response.to.have.status(200);
});
const json = pm.response.json();
pm.expect(["SUCCESS", "FAILED"]).to.include(json.paymentStatus);
pm.expect(["BOOKED", "FAILED"]).to.include(json.orderStatus);
```

> Payment service is mock/randomized, so failures are expected sometimes. Retry the order call to observe both outcomes.

---

## 05 - Verify

### Request 10: Check inventory after ordering
- **Method:** `GET`
- **URL:** `{{gateway_url}}/admin/inventory/products`
- **Headers:** `Authorization: Bearer {{admin_token}}`
- **Expected:**
  - On successful payment/order, product stock is reduced.
  - On failed payment/order, stock should remain unchanged.

### Request 11: Product detail check
- **Method:** `GET`
- **URL:** `{{gateway_url}}/catalog/getProducts/{{product_id}}`
- **Expected:** product details are returned.

---

## 06 - Payment Direct (Optional)

Because gateway does not currently expose `/payment/**`, test payment service directly:

### Request 12: Process payment directly
- **Method:** `POST`
- **URL:** `{{payment_url}}/payment/process`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "orderId": 999,
  "amount": 1499.0
}
```

**Expected:**
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

## 6) Recommended Postman Runner Order

Run requests in this exact sequence:

1. Request 1 (Smoke)
2. Request 2, 3, (4 optional)
3. Request 5, 6
4. Request 7, 8
5. Request 9
6. Request 10, 11
7. Request 12 (optional)

This ensures tokens and `product_id` are populated before dependent requests run.

---

## 7) Common Failure Cases and Fixes

1. **401 Unauthorized on admin endpoints**
   - Verify `admin_token` exists and is unexpired.
   - Re-run admin login.

2. **401 Unauthorized on order placement**
   - Verify `user_token` is set from user login request.
   - Confirm `Authorization` header is exactly `Bearer <token>`.

3. **404 or connection refused from gateway routes**
   - Ensure gateway and target service are running on expected ports.

4. **Order keeps failing**
   - Payment module is randomized; retry request.

5. **No products in catalog**
   - Add product first via admin flow.

---

## 8) Optional Enhancements for Team Usage

- Export and version-control the Postman collection JSON.
- Add collection-level pre-request scripts for common headers.
- Add tests that assert schema shape of responses.
- Create a second environment for staging URLs.

---

## 9) Success Criteria Checklist

You have successfully tested the full workflow if all are true:

- [ ] Admin login returns a token.
- [ ] Product is created and `product_id` captured.
- [ ] User register/login works and user token is captured.
- [ ] Catalog endpoints return products.
- [ ] Order endpoint returns valid order/payment statuses.
- [ ] Inventory reflects stock reduction on successful order.

