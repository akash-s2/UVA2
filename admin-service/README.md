# Admin Service Module

This module is the **admin/catalog backend service** in UrbanVogue. It manages product lifecycle, inventory updates, catalog read APIs, and internal stock/price endpoints used by the order flow.

The service runs on **port 8092**.

---

## 1) Responsibilities

`admin-service` currently provides four major capabilities:

1. **Product Management (Admin)**
   - Add new products.
   - Update selected product fields.

2. **Inventory Management (Admin)**
   - Update stock quantity for a product.
   - View complete product inventory list.

3. **Catalog APIs (Public Read)**
   - List products for storefront explore pages.
   - Get full product details by product ID.

4. **Internal Order Support APIs**
   - Return stock-aware product details (price, quantity availability, name).
   - Reduce stock after successful order/payment.

---

## 2) Module Structure

```text
admin-service/
  src/main/java/com/UrbanVogue/admin/
    AdminServiceApplication.java

    addProduct/
      controller/ProductController.java
      service/ProductService.java
      dto/{ProductRequestDTO, ProductResponseDTO}.java
      entity/Product.java
      repository/ProductRepository.java

    UpdateDetails/
      controller/UpdateProductController.java
      service/UpdateProductService.java
      dto/UpdateProductRequestDTO.java

    Inventory/
      controller/InventoryController.java
      service/InventoryService.java
      dto/InventoryProductDTO.java

    catalog/
      controller/CatalogController.java
      service/CatalogService.java
      dto/CatalogProductDTO.java

    orderHandling/
      controller/InternalController.java
      service/InternalService.java
      dto/ProductInternalDTO.java

    security/{SecurityConfig, JwtUtil}.java
    filter/JwtAuthFilter.java

  src/main/resources/application.properties
```

---

## 3) Security Model

Security is JWT-based with a custom `JwtAuthFilter`.

### Route rules (as configured)

- `/admin/**` -> requires role `ADMIN`
- `/admin/products/**` -> `ADMIN` or `USER`
- `/catalog/**` -> `permitAll`
- `/internal/**` -> `permitAll`
- other routes -> authenticated

> Note: Because `/admin/**` is broader and listed before `/admin/products/**`, product endpoints under `/admin/products/**` effectively require `ADMIN` in current behavior.

JWT settings are configured in `application.properties`:
- `jwt.secret`
- `jwt.expiration=259200000` (3 days)

---

## 4) API Endpoints

## 4.1 Admin Product APIs

### Add product
- **POST** `/admin/products/add`

Request body (`ProductRequestDTO`) example:
```json
{
  "name": "Classic Tee",
  "brand": "UrbanVogue",
  "category": "T-Shirt",
  "size": "M",
  "color": "Black",
  "price": 799.0,
  "imageUrl": "https://example.com/tee.jpg",
  "numberOfPieces": 50,
  "description": "Cotton regular-fit t-shirt"
}
```

Response (`ProductResponseDTO`) example:
```json
{
  "id": 1,
  "name": "Classic Tee",
  "message": "Product added successfully"
}
```

### Update selected product fields
- **PUT** `/admin/products/update/{id}`

Request body (`UpdateProductRequestDTO`) example:
```json
{
  "name": "Classic Tee V2",
  "price": 899.0,
  "size": "L"
}
```

---

## 4.2 Inventory APIs

### Update stock
- **PUT** `/admin/inventory/{productId}?numberOfPieces=100`

Response:
- plain message: `Stock updated for product id: <id>`

### Get all products (inventory view)
- **GET** `/admin/inventory/products`

Returns list of full `Product` entities.

---

## 4.3 Catalog APIs (public)

### Get products for explore
- **GET** `/catalog/getProducts`

Returns lightweight cards (`CatalogProductDTO`) with:
- `id`
- `name`
- `price`
- `imageUrl`

### Get product detail
- **GET** `/catalog/getProducts/{id}`

Returns full `Product` object.

---

## 4.4 Internal APIs (used by `user-service` order flow)

### Get product for order validation
- **GET** `/internal/products/{id}?qty=<quantity>`

Returns (`ProductInternalDTO`):
- `price`
- `numberOfPieces` (requested qty if available, else `null`)
- `name`

### Reduce stock after order success
- **PUT** `/internal/products/reduce/{id}?qty=<quantity>`

No response body.

---

## 5) Product and Inventory Data Model

`Product` entity fields:
- `id`
- `name`, `brand`, `category`, `size`, `color`
- `price`
- `imageUrl`, `description`
- `numberOfPieces`
- `createdAt` (auto-set on insert via `@PrePersist`)

All product-related features (admin, inventory, catalog, internal order support) use this same entity/repository.

---

## 6) Inter-Service Integration

This service is called by:

- **user-service (`:8091`)**
  - Product browsing (`/catalog/getProducts`, `/catalog/getProducts/{id}`)
  - Order placement helpers (`/internal/products/{id}`, `/internal/products/reduce/{id}`)

This service itself does not currently call downstream services for product/inventory workflows.

---

## 7) Configuration

From `application.properties`:

- `spring.application.name=admin-service`
- `server.port=8092`
- H2 file DB: `jdbc:h2:file:../data/UrbanVogueDB1;AUTO_SERVER=TRUE;`
- JPA schema mode: `spring.jpa.hibernate.ddl-auto=update`
- H2 console enabled at `/h2-console`
- JWT secret + expiration configured

Admin, user, and payment modules can point to the same H2 DB file in local setup.

---

## 8) Dependencies

Key libraries from `pom.xml`:
- Spring Web MVC
- Spring Data JPA
- Spring Security
- H2 Database
- Spring Validation
- Spring Actuator
- Spring Cloud OpenFeign
- JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)

Java version: **21**.

---

## 9) Run Locally

From repo root:

```bash
cd admin-service
mvn spring-boot:run
```

Expected startup:
- service available at `http://localhost:8092`
- console prints `Admin service`

---

## 10) Quick Manual Test Commands

### Add product
```bash
curl -X POST "http://localhost:8092/admin/products/add" \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Classic Tee","brand":"UrbanVogue","category":"T-Shirt","size":"M","color":"Black","price":799.0,"imageUrl":"https://example.com/tee.jpg","numberOfPieces":50,"description":"Cotton t-shirt"}'
```

### Catalog list (public)
```bash
curl "http://localhost:8092/catalog/getProducts"
```

### Internal stock reduce
```bash
curl -X PUT "http://localhost:8092/internal/products/reduce/1?qty=1"
```

---

## 11) Known Limitations / Improvements

1. **Security matcher clarity**
   - Refine matcher order/definitions to avoid ambiguity for `/admin/products/**`.
2. **Internal endpoint protection**
   - `/internal/**` is currently public in HTTP config; consider restricting to trusted services.
3. **Validation**
   - Add bean validations (`@NotBlank`, `@Positive`, etc.) on DTOs.
4. **Error handling**
   - Replace raw `RuntimeException` with typed exceptions + global handler.
5. **Externalized service/network config**
   - Keep ports/URLs and secrets environment-driven for deployment.
6. **Observability**
   - Add structured logs, metrics, and tracing for inventory/order interactions.

---

## 12) Summary

`admin-service` is the source of truth for product and stock data in UrbanVogue. It supports admin product operations, public catalog reads, and internal stock/price APIs used by order placement across services.
