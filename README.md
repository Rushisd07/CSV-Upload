# CSV/JSON File Upload API — Spring Boot

A production-grade REST API that accepts large CSV or JSON file uploads, parses them with streaming (no full in-memory load), validates schema and data quality, and efficiently bulk-inserts into a PostgreSQL database across multiple related tables.

---

## Architecture Overview

```
                        ┌─────────────────────────────────────────┐
                        │           REST API (Controller)         │
                        │  POST /api/v1/upload/csv                │
                        │  POST /api/v1/upload/json               │
                        │  GET  /api/v1/upload/jobs/{jobId}       │
                        └───────────────┬─────────────────────────┘
                                        │ returns jobId immediately (async)
                        ┌───────────────▼─────────────────────────┐
                        │        FileUploadService (Async)        │
                        │   - Streams file in batches of N rows   │
                        │   - Never loads full file into memory   │
                        └──┬──────────────┬──────────────┬────────┘
                           │              │              │
               ┌───────────▼──┐  ┌────────▼──┐  ┌──────▼────────┐
               │CustomerService│  │ProductSvc │  │  OrderService  │
               │  validate     │  │  validate │  │  validate      │
               │  batch upsert │  │  batch    │  │  group by order│
               └───────────────┘  └───────────┘  └───────────────┘
                           │              │              │
                        ┌──▼──────────────▼──────────────▼──────┐
                        │         JdbcBatchInserter              │
                        │  INSERT ... ON CONFLICT DO UPDATE      │
                        │  JDBC batch_size=500                   │
                        └──────────────────┬─────────────────────┘
                                           │
                        ┌──────────────────▼─────────────────────┐
                        │            PostgreSQL                   │
                        │  categories / customers / products      │
                        │  orders / order_items / upload_jobs     │
                        └─────────────────────────────────────────┘
```

---

## Database Schema (5 Related Tables)

| Table         | Key Fields                              | Relationships               |
|---------------|-----------------------------------------|-----------------------------|
| `categories`  | id, category_code, parent_id            | self-referencing (hierarchy)|
| `customers`   | id, customer_code, email                | standalone                  |
| `products`    | id, product_code, sku, category_id      | → categories                |
| `orders`      | id, order_number, customer_id           | → customers                 |
| `order_items` | id, order_id, product_id, quantity      | → orders, products          |
| `upload_jobs` | id, job_id (UUID), status, progress     | tracking table              |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

---

## PostgreSQL Setup

```sql
-- Run as postgres superuser
CREATE DATABASE dataloader_db;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE dataloader_db TO postgres;
```

Or with custom credentials — update `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/dataloader_db
spring.datasource.username=YOUR_USER
spring.datasource.password=YOUR_PASSWORD
```

**Flyway** automatically runs migrations on startup:
- `V1__initial_schema.sql` — creates all 6 tables + indexes
- `V2__seed_categories.sql` — seeds 15 product categories

---

## Build & Run

```bash
# Clone / navigate to project
cd csv-upload-api

# Build
mvn clean package -DskipTests

# Run
java -jar target/csv-upload-api-1.0.0.jar

# Or with Maven
mvn spring-boot:run
```

App starts at: `http://localhost:8080`

---

## API Reference

### 1. Upload CSV File

```
POST /api/v1/upload/csv
Content-Type: multipart/form-data

Parameters:
  file      (required) - CSV file
  dataType  (required) - CUSTOMERS | PRODUCTS | ORDERS
```

**Example (curl):**
```bash
# Upload customers
curl -X POST http://localhost:8080/api/v1/upload/csv \
  -F "file=@sample-data/customers.csv" \
  -F "dataType=CUSTOMERS"

# Upload products
curl -X POST http://localhost:8080/api/v1/upload/csv \
  -F "file=@sample-data/products.csv" \
  -F "dataType=PRODUCTS"

# Upload orders (must upload customers + products first)
curl -X POST http://localhost:8080/api/v1/upload/csv \
  -F "file=@sample-data/orders.csv" \
  -F "dataType=ORDERS"
```

**Response (202 Accepted):**
```json
{
  "success": true,
  "message": "File accepted for processing. Track progress using the jobId.",
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "customers.csv",
    "fileType": "CSV",
    "status": "PENDING",
    "totalRows": 0,
    "processedRows": 0,
    "failedRows": 0,
    "progressPercent": 0.0
  }
}
```

---

### 2. Upload JSON File

```
POST /api/v1/upload/json
Content-Type: multipart/form-data
```

Supports both formats:
```json
[{ "customerCode": "C001", ... }, ...]
```
or:
```json
{ "data": [{ "customerCode": "C001", ... }, ...] }
```

```bash
curl -X POST http://localhost:8080/api/v1/upload/json \
  -F "file=@sample-data/customers.json" \
  -F "dataType=CUSTOMERS"
```

---

### 3. Check Job Status

```
GET /api/v1/upload/jobs/{jobId}
```

```bash
curl http://localhost:8080/api/v1/upload/jobs/550e8400-e29b-41d4-a716-446655440000
```

**Response:**
```json
{
  "success": true,
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "COMPLETED",
    "totalRows": 20,
    "processedRows": 19,
    "failedRows": 1,
    "progressPercent": 95.0,
    "startedAt": "2025-11-16T10:00:01",
    "completedAt": "2025-11-16T10:00:03"
  }
}
```

**Job Statuses:**
| Status       | Meaning                                    |
|--------------|--------------------------------------------|
| `PENDING`    | Job created, not yet started               |
| `PROCESSING` | Actively ingesting rows                    |
| `COMPLETED`  | All rows processed, 0 failures             |
| `PARTIAL`    | Some rows failed, some succeeded           |
| `FAILED`     | Fatal error, no rows inserted              |

---

### 4. Query Loaded Data

```bash
# Summary counts
GET /api/v1/data/summary

# Paginated data
GET /api/v1/data/customers?page=0&size=20
GET /api/v1/data/products?page=0&size=20
GET /api/v1/data/orders?page=0&size=20
GET /api/v1/data/categories

# Health check
GET /actuator/health
```

---

## CSV File Format

### customers.csv
| Column         | Required | Type    | Notes                     |
|----------------|----------|---------|---------------------------|
| customerCode   | ✅       | String  | Unique identifier         |
| firstName      | ✅       | String  |                           |
| lastName       | ✅       | String  |                           |
| email          | ✅       | String  | Must be valid email       |
| phone          |          | String  |                           |
| dateOfBirth    |          | Date    | yyyy-MM-dd or dd/MM/yyyy  |
| country        |          | String  |                           |
| city           |          | String  |                           |
| address        |          | String  |                           |
| postalCode     |          | String  |                           |
| loyaltyPoints  |          | Integer | Default: 0                |
| isActive       |          | Boolean | true/false/yes/no/1/0     |

### products.csv
| Column        | Required | Type    | Notes                      |
|---------------|----------|---------|----------------------------|
| productCode   | ✅       | String  | Unique identifier          |
| productName   | ✅       | String  |                            |
| unitPrice     | ✅       | Decimal | Must be ≥ 0                |
| categoryCode  |          | String  | Must match categories table|
| description   |          | String  |                            |
| stockQuantity |          | Integer | Default: 0                 |
| weightKg      |          | Decimal |                            |
| brand         |          | String  |                            |
| sku           |          | String  | Unique if provided         |
| isActive      |          | Boolean |                            |

### orders.csv (one row per order-item)
| Column         | Required | Notes                              |
|----------------|----------|------------------------------------|
| orderNumber    | ✅       | Groups multiple items per order    |
| customerCode   | ✅       | Must exist in customers table      |
| productCode    | ✅       | Must exist in products table       |
| quantity       | ✅       | Positive integer                   |
| unitPrice      | ✅       | Item price                         |
| status         |          | PENDING/CONFIRMED/SHIPPED/DELIVERED|
| totalAmount    |          | Order-level total                  |
| currency       |          | Default: USD                       |

---

## Efficient Design Decisions

### 1. Streaming Parsers (No Full In-Memory Load)
- **CSV**: Apache Commons CSV with `BufferedReader(64KB buffer)` — processes line by line
- **JSON**: Jackson Streaming API (`JsonParser`) — processes token by token

### 2. Batch JDBC Inserts
- Uses `JdbcTemplate.batchUpdate()` bypassing JPA overhead
- `INSERT ... ON CONFLICT DO UPDATE` for idempotent upserts
- Configurable batch size (default: 500 rows per batch)

### 3. Async Processing
- `@Async` with `ThreadPoolTaskExecutor` (4–8 threads)
- API returns `202 Accepted` immediately with a tracking `jobId`
- Client polls `GET /jobs/{jobId}` for progress

### 4. Connection Pool Tuning
- HikariCP with pool size 20, proper timeouts
- Hibernate `jdbc.batch_size=500` for JPA operations
- `order_inserts=true` and `order_updates=true`

### 5. Caching Lookups
- In-process `ConcurrentHashMap` caches for `category_code → id`, `customer_code → id`, `product_code → id`
- Prevents N+1 DB lookups during bulk processing

### 6. Schema Validation
- Required field checks, email regex, positive number checks
- Flexible date parsing (multiple formats supported)
- Invalid rows are counted as `failedRows` and skipped — does not abort the job

---

## Project Structure

```
src/main/java/com/dataloader/
├── CsvUploadApiApplication.java
├── config/
│   ├── AsyncConfig.java          # Thread pool configuration
│   └── JacksonConfig.java        # ObjectMapper setup
├── controller/
│   ├── FileUploadController.java  # Upload endpoints
│   └── DataQueryController.java  # Read endpoints
├── service/
│   ├── FileUploadService.java    # Orchestrates async processing
│   ├── UploadJobService.java     # Job lifecycle management
│   ├── CustomerService.java      # Customer batch processor
│   ├── ProductService.java       # Product batch processor
│   └── OrderService.java         # Order + items processor
├── model/
│   ├── UploadJob.java
│   ├── Customer.java
│   ├── Product.java
│   ├── Order.java
│   ├── OrderItem.java
│   └── Category.java
├── repository/                   # Spring Data JPA repos
├── dto/
│   ├── ApiResponse.java
│   ├── UploadJobResponse.java
│   ├── CustomerCsvRow.java
│   ├── ProductCsvRow.java
│   └── OrderCsvRow.java
├── util/
│   ├── CsvStreamParser.java      # Streaming CSV reader
│   ├── JsonStreamParser.java     # Streaming JSON reader
│   ├── JdbcBatchInserter.java    # JDBC batch helper
│   └── DataValidator.java        # Schema + data quality
└── exception/
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.properties
└── db/migration/
    ├── V1__initial_schema.sql
    └── V2__seed_categories.sql

sample-data/
├── customers.csv       (20 customers)
├── products.csv        (20 products)
├── orders.csv          (15 orders, 26 line items)
├── customers.json
└── products.json
```

---

## Recommended Upload Order

Since orders reference customers and products, upload in this order:
1. `customers.csv` → `CUSTOMERS`
2. `products.csv` → `PRODUCTS`
3. `orders.csv` → `ORDERS`

---

## Running Tests

```bash
mvn test
```

Tests use an H2 in-memory database — no PostgreSQL needed for tests.
