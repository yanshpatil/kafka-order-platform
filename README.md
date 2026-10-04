# Kafka Order Processing Platform

An event-driven order platform built with **Java 17, Spring Boot 3 and Apache Kafka**. Three
microservices cooperate purely through Kafka events using the **Saga (choreography)** pattern,
with a **transactional outbox**, **idempotent consumers**, **retries with a dead letter topic**
and **compensating transactions**.

## Architecture

```
                POST /orders
  Client ─────────────────────▶ Order Service ──(outbox)──▶ orders.created
                                      ▲                            │
                                      │                            ▼
                    payments.completed│                    Inventory Service
                    payments.failed   │                    reserves stock
                    inventory.failed  │                     │            │
                                      │        inventory.reserved   inventory.failed
                                      │                     ▼
                                      └───────────────  Payment Service
                                                         charges the order
                                                                │
                                         payments.failed ───────┴──▶ Inventory Service
                                         (compensation: release stock)
```

Happy path: `PENDING → (stock reserved) → (payment ok) → CONFIRMED`
Failure paths: out of stock → `CANCELLED`; payment declined → stock released, `CANCELLED`.

| Service | Port | Database | Consumes | Produces |
|---|---|---|---|---|
| order-service | 8081 | orders_db | payments.completed, payments.failed, inventory.failed | orders.created (via outbox) |
| inventory-service | 8082 | inventory_db | orders.created, payments.failed | inventory.reserved, inventory.failed |
| payment-service | 8083 | payments_db | inventory.reserved | payments.completed, payments.failed |

Topics have 3 partitions and are keyed by `orderId`, so all events of one order stay ordered.

## Reliability techniques

- **Transactional outbox (order-service):** the order and its `OrderCreated` event are written in
  one DB transaction; a poller relays the row to Kafka. No lost or phantom events.
- **Idempotent consumers:** inventory keys reservations by `orderId`, payment keys charges by
  `orderId`, order status only changes from `PENDING`. Redelivered events are harmless.
- **Idempotent producer:** `acks=all`, `enable.idempotence=true`.
- **Retries + DLT:** failed records are retried 3 times with exponential backoff, then moved to
  `<topic>.DLT` (see `KafkaCommonConfig`).
- **Compensation:** `payments.failed` makes inventory release the reserved stock.
- **Optimistic locking:** `@Version` on stock prevents lost updates under concurrency.

## Prerequisites

Java 17+, Maven 3.9+, Docker.

## Run it

```bash
# 1. Infrastructure: Kafka (KRaft), Postgres, Kafka UI
docker compose up -d

# 2. Build everything
mvn clean install -DskipTests

# 3. Start the services (three terminals)
mvn -pl order-service spring-boot:run
mvn -pl inventory-service spring-boot:run
mvn -pl payment-service spring-boot:run
```

Kafka UI: http://localhost:8090

## Try it

Place an order (products P1001 to P1004 have 100 units, P1005 has only 5):

```bash
curl -s -X POST localhost:8081/orders \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1","items":[{"productId":"P1001","quantity":2,"price":499.00}]}'
```

Copy the returned `id`, then poll the order. It starts `PENDING` and ends `CONFIRMED`
(or `CANCELLED` about 10% of the time due to the simulated payment decline):

```bash
curl -s localhost:8081/orders/<id>
curl -s localhost:8083/payments/<id>
curl -s localhost:8082/inventory
```

Trigger the failure paths:

```bash
# Out of stock -> CANCELLED (inventory.failed)
curl -s -X POST localhost:8081/orders -H 'Content-Type: application/json' \
  -d '{"userId":"u1","items":[{"productId":"P1005","quantity":50,"price":10.00}]}'

# Payment over the limit -> stock released -> CANCELLED (payments.failed)
curl -s -X POST localhost:8081/orders -H 'Content-Type: application/json' \
  -d '{"userId":"u1","items":[{"productId":"P1001","quantity":1,"price":150000.00}]}'
```

After the second one, `GET :8082/inventory` shows P1001 back at its original quantity.

## Project structure

```
common/              shared event records, topic names, Kafka config (error handler, DLT, topics)
order-service/       REST API, orders, transactional outbox, saga completion
inventory-service/   stock, reservations, compensation
payment-service/     simulated gateway, idempotent charging
docker-compose.yml   Kafka, Postgres, Kafka UI
```

## Design decisions and known limitations

- Only order-service uses the outbox. Inventory and payment publish right after their DB commit,
  and recover from a crash in between because redelivery re-reads the stored result and
  republishes. Using the outbox everywhere would be the stricter option.
- Schema is JSON with Jackson. Moving to Avro and a Schema Registry would add enforced
  compatibility.
- Tables are created with `ddl-auto: update` for simplicity; use Flyway for production.
- Single-broker dev cluster (replication factor 1). Production should use 3 brokers,
  `replication.factor=3`, `min.insync.replicas=2`.

## Roadmap

- [ ] Fraud detection service (Kafka Streams, windowed order-frequency rules)
- [ ] Notification service
- [ ] Integration tests with Testcontainers
- [ ] Prometheus and Grafana dashboards (consumer lag, throughput)
- [ ] Dockerfiles for each service
