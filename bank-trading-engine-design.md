# Bank Trading Engine Design

### Java Order Matching Engine Specification

---

# 1. Source of Truth

This document defines the Java application architecture.

The database schema, stored procedures, triggers, indexes, and views are already implemented separately and must not be modified.

The database remains the source of truth.

Runtime data structures are caches only.

---

# 2. Architecture

One OrderBook exists per instrument.

```
HashMap<String, OrderBook>
```

Each OrderBook contains:

```
TreeMap<BigDecimal, PriceLevel> buyBook   // descending
TreeMap<BigDecimal, PriceLevel> sellBook  // ascending
```

Each PriceLevel contains:

* price
* head
* tail
* orderCount
* totalQuantity

Each OrderNode contains:

* Order
* prev
* next
* priceLevel

Maintain:

```
HashMap<Long, OrderNode>
```

for O(1) active order lookup.

Runtime caches:

* Instruments
* Wallets
* Holdings
* Trader → Client assignments
* Recent trades (capped)

Only TreeMap and HashMap from Java Collections are allowed.

Implement the remaining data structures manually.

---

# 3. Startup

On application startup:

* Load ACTIVE instruments
* Load wallets
* Load holdings
* Load trader assignments
* Load recent trades
* Load OPEN orders
* Rebuild every OrderBook

Do not load:

* FILLED
* CANCELLED
* REJECTED

orders into memory.

---

# 4. Matching Engine

Implement a synchronous single-threaded matching engine.

Do not use:

* Threads
* ExecutorService
* CompletableFuture
* Locks
* synchronized
* Parallel Streams

Matching follows strict Price-Time Priority.

Priority:

1. Best Price
2. FIFO
3. Partial fills
4. Continue until remaining quantity is zero or no executable order exists.

---

# 5. Order Processing

### LIMIT Orders

1. Validate.
2. Attempt matching.
3. Remaining quantity enters OrderBook.
4. Preserve original timestamp.

### MARKET Orders

1. Match immediately.
2. Never rest in OrderBook.
3. Fill available quantity.
4. Cancel remaining quantity.

### IOC Orders

1. Match immediately.
2. Cancel remaining quantity.
3. Never rest.

### FOK Orders

1. Verify full fill is possible.
2. Execute entire order.
3. Otherwise reject.

---

# 6. Risk Validation

Validate before entering the OrderBook.

Checks:

* Trader assigned to client
* Instrument active
* Valid tick size
* Positive quantity
* BUY has sufficient wallet balance
* SELL has sufficient holdings

Reject invalid orders immediately.

---

# 7. Reservation

BUY

```
Available Balance
        ↓
Reserved Balance
```

SELL

```
Available Holdings
        ↓
Reserved Holdings
```

Release reservations on:

* Cancel
* Reject
* Expiry

Convert reservations into settlement after successful execution.

---

# 8. Settlement

Settlement is independent of the Matching Engine.

Matching Engine only discovers trades.

SettlementService performs:

* Wallet updates
* Holding updates
* Order updates
* Trade creation
* Audit logging

Execute inside one database transaction.

On failure:

* Roll back database
* Roll back cache updates

Only update runtime caches after successful commit.

---

# 9. Cancellation

Use:

```
HashMap<Long, OrderNode>
```

for O(1) lookup.

Cancellation must:

* Remove order from linked list
* Remove empty PriceLevels
* Release reservations
* Persist changes

---

# 10. Expiry

Expire:

* PENDING
* PARTIALLY_FILLED

orders.

Release reservations.

Remove from memory.

Persist changes.

---

# 11. Required Services

Implement:

* TradingService
* MatchingEngine
* OrderBookService
* RiskValidationService
* ReservationService
* SettlementService
* CancelOrderService
* ExpiryService

TradingService orchestrates the workflow.

MatchingEngine must never perform database updates directly.

---

# 12. Runtime Complexity

| Operation              | Complexity |
| ---------------------- | ---------- |
| Add Order              | O(log P)   |
| Cancel Order           | O(1)       |
| Lookup Order           | O(1)       |
| Insert into PriceLevel | O(1)       |
| Remove from PriceLevel | O(1)       |
| Best Bid               | O(1)       |
| Best Ask               | O(1)       |
| FIFO Removal           | O(1)       |

---

# 13. Edge Cases

Handle:

* Empty order book
* Invalid quantity
* Invalid price
* Invalid tick size
* Insufficient wallet balance
* Insufficient holdings
* Self trade prevention
* Partial fills
* Multiple price level fills
* IOC remainder cancellation
* FOK rejection
* Market order without liquidity
* Duplicate order IDs
* Order expiry
* Cache rebuild after restart

---

# 14. Test Mode

Authentication remains unchanged.

Create a separate:

```
TestApplication.java
```

Responsibilities:

* Connect to database
* Load runtime caches
* Rebuild OrderBooks
* Automatically use seeded Admin
* Provide console menu
* Invoke service layer methods only

No business logic should exist inside TestApplication.

The class should be removable after authentication is completed.

---

# 15. Code Quality

* Production-quality Java
* Small cohesive classes
* Meaningful names
* No duplicated logic
* No placeholder methods
* No TODO comments
* No dead code
* Everything must compile
