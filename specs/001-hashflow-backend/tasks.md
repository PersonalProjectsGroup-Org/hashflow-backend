---

description: "Task list template for feature implementation"

---

# Tasks: Hashflow Backend Platform

**Input**: Design documents from `/specs/001-hashflow-backend/`

**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: Tests ARE requested for all business-rule logic (constitution principle V — test-first). Contract tests validate endpoints against the OpenAPI spec.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US5)
- Include exact file paths in descriptions

## Path Conventions

- Multi-module Gradle repo: `services/service-*/src/main/java/com/hashflow/*/...`
- Tests: `services/service-*/src/test/java/com/hashflow/*/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Repository-level initialization shared by all services

- [ ] T001 Bootstrap Gradle multi-module root (`settings.gradle.kts`) including all four services: `services:service-telemetry`, `services:service-financial`, `services:service-agent`, `services:service-news`
- [ ] T002 [P] Define shared build conventions in root `build.gradle.kts` (Java 21 toolchain, Spring Boot 3.3 plugin `apply false`, group `com.hashflow`, Checkstyle config)
- [ ] T003 [P] Add `.github/workflows/ci-telemetry.yml`, `ci-financial.yml`, `ci-agent.yml`, `ci-news.yml` (setup JDK 21 → Gradle build → Checkstyle → unit tests → integration tests → Docker build)
- [ ] T004 [P] Create `docker-compose.yml` with PostgreSQL 16 (one DB per service: `hashflow_telemetry`, `hashflow_financial`, `hashflow_agent`, `hashflow_news`), Redis 7, RabbitMQ 3-management

**Checkpoint**: `./gradlew build` succeeds with all four services compiling and CI workflows in place.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 [P] Verify/extend shared OpenAPI spec (`hashflow-infra/openapi/spec.yaml`) with all endpoints from plan Phase 0 Notes; add schemas `Rig`, `CreateRigRequest`, `Telemetry`, `ProfitabilitySnapshot`, `ImpactEvent`, `NewsItem`
- [ ] T006 [P] Set up DTO generation from OpenAPI into each service (`services/*/src/main/java/com/hashflow/*/dto/`)
- [ ] T007 [P] Configure Flyway + JPA per service (`src/main/resources/db/migration/`, `application.yml` with per-service datasource)
- [ ] T008 [P] Configure Actuator + Micrometer + structured logging per service (`src/main/resources/application.yml`)
- [ ] T009 [P] Add shared error handling (RFC-7807 problem+json) and correlation-ID filter per service
- [ ] T010 [P] Add contract test harness per service (validate endpoints against OpenAPI spec)

**Checkpoint**: Foundation ready — each service boots with health endpoint, DB migration, and generated DTOs.

---

## Phase 3: User Story 1 - Rig Lifecycle & Real-Time Telemetry Simulation (Priority: P1) 🎯 MVP

**Goal**: Register rigs and stream believable simulated telemetry (RN01–RN03) to the dashboard over SSE/WebSocket.

**Independent Test**: Register a rig, observe ±3% oscillation per cycle, force 85°C→THROTTLING (−50% hashrate), force 95°C→OFFLINE (+High-impact event), verify start/stop transitions.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T011 [P] [US1] Contract tests for `/api/rigs` (GET/POST), `/api/rigs/{rigId}` (PATCH/DELETE), `/api/rigs/{rigId}/telemetry` in `services/service-telemetry/src/test/java/com/hashflow/telemetry/contract/`
- [ ] T012 [P] [US1] Unit tests for RN01 (OFFLINE → hashrate 0, power 5%) in `.../simulation/RigSimulationEngineTest.java`
- [ ] T013 [P] [US1] Unit tests for RN02 (oscillation ≤ ±3% per cycle) in `.../simulation/RigSimulationEngineTest.java`
- [ ] T014 [P] [US1] Unit tests for RN03 (85°C → THROTTLING −50%; 95°C → OFFLINE + High impact) in `.../simulation/RigSimulationEngineTest.java`
- [ ] T015 [US1] Integration test for full lifecycle (register → simulate → throttle → shutdown) in `.../integration/RigLifecycleIntegrationTest.java`

### Implementation for User Story 1

- [ ] T016 [P] [US1] Create `Rig` entity + repository in `services/service-telemetry/src/main/java/com/hashflow/telemetry/model/` + Flyway migration `V1__create_rig.sql`
- [ ] T017 [P] [US1] Create `Telemetry` entity + repository + migration `V2__create_telemetry.sql`
- [ ] T018 [US1] Implement `RigService` (CRUD, start/stop transitions) in `.../service/RigService.java`
- [ ] T019 [US1] Implement `RigSimulationEngine` (RN01–RN03: oscillation, throttling, emergency shutdown, standby power) in `.../simulation/RigSimulationEngine.java`
- [ ] T020 [US1] Implement `TelemetryController` (REST snapshot + SSE stream) in `.../controller/TelemetryController.java`
- [ ] T021 [US1] Implement reading-cycle scheduler (default 10s) emitting telemetry via SSE and Redis Pub/Sub (`telemetry.<rigId>`) in `.../service/TelemetryStreamService.java`
- [ ] T022 [US1] Emit High-impact event on emergency shutdown (publish to `alerts.*` channel) in `.../simulation/RigSimulationEngine.java`
- [ ] T023 [US1] Add validation and error handling for rig operations

**Checkpoint**: US1 fully functional — a registered rig streams live telemetry; thermal events behave per RN01–RN03.

---

## Phase 4: User Story 2 - Financial Profitability per Rig (Priority: P2)

**Goal**: Compute net profit per rig per RN04 with live prices/difficulty and user kWh price.

**Independent Test**: With fixed price/difficulty inputs, net profit matches the RN04 formula exactly; kWh edit changes energy cost; offline rig uses standby power.

### Tests for User Story 2 ⚠️

- [ ] T024 [P] [US2] Contract tests for `/api/financial/profitability` in `services/service-financial/src/test/java/com/hashflow/financial/contract/`
- [ ] T025 [P] [US2] Unit tests for RN04 formula (gross revenue, energy cost, net profit) in `.../service/ProfitCalculatorTest.java`
- [ ] T026 [US2] Integration test: price/difficulty feed → profitability snapshot → dashboard payload in `.../integration/ProfitabilityIntegrationTest.java`

### Implementation for User Story 2

- [ ] T027 [P] [US2] Create `ProfitabilitySnapshot` entity + repository + migration in `services/service-financial/src/main/java/com/hashflow/financial/model/`
- [ ] T028 [P] [US2] Create `PricePoint` (price history) entity + repository + migration
- [ ] T029 [P] [US2] Implement external market client (coin price + network difficulty) with caching + staleness marker in `.../service/MarketDataClient.java`
- [ ] T030 [US2] Implement `ProfitCalculator` (RN04) in `.../service/ProfitCalculator.java`
- [ ] T031 [US2] Implement `ProfitabilityController` + kWh price configuration endpoint in `.../controller/`
- [ ] T032 [US2] Consume rig telemetry (from Redis Pub/Sub `telemetry.<rigId>`) for offline-rig standby power handling

**Checkpoint**: US2 works independently — profitability correct per RN04, kWh editable, offline rigs cost standby energy.

---

## Phase 5: User Story 3 - Impact Analysis & Notifications (Priority: P2)

**Goal**: Classify market/hardware impact (RN05–RN06) and dispatch notifications with cooldown (RN07) and retry.

**Independent Test**: Feed controlled price deltas and rig temperatures; verify LOW/MEDIUM/HIGH classification, channel selection, 15-min cooldown, and webhook backoff.

### Tests for User Story 3 ⚠️

- [ ] T033 [P] [US3] Contract tests for `/api/agent/alerts` in `services/service-agent/src/test/java/com/hashflow/agent/contract/`
- [ ] T034 [P] [US3] Unit tests for RN05 (10-minute delta window) in `.../service/VolatilityServiceTest.java`
- [ ] T035 [P] [US3] Unit tests for RN06 decision matrix (LOW/MEDIUM/HIGH + channels) in `.../service/ImpactClassifierTest.java`
- [ ] T036 [P] [US3] Unit tests for RN07 cooldown (15 min per coin/rig trigger) in `.../service/NotificationCooldownTest.java`
- [ ] T037 [US3] Integration test: price swing → classification → WebSocket + webhook dispatch in `.../integration/NotificationIntegrationTest.java`

### Implementation for User Story 3

- [ ] T038 [P] [US3] Create `ImpactEvent` + `NotificationDelivery` entities + repositories + migrations in `services/service-agent/src/main/java/com/hashflow/agent/model/`
- [ ] T039 [P] [US3] Implement `VolatilityService` (10-minute window, Delta %) in `.../service/VolatilityService.java` (subscribes to market price updates)
- [ ] T040 [P] [US3] Implement `ImpactClassifier` (RN06 matrix) in `.../service/ImpactClassifier.java`
- [ ] T041 [P] [US3] Implement `NotificationCooldown` (RN07, Redis-backed TTL 15 min) in `.../service/NotificationCooldown.java`
- [ ] T042 [US3] Implement `NotificationDispatcher` — WebSocket (Redis Pub/Sub relay), webhook, email in `.../service/NotificationDispatcher.java`
- [ ] T043 [US3] Implement webhook retry with exponential backoff (2s → 4s → 8s) + dead-letter via RabbitMQ in `.../service/WebhookRetryService.java`
- [ ] T044 [US3] Implement `AlertController` (alert history, "Nenhum alerta" empty state) in `.../controller/AlertController.java`
- [ ] T045 [US3] Consume High-impact events from `alerts.*` channel (emergency shutdown from US1)

**Checkpoint**: US3 works independently — impact classification correct, all channels fire per matrix, cooldown and retry verified.

---

## Phase 6: User Story 4 - Profit Switching Recommendations (Priority: P3)

**Goal**: Recommend coin switches for GPU rigs only when ≥10% more profitable over 24h (RN08–RN10).

**Independent Test**: Fixed hashrate curves + prices; verify threshold decision and that ASICs are never considered.

### Tests for User Story 4 ⚠️

- [ ] T046 [P] [US4] Unit tests for RN08 (GPU-only eligibility) in `services/service-financial/src/test/java/com/hashflow/financial/service/ProfitSwitchingEngineTest.java`
- [ ] T047 [P] [US4] Unit tests for RN09/RN10 (24h projection, ≥10% threshold) in `.../ProfitSwitchingEngineTest.java`
- [ ] T048 [US4] Integration test: switching recommendation → alert generated in `.../integration/ProfitSwitchingIntegrationTest.java`

### Implementation for User Story 4

- [ ] T049 [P] [US4] Create `SwitchingRecommendation` entity + repository + migration in `services/service-financial/src/main/java/com/hashflow/financial/model/`
- [ ] T050 [US4] Implement `ProfitSwitchingEngine` (RN08–RN10: GPU-only, 24h projection, 10% threshold) in `.../service/ProfitSwitchingEngine.java`
- [ ] T051 [US4] Implement switching recommendation endpoint + alert emission (to service-agent) in `.../controller/ProfitSwitchingController.java`

**Checkpoint**: US4 works independently — recommendations respect GPU-only eligibility and the 10% threshold.

---

## Phase 7: User Story 5 - News Aggregation & Sentiment Analysis (Priority: P3)

**Goal**: Import relevant crypto news (RN11) and tag sentiment (RN12) for the news feed.

**Independent Test**: Feed sample articles; verify relevance filter (supported coins) and sentiment assignment.

### Tests for User Story 5 ⚠️

- [ ] T052 [P] [US5] Contract tests for `/api/news` in `services/service-news/src/test/java/com/hashflow/news/contract/`
- [ ] T053 [P] [US5] Unit tests for RN11 relevance filter in `.../service/NewsRelevanceFilterTest.java`
- [ ] T054 [P] [US5] Unit tests for RN12 keyword sentiment (dictionary, tie → NEUTRAL) in `.../service/SentimentAnalyzerTest.java`
- [ ] T055 [US5] Integration test: fetch → filter → sentiment → feed payload in `.../integration/NewsIntegrationTest.java`

### Implementation for User Story 5

- [ ] T056 [P] [US5] Create `NewsArticle` entity + repository + migration in `services/service-news/src/main/java/com/hashflow/news/model/`
- [ ] T057 [P] [US5] Implement `NewsFetcher` (scheduled, configurable sources) in `.../service/NewsFetcher.java`
- [ ] T058 [P] [US5] Implement `NewsRelevanceFilter` (RN11, supported-coin dictionary) in `.../service/NewsRelevanceFilter.java`
- [ ] T059 [P] [US5] Implement `SentimentAnalyzer` (RN12 keyword dictionary) in `.../service/SentimentAnalyzer.java`
- [ ] T060 [US5] Implement `NewsController` (list with sentiment tags) in `.../controller/NewsController.java`

**Checkpoint**: US5 works independently — feed contains only relevant articles with correct sentiment tags.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T061 [P] End-to-end `docker-compose.yml` at repo root wiring all four services + PostgreSQL + Redis + RabbitMQ (BE-only, per project plan §9)
- [ ] T062 [P] NGINX load-balancer config with two instances of `service-telemetry` for chaos testing (per project plan §6)
- [ ] T063 Chaos engineering validation: kill one backend instance mid-transmission and verify failover with zero message loss (constitution principle IV)
- [ ] T064 [P] Load test with 100+ simulated rigs; verify sub-second telemetry delivery (SC-002)
- [ ] T065 [P] API rate limiting + security hardening on public endpoints (project plan Phase 4)
- [ ] T066 [P] Update `README.md` with architecture, local dev, and spec-kit workflow (AGENTS.md reference)
- [ ] T067 [P] Run `/speckit.converge` and resolve any remaining gaps between codebase and spec/plan/tasks

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational
  - US2 depends on US1 telemetry data (consumes `telemetry.<rigId>`); US3 depends on US1 High-impact events and US2 price updates; US4 depends on US2 profitability; US5 is independent
- **Polish (Final Phase)**: Depends on all desired user stories

### User Story Dependencies

- **User Story 1 (P1)**: After Foundational — no story dependencies (MVP)
- **User Story 2 (P2)**: After Foundational; consumes US1 telemetry
- **User Story 3 (P2)**: After Foundational; consumes US1 events + US2 price feed
- **User Story 4 (P3)**: After Foundational; depends on US2 profitability
- **User Story 5 (P3)**: After Foundational — independent

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Entities → repositories → services → controllers
- Core implementation before integration

### Parallel Opportunities

- All Phase 1/2 tasks marked [P] run in parallel
- US5 is fully independent and can start as soon as Foundational completes (even in parallel with US1)
- Tests within a story marked [P] run in parallel

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 (rig telemetry simulation) — the dashboard's core value
4. **STOP and VALIDATE**: simulate rigs, verify RN01–RN03, demo live telemetry

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → telemetry MVP (demo!)
3. US3 (alerts) → proactive monitoring (depends on US1 events)
4. US2 + US4 → profitability + switching
5. US5 → news feed (parallel-friendly)
6. Polish → chaos + load validation, CI/CD, deploy

### Parallel Team Strategy

With two developers (frontend + backend), backend stories can be distributed: US1+US2 by one, US3+US5 by another, after Foundational.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group (conventional commits per constitution)
- The OpenAPI contract in `hashflow-infra` must be updated before implementing any endpoint (contract-first)
