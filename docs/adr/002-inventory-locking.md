# ADR 002: Inventory Locking and Reservation Concurrency Strategy

- **Status:** Accepted
- **Date:** 2026-11-18
- **Decision:** Use atomic conditional SQL updates and PostgreSQL transaction-level advisory locks.

## 1. Context

The FlashSale application must handle multiple users attempting to reserve the same product at the same time.

The system must guarantee that:

- Inventory never becomes negative.
- Reservations never exceed available stock.
- The same idempotency key cannot create multiple reservations.
- Releasing inventory happens only once.
- Concurrent confirmation, cancellation, and expiration operations do not corrupt reservation state.

A simple read-then-update approach is not sufficient because multiple transactions could read the same available stock before either transaction updates it.

## 2. Decision

We use PostgreSQL atomic conditional updates for inventory management and transaction-level advisory locks for idempotency key synchronization.

### 2.1 Atomic Inventory Reservation

Inventory is reserved using a single conditional SQL UPDATE:

```sql
UPDATE inventory
SET reserved_stock = reserved_stock + :quantity,
    updated_at = CURRENT_TIMESTAMP
WHERE campaign_id = :campaignId
  AND product_id = :productId
  AND total_stock - reserved_stock >= :quantity;
```

The operation succeeds only when sufficient stock is available.

The application checks the affected row count:

- `1`: Inventory was successfully reserved.
- `0`: The requested stock is unavailable.

This prevents concurrent transactions from reserving more stock than the available amount.

### 2.2 Atomic Inventory Release

Inventory release also uses a conditional UPDATE:

```sql
UPDATE inventory
SET reserved_stock = reserved_stock - :quantity,
    updated_at = CURRENT_TIMESTAMP
WHERE campaign_id = :campaignId
  AND product_id = :productId
  AND reserved_stock >= :quantity;
```

The condition prevents reserved stock from becoming negative.

### 2.3 Idempotency Synchronization

We use PostgreSQL transaction-level advisory locks based on the idempotency key:

```sql
SELECT pg_advisory_xact_lock(hashtext(?));
```

The lock is acquired before checking whether a reservation already exists for the key.

This ensures that concurrent requests using the same key are processed sequentially within their transactions.

After acquiring the lock:

- If the key does not exist, a new reservation can be created.
- If the key exists with the same request payload, the original reservation is returned.
- If the key exists with a different payload, the request is rejected.

The database uniqueness constraint on the idempotency key provides an additional integrity safeguard.

### 2.4 Reservation State Transitions

Reservation status changes use conditional database updates.

Only an ACTIVE reservation can transition to CONFIRMED, CANCELLED, or EXPIRED.

Inventory is released only when the conditional transition from ACTIVE succeeds.

This ensures that concurrent cancellation and expiration operations cannot release the same inventory twice.

Confirmation does not release inventory.

## 3. Alternatives Considered

### Pessimistic Row Locking

Using `SELECT ... FOR UPDATE` could lock inventory rows before checking and updating stock.

It provides explicit row-level locking but requires a separate read and update operation.

We chose atomic conditional UPDATE because it combines the availability check and inventory modification into one database operation.

### Optimistic Locking

Using a version column could detect concurrent modifications and require retry logic.

This approach was not selected for the current implementation because atomic conditional updates provide a simpler solution for inventory reservation.

### Application-Level Synchronization

Using Java synchronization or locks would not provide reliable coordination across multiple application instances.

Therefore, concurrency control is handled by PostgreSQL.

## 4. Consequences

### Positive

- Inventory updates are atomic.
- Overselling is prevented at the database level.
- Inventory cannot become negative through the implemented release operation.
- Repeated requests with the same idempotency key are handled consistently.
- Reservation state transitions prevent duplicate inventory release.
- PostgreSQL coordinates concurrent operations across application instances.

### Trade-offs

- Advisory locks introduce additional database coordination.
- The `hashtext` function uses a 32-bit hash, so different keys can theoretically map to the same lock. This may cause unnecessary serialization, but does not compromise correctness.
- Database-specific SQL makes this implementation PostgreSQL-dependent.
- Concurrent operations may wait for database locks under heavy load.

## 5. Validation Strategy

The implementation is validated through repository, service, and PostgreSQL-backed integration tests.

The test scenarios include:

- 100 concurrent requests competing for 10 available units.
- Concurrent requests using the same idempotency key and payload.
- Reusing an idempotency key with a different payload.
- Concurrent cancellation and expiration.
- Concurrent confirmation and expiration.
- Repeated expiration processing.
- Verification that inventory is never negative and is released only once.

## 6. Final Decision

The current implementation uses atomic conditional SQL updates for inventory consistency, PostgreSQL transaction-level advisory locks for idempotency synchronization, and conditional reservation status transitions for safe lifecycle management.

This strategy prioritizes database-level correctness and supports concurrent operations across multiple application instances.
