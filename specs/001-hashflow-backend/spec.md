# Feature Specification: Hashflow Backend Platform

**Feature Branch**: `001-hashflow-backend`

**Created**: 2026-08-08

**Status**: Draft

**Input**: User description: "Build the Hashflow backend: a crypto mining monitoring and automation platform (telemetry simulation, profitability, impact analysis, profit switching, news sentiment) that powers the real-time dashboard defined in the project plan and Figma design."

## User Scenarios & Testing

### User Story 1 - Rig Lifecycle & Real-Time Telemetry Simulation (Priority: P1)

The miner registers their mining rigs (GPU rigs like "RTX 4090 Beast" or ASICs like "Antminer S19 Pro") and watches live health data: hashrate, temperature, and power consumption. The platform simulates realistic hardware behavior — values oscillate on every reading cycle, a hot rig (>85°C) throttles itself to 50% hashrate, and a rig past 95°C shuts down to prevent burnout. The operator can turn a rig on or off and see its status change instantly (ONLINE / OFFLINE / THROTTLING).

**Why this priority**: This is the core value of the platform — the dashboard is useless without live, believable rig telemetry. Everything else (profitability, alerts, switching) consumes this data.

**Independent Test**: Can be fully tested by registering a rig, observing simulated telemetry cycles with oscillation, forcing a thermal event, and verifying the rig transitions ONLINE → THROTTLING → OFFLINE with the correct hashrate/power values. Delivers a working monitoring dashboard.

**Acceptance Scenarios**:

1. **Given** a rig with status `ONLINE`, **When** a reading cycle runs, **Then** hashrate and temperature change by at most ±3% from the previous cycle.
2. **Given** a rig whose component temperature exceeds 85°C, **When** the next reading cycle runs, **Then** the rig enters `THROTTLING` and hashrate drops to 50%.
3. **Given** a rig whose component temperature exceeds 95°C, **When** the next reading cycle runs, **Then** the rig is forced to `OFFLINE`, hashrate becomes 0, power drops to 5% of max (standby), and a High-impact event is emitted.
4. **Given** a rig set to `OFFLINE` by the operator, **When** telemetry is requested, **Then** hashrate is 0 and power consumption equals 5% of max.
5. **Given** any rig, **When** the operator issues a stop command, **Then** the rig transitions to `OFFLINE`; when a start command is issued, it returns to `ONLINE`.

### User Story 2 - Financial Profitability per Rig (Priority: P2)

The miner sees, for every rig, how much money it makes: gross revenue from mined crypto, energy cost, and net profit per day ("Lucro/dia", "Faturamento", "Custo Energia" on the dashboard). The platform pulls live coin prices and network difficulty from external APIs and lets the user set their kWh electricity price ("Clique para editar kWh").

**Why this priority**: Profit is the reason miners run the platform; it depends on US1 data but can be delivered as a distinct slice.

**Independent Test**: Can be fully tested by providing fixed price/difficulty inputs and verifying the net profit matches the RN04 formula exactly, including the kWh price edit.

**Acceptance Scenarios**:

1. **Given** a rig, current coin price, and network difficulty, **When** profitability is calculated, **Then** net profit equals `(Hashrate / Global Difficulty) × Block Reward × Coin Price − ((PowerWatts × 24) / 1000) × kWh Price`.
2. **Given** the user updates the kWh price, **When** profitability refreshes, **Then** the energy cost and net profit reflect the new value.
3. **Given** an offline rig (standby power = 5% max), **When** profitability is calculated, **Then** the energy cost reflects standby consumption, not full load.

### User Story 3 - Impact Analysis & Notifications (Priority: P2)

The miner is alerted when the market or their hardware becomes dangerous. The platform watches the 10-minute price delta (market volatility) and the hardware state, classifies the impact as LOW / MEDIUM / HIGH, and dispatches notifications through the appropriate channel: dashboard update only, WebSocket + webhook, or WebSocket + webhook + email. A spam-prevention cooldown blocks repeat emails for the same trigger for 15 minutes ("Alertas" panel on the dashboard shows "Nenhum alerta" when calm).

**Why this priority**: Alerts are what make the platform proactive rather than a passive dashboard; it depends on US1 data and market feeds.

**Independent Test**: Can be fully tested by feeding controlled price deltas and rig temperatures and verifying the impact classification, channel selection, and cooldown behavior end to end.

**Acceptance Scenarios**:

1. **Given** a coin price variation between −1.99% and +1.99%, **When** the agent evaluates the window, **Then** impact is `LOW` and only the dashboard is updated.
2. **Given** a coin price variation between ±2% and ±4.99%, **When** the agent evaluates, **Then** impact is `MEDIUM` and both WebSocket and webhook are notified.
3. **Given** a coin price variation ≥ ±5% or a rig at 95°C, **When** the agent evaluates, **Then** impact is `HIGH` and WebSocket + webhook + email are all notified.
4. **Given** a High-impact alert triggered for a coin/rig, **When** another High-impact trigger for the same coin/rig occurs within 15 minutes, **Then** no repeat email is sent (cooldown active).
5. **Given** the price delta is evaluated, **When** it is computed, **Then** it compares the current price with the price registered exactly 10 minutes earlier.

### User Story 4 - Profit Switching Recommendations (Priority: P3)

For GPU rigs only, the platform estimates the projected net profit of mining alternative algorithms/coins (e.g., Ethereum Classic vs Ravencoin) over the last 24 hours and recommends switching when the alternative is at least 10% more profitable than the current coin, preventing constant switching from small fluctuations.

**Why this priority**: Advanced optimization feature that builds on profitability (US2) and only affects GPU rigs.

**Independent Test**: Can be fully tested with fixed hashrate curves per algorithm and controlled prices; verify the 10% threshold decision and that ASIC rigs are never considered.

**Acceptance Scenarios**:

1. **Given** a GPU rig mining coin A, **When** the projected net profit of coin B over 24h is less than 10% higher than coin A, **Then** no switch is recommended.
2. **Given** a GPU rig mining coin A, **When** the projected net profit of coin B over 24h is ≥ 10% higher than coin A, **Then** a switch to coin B is recommended and an alert is generated.
3. **Given** an ASIC rig, **When** the switching algorithm runs, **Then** it is never considered for a switch.

### User Story 5 - News Aggregation & Sentiment Analysis (Priority: P3)

The miner reads a crypto news feed filtered for relevance to their supported coins, with color-coded sentiment tags (green = positive, red = negative, gray = neutral) from the design. Only articles mentioning a supported coin are imported; sentiment is assigned from a keyword dictionary.

**Why this priority**: Informational value-add; independent of rig telemetry.

**Independent Test**: Can be fully tested by feeding sample articles (BTC/Ethereum mentions and non-relevant articles) and verifying the relevance filter and sentiment assignment.

**Acceptance Scenarios**:

1. **Given** an article whose title or body mentions a supported coin name or ticker (e.g., BTC, Bitcoin, ETH, Ethereum), **When** it is ingested, **Then** it is imported; an article with no supported-coin mention is rejected.
2. **Given** an imported article containing negative terms (Ban, Crash, Regulation, Hack, Attack), **When** sentiment is assigned, **Then** the result is `NEGATIVE`.
3. **Given** an imported article containing positive terms (Rally, Adoption, Partnership, Upgrade, Record), **When** sentiment is assigned, **Then** the result is `POSITIVE`.
4. **Given** an imported article with no keyword matches or a tie, **When** sentiment is assigned, **Then** the result is `NEUTRAL`.

### Edge Cases

- What happens when the external price/difficulty API is unavailable? (Graceful degradation: last-known values + staleness marker; profitability calculations pause rather than crash.)
- How does the system handle a rig that oscillates around the 85°C/95°C thresholds? (Hysteresis to avoid rapid flapping between THROTTLING/ONLINE.)
- What happens when multiple High-impact triggers fire for different coins simultaneously? (Cooldown is per coin/rig trigger, so unrelated alerts still dispatch.)
- How is a webhook that never acknowledges handled? (Exponential backoff retry 2s → 4s → 8s, then dead-letter.)
- What happens when the WebSocket connection drops mid-broadcast? (Redis Pub/Sub relays ensure remaining instances still deliver; clients reconnect with last-known state.)
- What happens if a rig is deleted while a simulation cycle is running? (Cycle aborts gracefully; no orphan telemetry writes.)

## Requirements

### Functional Requirements

- **FR-001**: System MUST allow registering, listing, updating, and deleting mining rigs (GPU and ASIC), each with name, type, algorithm, max hashrate, and max power.
- **FR-002**: System MUST generate telemetry for every `ONLINE` rig on a configurable reading cycle (default 10s), applying a random oscillation factor of at most ±3% to hashrate and temperature.
- **FR-003**: System MUST enforce thermal protection: >85°C triggers `THROTTLING` (hashrate −50%); >95°C forces `OFFLINE` (emergency shutdown) and emits a High-impact event.
- **FR-004**: System MUST force hashrate to 0 and power to 5% of max (standby) whenever a rig is `OFFLINE`.
- **FR-005**: System MUST support operator start/stop commands that transition a rig between `ONLINE` and `OFFLINE`.
- **FR-006**: System MUST stream live telemetry to consumers via real-time channels (WebSocket for alert-level events, SSE for periodic metrics).
- **FR-007**: System MUST calculate net profit per rig strictly per the RN04 formula using external coin prices and network difficulty.
- **FR-008**: System MUST allow the user to configure the kWh electricity price used in energy cost calculations.
- **FR-009**: System MUST compute market volatility as the percentage delta between the current coin price and the price exactly 10 minutes earlier.
- **FR-010**: System MUST classify impact per the RN06 matrix (LOW/MEDIUM/HIGH) and dispatch notifications to the matching channels (dashboard / WebSocket+webhook / WebSocket+webhook+email).
- **FR-011**: System MUST enforce a 15-minute notification cooldown preventing repeat email for the same coin/rig High-impact trigger.
- **FR-012**: System MUST retry failed webhooks with exponential backoff (2s → 4s → 8s) and a dead-letter path.
- **FR-013**: System MUST run the profit-switching algorithm exclusively for GPU rigs, comparing 24h projected net profit across algorithms.
- **FR-014**: System MUST recommend a switch only when the alternative coin's net profit is ≥10% higher than the current coin, and generate an alert on recommendation.
- **FR-015**: System MUST ingest news articles only when the title or body mentions a supported coin name or ticker.
- **FR-016**: System MUST assign sentiment (POSITIVE/NEGATIVE/NEUTRAL) from the keyword dictionary per RN12.
- **FR-017**: System MUST expose all public endpoints per the OpenAPI contract in the shared `hashflow-infra` repo (Rigs, Telemetry, News, plus financial/alert surfaces).
- **FR-018**: System MUST expose health and metrics endpoints (Actuator/Micrometer) for the dashboard and ops.
- **FR-019**: System MUST keep every service instance stateless; real-time state must be recoverable after any single instance is killed (chaos engineering requirement).

### Key Entities

- **Rig**: A mining machine (GPU or ASIC) with name, type, algorithm, maxHashrate, maxPowerWatts, and status (`ONLINE`/`OFFLINE`/`THROTTLING`). Belongs to an operator.
- **Telemetry**: Per-cycle snapshot for a rig: hashrate, temperature, powerWatts, status, timestamp.
- **PricePoint / PriceHistory**: Coin price samples with timestamp, used for the 10-minute volatility window.
- **ProfitabilitySnapshot**: Per-rig computed gross revenue, energy cost, and net profit for a window.
- **ImpactEvent / Alert**: Classified event (LOW/MEDIUM/HIGH) with trigger (coin delta or hardware), channels, and cooldown bookkeeping.
- **NotificationDelivery**: Webhook/email delivery record with retry state (backoff schedule, dead-letter).
- **SwitchingRecommendation**: Proposed coin switch with projected profit comparison and decision.
- **NewsArticle**: Imported article with source, title/body, relatedCoins, sentiment, publishedAt.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Registered rigs begin producing telemetry within one reading cycle (≤10s) of going `ONLINE`.
- **SC-002**: Telemetry stream sustains 100+ simulated rigs with sub-second delivery to connected dashboard clients without loss.
- **SC-003**: A rig crossing 95°C transitions to `OFFLINE` and emits a High-impact alert within one reading cycle.
- **SC-004**: Net profit calculations match the RN04 formula exactly (verified by contract tests) with prices/difficulty no older than 10 minutes in normal operation.
- **SC-005**: A High-impact email is delivered at most once per coin/rig per 15-minute window (cooldown verified).
- **SC-006**: 100% of the endpoints exposed by services are present in the shared OpenAPI spec (contract coverage).
- **SC-007**: Killing any single backend instance mid-transmission causes no message loss for connected clients (chaos test passes).
- **SC-008**: News feed imports only relevant articles and sentiment accuracy on a labeled test corpus ≥ 90%.

## Assumptions

- The frontend (`hashflow-frontend`) and shared contract repo (`hashflow-infra`) are developed by the frontend owner in parallel; this spec covers the backend only.
- No user authentication is required in this first phase (single-operator assumption); auth is a future feature.
- Market price and difficulty feeds come from public APIs; when unavailable, the system degrades to last-known values marked stale rather than failing.
- The reading cycle defaults to 10 seconds but is configurable per environment.
- ASIC rigs are algorithm-locked and never participate in profit switching (RN08).
- The dashboard consumes WebSockets (alert-level) and SSE (periodic metrics) as defined in the design; the backend provides both.
- Notification cooldown is per (coin, rig) trigger pair, allowing unrelated High-impact alerts to dispatch during cooldown.
- This feature delivers the backend for the dashboard described in the Figma design, including the rigs, alerts, market, and news surfaces.
