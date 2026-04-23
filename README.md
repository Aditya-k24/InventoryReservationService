# Inventory Reservation Service

A Spring Boot + PostgreSQL backend that prevents overselling through transactional stock holds, row-level locking, timed reservation expiry, and idempotent REST APIs.

## Overview

Clients can increase or decrease stock, place a timed reservation on one or more SKUs, confirm that reservation into a committed deduction, or cancel it. A scheduled background job automatically expires stale reservations and releases stock. Every state transition is protected by a database transaction with `SELECT FOR UPDATE` row locking, so concurrent requests cannot oversell inventory.

```
Client → Spring Security (Basic Auth)
       → Controller
       → Service (@Transactional + row-level locks)
       → Repository (Spring Data JPA)
       → PostgreSQL
```

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.4 |
| Security | Spring Security — HTTP Basic, BCrypt |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Validation | Bean Validation (Jakarta) |
| Observability | Spring Boot Actuator |
| API Docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, MockMvc, Testcontainers |
| Container | Docker + Compose |

## Prerequisites

- Java 21+
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Docker (for local database or full stack)

## Local Setup

### Option 1 — Docker Compose (full stack)

```bash
docker compose up --build
```

The app will be available at `http://localhost:8080`.

### Option 2 — Run the app against a local PostgreSQL

**Start only the database:**

```bash
docker compose up -d postgres
```

**Copy and edit environment config:**

```bash
cp .env.example .env
```

**Run the application:**

```bash
./mvnw spring-boot:run
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/inventory_reservation` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |

## Demo Credentials

Two users are seeded on first startup:

| Email | Password | Role |
|---|---|---|
| `admin@example.com` | `changeme` | `ADMIN` |
| `ops@example.com` | `changeme` | `OPERATOR` |

Roles control endpoint access — see [API Overview](#api-overview) below.

## API Overview

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/inventory/adjustments` | ADMIN | Increase or decrease stock |
| `GET` | `/api/v1/inventory/{skuCode}` | ADMIN, OPERATOR | Read current stock |
| `POST` | `/api/v1/reservations` | ADMIN, OPERATOR | Create a timed stock hold |
| `GET` | `/api/v1/reservations/{id}` | ADMIN, OPERATOR | Read reservation state |
| `POST` | `/api/v1/reservations/{id}/confirm` | ADMIN, OPERATOR | Commit reserved stock |
| `POST` | `/api/v1/reservations/{id}/cancel` | ADMIN, OPERATOR | Release reserved stock |
| `GET` | `/actuator/health` | Public | Liveness check |

Interactive docs are available at **`/swagger-ui.html`** when the app is running.

## Example Requests

**Check stock:**

```bash
curl -u ops@example.com:changeme \
  http://localhost:8080/api/v1/inventory/SKU-RED-CHAIR
```

```json
{
  "skuCode": "SKU-RED-CHAIR",
  "name": "Red Chair",
  "totalQuantity": 25,
  "availableQuantity": 25,
  "reservedQuantity": 0
}
```

**Create a reservation:**

```bash
curl -u ops@example.com:changeme \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-100045" \
  -X POST http://localhost:8080/api/v1/reservations \
  -d '{
    "customerReference": "ORDER-100045",
    "ttlMinutes": 15,
    "items": [
      { "skuCode": "SKU-RED-CHAIR", "quantity": 2 }
    ]
  }'
```

```json
{
  "reservationId": "0b44f7c8-2af0-4ef0-88f1-9559dcb3fb0e",
  "status": "PENDING",
  "customerReference": "ORDER-100045",
  "expiresAt": "2026-04-20T15:30:00Z",
  "items": [{ "skuCode": "SKU-RED-CHAIR", "quantity": 2 }],
  "links": {
    "self": "/api/v1/reservations/0b44f7c8-2af0-4ef0-88f1-9559dcb3fb0e",
    "confirm": "/api/v1/reservations/0b44f7c8-2af0-4ef0-88f1-9559dcb3fb0e/confirm",
    "cancel": "/api/v1/reservations/0b44f7c8-2af0-4ef0-88f1-9559dcb3fb0e/cancel"
  }
}
```

**Confirm the reservation:**

```bash
curl -u ops@example.com:changeme -X POST \
  http://localhost:8080/api/v1/reservations/0b44f7c8-2af0-4ef0-88f1-9559dcb3fb0e/confirm
```

**Adjust stock (admin only):**

```bash
curl -u admin@example.com:changeme \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8080/api/v1/inventory/adjustments \
  -d '{ "skuCode": "SKU-RED-CHAIR", "delta": 10, "reason": "restock" }'
```

**Error response (RFC 9457 ProblemDetail):**

```json
{
  "type": "https://example.com/problems/insufficient-inventory",
  "title": "Insufficient inventory",
  "status": 409,
  "detail": "Requested 4 units of SKU-RED-CHAIR but only 3 are available.",
  "instance": "/api/v1/reservations"
}
```

## Database Migrations

Flyway migrations run automatically on startup:

| Migration | Purpose |
|---|---|
| `V1__initial_schema.sql` | Core tables: `app_user`, `sku`, `inventory_item`, `reservation`, `reservation_item` |
| `V2__add_inventory_events.sql` | Audit/event log table |
| `V3__add_pending_reservation_index.sql` | Partial index for expiry queries |
| `V4__seed_demo_data.sql` | Seed SKUs and initial inventory |

The `inventory_item` table enforces invariants at the database level:

```sql
CONSTRAINT chk_inventory_non_negative CHECK (total_qty >= 0 AND available_qty >= 0 AND reserved_qty >= 0),
CONSTRAINT chk_inventory_balance      CHECK (total_qty = available_qty + reserved_qty)
```

## Testing

```bash
./mvnw test
```

The test suite (27 tests) covers:

| Layer | What is tested |
|---|---|
| Unit | `InventoryItem` quantity transitions, `Reservation` state machine |
| Integration | Full HTTP stack against a real PostgreSQL via Testcontainers |
| Concurrency | Two simultaneous reservations against stock of 1 — exactly one succeeds |
| Idempotency | Same `Idempotency-Key` sent twice returns the same `reservationId` |
| Security | Unauthenticated → 401, wrong role → 403 |
| Expiry | Back-dated reservation expires and stock is released |

Testcontainers pulls a `postgres:16-alpine` image automatically — no local database needed for tests.

## Inventory State Machine

```
Reserve:        available_qty -= qty,  reserved_qty += qty
Confirm:        reserved_qty  -= qty,  total_qty    -= qty
Cancel/Expire:  reserved_qty  -= qty,  available_qty += qty
Adjust +N:      total_qty     += N,    available_qty += N
Adjust -N:      total_qty     -= N,    available_qty -= N  (requires available_qty >= N)
```

## Idempotency

`POST /api/v1/reservations` accepts an optional `Idempotency-Key` header. If a reservation already exists for the same user + key combination, the original response is returned rather than creating a new hold. This makes client retries safe under partial failures.

## Reservation Expiry

A scheduled job runs every 60 seconds (configurable via `app.expiry.interval-ms`). It finds all `PENDING` reservations whose `expires_at` has passed, releases their stock, and marks them `EXPIRED`.
