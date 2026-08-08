# Hashflow Backend Constitution

**Project**: Hashflow — Crypto Mining Monitoring & Automation Platform (Backend)
**Owner**: Raphael Santos
**Scope**: `hashflow-backend` repository — microservices, contracts, and infrastructure for the Hashflow platform. The frontend (`hashflow-frontend`, Mateus Quintanilha) and shared contracts (`hashflow-infra`) live in separate repositories.

## Core Principles

### I. Contract-First (OpenAPI Is the Source of Truth)

All inter-service and frontend contracts are defined in the OpenAPI 3.0 spec living in the shared `hashflow-infra` repository (`openapi/spec.yaml`). Backend services must never invent public API shapes; every REST endpoint exposed by a service MUST be represented in the shared spec before implementation. Java DTOs are generated from the spec. When a contract changes, the spec is updated first, then the generated artifacts (TS types + Java DTOs) are refreshed.

### II. Microservice Isolation (Database-Per-Service)

Each service owns its domain and its own PostgreSQL database. No service may read or write another service's database directly — inter-service communication happens exclusively through APIs (REST for synchronous calls, Redis Pub/Sub for real-time broadcast, RabbitMQ for async messaging). Shared domain knowledge travels through contracts, not shared schemas.

### III. Simulation Realism (Business Rules Are Law)

The telemetry simulation must obey the business rules RN01–RN12 exactly:
- **RN01** — A rig only produces telemetry while `ONLINE`; `OFFLINE` forces hashrate to 0 and power to 5% of max (standby).
- **RN02** — Hashrate and temperature oscillate on every reading cycle by a random factor of at most ±3%.
- **RN03** — Temperature > 85°C activates `THROTTLING` (hashrate cut by 50%); > 95°C forces `OFFLINE` (emergency shutdown) and emits a High-impact event.
- **RN04** — Net profit is strictly `(Hashrate / Global Difficulty) × Block Reward × Coin Price − ((PowerWatts × 24) / 1000) × kWh Price`. External price/difficulty feeds are mandatory.
- **RN05** — Volatility windows compare current coin price against the price 10 minutes ago (Delta %).
- **RN06** — Impact classification follows the decision matrix: ±<2% → LOW (dashboard only); ±2–4.99% → MEDIUM (WebSocket + webhook); ≥±5% OR rig at 95°C → HIGH (WebSocket + webhook + email).
- **RN07** — After a High-impact alert for a given coin/rig, notification cooldown blocks repeat email for the same trigger for 15 minutes.
- **RN08** — Profit switching runs only for GPU rigs (ASICs are algorithm-locked).
- **RN09** — Switching compares projected net profit across algorithms over the last 24 hours.
- **RN10** — A switch is recommended only when the alternative coin's net profit is ≥10% higher than the current coin.
- **RN11** — News is imported only when title/body mentions a supported coin (name or ticker).
- **RN12** — Sentiment is keyword-dictionary based: negative terms (Ban, Crash, Regulation, Hack, Attack) → `NEGATIVE`; positive terms (Rally, Adoption, Partnership, Upgrade, Record) → `POSITIVE`; otherwise/tie → `NEUTRAL`.

### IV. Real-Time by Default (No Refresh Buttons)

The dashboard consumes a continuous stream: **WebSockets** for alert-level events, **Server-Sent Events** for periodic metric updates. Backend instances must be **100% stateless** behind the NGINX load balancer; WebSocket state is synchronized across instances via **Redis Pub/Sub**. The system must failover transparently when any single instance is killed (chaos engineering requirement).

### V. Test-First Quality Gates

- TDD is mandatory for all business-rule logic: tests are written first, verified to fail, then implementation follows (Red-Green-Refactor).
- Every service ships unit tests + integration tests; contract tests validate endpoints against the OpenAPI spec.
- CI (GitHub Actions) runs per service: setup JDK 21 → Gradle build → Checkstyle → unit tests → integration tests → Docker build.
- No PR merges to `main` with failing tests, missing Checkstyle compliance, or uncommitted spec/plan drift.

### VI. Observability & Reliability

- Spring Boot Actuator + Micrometer metrics are enabled in every service; the dashboard consumes real metrics.
- Structured logging everywhere; correlation IDs across inter-service calls.
- Webhook delivery retries with exponential backoff (2s → 4s → 8s) via `@Async` / message queue with a dead-letter path.
- Chaos engineering is a first-class practice: kill an instance mid-transmission and verify transparent failover.

## Technology Stack (Non-Negotiable)

- **Java 21 LTS** + **Spring Boot 3.x** + **Gradle** (Kotlin DSL) — no Maven, no other JVM language.
- **PostgreSQL 16** per service (database-per-service), **Flyway** for migrations.
- **Redis 7** — Pub/Sub for cross-instance WebSocket sync and caching.
- **RabbitMQ** — async messaging (notification/webhook queues with retry policy).
- **OpenAPI 3.0** — contract single source of truth (shared `hashflow-infra` repo).
- Docker + Docker Compose for local dev; NGINX as the load balancer in front of the stateless cluster.
- Deployments: Render (free tier) or AWS ECS / OCI free tier; Supabase PostgreSQL or local Docker for dev.

## Development Workflow

1. **Spec first**: every feature starts as a `specs/NNN-<feature>/spec.md` written with the spec-kit workflow (`/speckit.specify`), reviewed before planning.
2. **Plan with contracts**: `/speckit.plan` produces the technical plan; OpenAPI changes land in `hashflow-infra` before service code.
3. **Tasks and implementation**: `/speckit.tasks` breaks work into dependency-ordered tasks; `/speckit.implement` executes them.
4. **Converge**: `/speckit.converge` after implementation verifies the codebase against spec/plan/tasks; repeat until converged.
5. **Commit hygiene**: conventional commits (`feat:`, `fix:`, `build:`, `chore:`), one logical change per commit, no unrelated edits.

## Governance

- This constitution supersedes ad-hoc conventions; all PRs/reviews must verify compliance with it.
- Complexity must be justified: prefer the simplest design that satisfies the business rules — no speculative abstractions (YAGNI). REST between services is the default; add RabbitMQ/Redis only when the notification or real-time requirement demands it.
- Amendments require documentation, approval, and a migration plan for existing artifacts.
- Business rules RN01–RN12 are normative; changes to them are product decisions that must flow through the spec, not silent code edits.

**Version**: 1.0.0 | **Ratified**: 2026-08-08 | **Last Amended**: 2026-08-08
