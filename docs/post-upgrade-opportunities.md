# Post-Upgrade Opportunities

This note captures Spring Boot 4 / Spring Framework 7 features that are worth considering after the upgrade branch is stabilized.

## Recommended Next Step

### Coroutine Context Propagation

This repo already uses Kotlin coroutines broadly in controllers, workflows, and adapters. Spring Boot 4 can improve observability across coroutine boundaries by enabling Reactor/Micrometer context propagation.

Implementation added on this branch:

- `io.micrometer:context-propagation` in the root Maven build
- `spring.reactor.context-propagation=auto` in the `server` application config

Why this is useful:

- It aligns the app with Spring Boot 4's coroutine/observability model.
- It preserves Micrometer observation and trace context when execution hops across coroutine boundaries.
- It is low-risk and does not require changing the workflow or controller APIs.

What it does not replace:

- The custom [`SecurityCoroutineContext`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/SecurityCoroutineContext.kt) is still needed for Spring Security's `SecurityContextHolder` propagation in the current design.
- This change improves observability context propagation. It is not a replacement for the repo's explicit security-context propagation.

Verification:

- `mvn clean test`

## Other Spring 4 Opportunities

### RestTestClient

Spring Framework 7 adds `RestTestClient`, which could replace part of the current Rest Assured integration-test usage with a more Spring-native client.

Suggested follow-up:

- pilot it in one `server` integration test class before deciding whether to migrate broadly

### API Versioning

Spring Boot 4 adds API versioning support. Since the repo already uses `/v1` endpoints, this may become useful when a `v2` surface is needed.

Suggested follow-up:

- revisit only when a second API version is actually planned

### HTTP Service Clients

If the system grows more outbound HTTP integrations, Spring's HTTP service clients are a good typed-client option.

Suggested follow-up:

- defer until a real external HTTP integration is added

### Spring Framework Resilience Features

Spring Framework 7 now includes retry and concurrency-limit features in core Spring. These may be useful later for outbound payment or notification integrations.

Suggested follow-up:

- evaluate when outbound calls become latency- or failure-sensitive
