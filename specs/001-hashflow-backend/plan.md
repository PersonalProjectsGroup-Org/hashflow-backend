# Implementation Plan: Hashflow Backend Platform

**Branch**: `001-hashflow-backend` | **Date**: 2026-08-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-hashflow-backend/spec.md`

## Summary

Build the Hashflow backend: four Spring Boot microservices (telemetry, financial, agent, news) that power the real-time mining dashboard. The platform simulates rig hardware (RN01–03), computes per-rig profitability (RN04), analyzes market/hardware impact and dispatches notifications (RN05–07), recommends profit switching for GPU rigs (RN08–10), and aggregates news with sentiment (RN11–12). Services communicate via REST, Redis Pub/Sub, and RabbitMQ, are 100% stateless behind an NGINX load balancer, and expose contracts defined in the shared OpenAPI spec (`hashflow-infra`).

## Technical Context

**Language/Version**: Java 21 LTS

**Primary Dependencies**: Spring Boot 3.3.x, Spring Data JPA, Spring Web, Spring WebSocket, Flyway, Spring Boot Actuator + Micrometer, springdoc-openapi (local spec serving), Spring AI (reserved for future diagnostics)

**Storage**: PostgreSQL 16 — one database per service (database-per-service pattern); Flyway migrations; Redis 7 for Pub/Sub + caching

**Messaging**: Redis Pub/Sub (cross-instance WebSocket sync), RabbitMQ (async notification/webhook queues with retry)

**Testing**: JUnit 5 + Spring Boot Test (unit/integration), contract tests against the OpenAPI spec, Checkstyle

**Target Platform**: Linux containers (Docker), Windows/macOS dev machines

**Project Type**: Web services (multi-service backend), Gradle multi-module (Kotlin DSL)

**Performance Goals**: 100+ simulated rigs with sub-second telemetry delivery; stateless cluster failover with zero message loss on single-instance kill

**Constraints**: <200ms p95 API latency (non-streaming endpoints); WebSocket state recoverable after instance loss; contract-first (no endpoint outside OpenAPI)

**Scale/Scope**: 4 microservices; single operator initially (no auth in v1); designed for horizontal scaling behind NGINX

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- ✅ **I. Contract-First** — plan generates Java DTOs from OpenAPI; endpoints tracked against shared spec.
- ✅ **II. Microservice Isolation** — database-per-service; no cross-service DB access.
- ✅ **III. Simulation Realism** — RN01–RN12 mapped to service modules (see traceability below).
- ✅ **IV. Real-Time by Default** — WebSocket + SSE; Redis Pub/Sub for stateless cluster.
- ✅ **V. Test-First Quality Gates** — TDD for business rules; CI per service (JDK 21 → build → Checkstyle → unit → integration → Docker).
- ✅ **VI. Observability & Reliability** — Actuator/Micrometer everywhere; exponential backoff retries (2s→4s→8s) + dead-letter.

### Business Rule Traceability

| Rule | Service | Module |
|------|---------|--------|
| RN01–RN03 | service-telemetry | simulation engine |
| RN04 | service-financial | profitability engine |
| RN05–RN07 | service-agent | impact analysis + notification dispatch |
| RN08–RN10 | service-financial | profit switching algorithm |
| RN11–RN12 | service-news | aggregation + sentiment |

## Project Structure

### Documentation (this feature)

```text
specs/001-hashflow-backend/
├── spec.md              # Feature specification
├── plan.md              # This file
├── checklists/
│   └── requirements.md  # Spec quality checklist
├── contracts/           # Phase 1 output — OpenAPI-derived endpoint inventory per service
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
hashflow-backend/
├── services/
│   ├── service-telemetry/          # Rig lifecycle, telemetry ingestion, simulation, throttling
│   │   └── src/main/java/com/hashflow/telemetry/
│   │       ├── controller/         # REST + SSE + WebSocket endpoints
│   │       ├── service/            # Business logic (rig lifecycle, reading cycle)
│   │       ├── repository/         # Spring Data JPA
│   │       ├── model/              # JPA entities (Rig, Telemetry)
│   │       ├── dto/                # Generated from OpenAPI
│   │       ├── config/             # Spring configuration, WebSocket/SSE setup
│   │       ├── simulation/         # Hardware simulation engine (RN01–RN03)
│   │       └── TelemetryApplication.java
│   ├── service-financial/          # Profitability (RN04), profit switching (RN08–RN10)
│   │   └── src/main/java/com/hashflow/financial/
│   │       ├── controller/         # Profitability + switching endpoints
│   │       ├── service/            # Profit engine, switching engine
│   │       ├── repository/         # Profit snapshots, price history
│   │       ├── model/              # JPA entities
│   │       ├── dto/                # Generated from OpenAPI
│   │       ├── config/             # Scheduled tasks, external API clients
│   │       └── FinancialApplication.java
│   ├── service-agent/              # Impact analysis (RN05–RN06), notifications (RN07)
│   │   └── src/main/java/com/hashflow/agent/
│   │       ├── controller/         # Alerts/history endpoints
│   │       ├── service/            # Impact classifier, dispatch, cooldown, retry
│   │       ├── repository/         # Impact events, deliveries, cooldown state
│   │       ├── model/              # JPA entities
│   │       ├── dto/                # Generated from OpenAPI
│   │       ├── config/             # Redis Pub/Sub, RabbitMQ, email/webhook clients
│   │       └── AgentApplication.java
│   └── service-news/               # Aggregation (RN11), sentiment (RN12)
│       └── src/main/java/com/hashflow/news/
│           ├── controller/         # News endpoints
│           ├── service/            # Aggregator, relevance filter, sentiment
│           ├── repository/         # News articles
│           ├── model/              # JPA entities
│           ├── dto/                # Generated from OpenAPI
│           ├── config/             # Scheduled fetchers, keyword dictionary
│           └── NewsApplication.java
├── .github/workflows/              # ci-telemetry.yml, ci-financial.yml, ci-agent.yml, ci-news.yml
├── docker-compose.yml              # BE-only services (PostgreSQL ×4, Redis, RabbitMQ)
├── Dockerfile                      # Multi-stage per service
└── README.md
```

**Structure Decision**: Multi-module Gradle monorepo (`services/*`) per the project plan, mirroring the repository structure in `hashflow-project-plan.md` §3. Each service owns its database, Dockerfile, and CI workflow. Public contracts are NOT defined locally — they are generated DTOs from the shared `hashflow-infra/openapi/spec.yaml`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | — | — |

## Phase 0 Notes (Research)

- Confirm shared OpenAPI spec v0.1 covers: `/api/rigs` (GET/POST), `/api/rigs/{rigId}/telemetry` (GET + SSE), `/api/rigs/{rigId}` (PATCH/DELETE), `/api/rigs/{rigId}/start|stop`, `/api/financial/profitability`, `/api/financial/switching-recommendations`, `/api/agent/alerts`, `/api/news` — extend the spec in `hashflow-infra` before implementing any endpoint.
- External price/difficulty sources: evaluate free crypto price APIs (CoinGecko etc.) and a difficulty feed; plan caching with staleness markers.
- Redis Pub/Sub channel design: `telemetry.<rigId>`, `alerts.*`, `market.*` — cross-instance relay for WebSocket clients.
