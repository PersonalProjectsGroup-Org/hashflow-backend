---
name: Pull Request
about: Submit changes following the Hashflow Spec-Driven Development workflow
title: "[FEATURE] Brief description"
labels: ""
assignees: ""
---

## Summary

<!-- What does this PR do and why? Keep it focused on the WHAT and the WHY, not the HOW. -->

Closes #<!-- issue number, if any -->

## Spec-Driven Development Traceability

<!-- Every change must trace back to the artifact pipeline. Fill in what applies. -->

- **Feature directory**: `specs/...` <!-- e.g. specs/001-hashflow-backend -->
- **Spec**: link to `spec.md`
- **Plan**: link to `plan.md`
- **Tasks**: list of task IDs covered (e.g. T011–T023)

## Constitution Compliance

- [ ] **Contract-first** — any new/changed endpoint was added to the shared OpenAPI spec (`hashflow-infra/openapi/spec.yaml`) and DTOs regenerated *before* controller code
- [ ] **Business rules** — behavior follows RN01–RN12 exactly; list affected rules: <!-- e.g. RN02, RN03 -->
- [ ] **Microservice isolation** — no cross-service database access; communication via API / Redis Pub/Sub / RabbitMQ only
- [ ] **Real-time by default** — stateless instances; WebSocket/SSE flows respect the no-refresh contract
- [ ] **Test-first** — tests were written before implementation (Red-Green-Refactor)
- [ ] **Observability** — Actuator/Micrometer metrics and structured logging included where applicable

## Testing

<!-- Show the evidence: which tests were added/changed and how to run them. -->

- [ ] Unit tests: `./gradlew :services:service-<name>:test`
- [ ] Integration tests: `./gradlew :services:service-<name>:integrationTest` (or equivalent)
- [ ] Contract tests against OpenAPI spec
- [ ] Checkstyle passes: `./gradlew check`
- [ ] Full build: `./gradlew build`

## Changes

<!-- Bullet list of the main changes, grouped logically. -->

- ...

## Screenshots / Recordings (if UI or dashboard-affecting)

<!-- Drag and drop screenshots here. -->

## Checklist

- [ ] Code follows the [Hashflow Backend Constitution](../../.specify/memory/constitution.md)
- [ ] Conventional commit message used (`feat:`, `fix:`, `build:`, `chore:`, `refactor:`)
- [ ] No unrelated changes included
- [ ] Documentation updated if behavior or contracts changed (spec/plan/tasks reflect reality)
- [ ] `/speckit.converge` run (or explained why not applicable)
