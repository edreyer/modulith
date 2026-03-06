# Project Guidelines

## Purpose
This repository is a Kotlin/Spring Boot modular monolith built around hexagonal architecture, CQRS, DDD, and functional programming practices.

The primary architectural rule is strict isolation:

- Each Maven module is an isolation boundary.
- A module may support one or more bounded contexts, but those boundaries must remain explicit.
- Boundaries are enforced at compile time through Maven artifacts and at runtime through separate Spring application contexts.
- Preserve that separation. Do not introduce shortcuts that couple one module's implementation to another module's implementation.

## Current Module Layout

The root build is Maven-based. Current modules include:

- `common`
- `server`
- `user`
- `user-api`
- `booking`
- `booking-api`
- `payment`
- `payment-api`

In the current structure, the `*-api` modules define contracts that other modules can depend on. The concrete bounded-context modules (`user`, `booking`, `payment`) depend on `common` plus their own API module. The `server` module composes the application.

## Architecture Rules

### Module Boundaries
- Treat every Maven module as a hard boundary.
- Do not add direct implementation dependencies from one bounded-context module to another.
- Cross-module communication should happen through published contracts, not through importing another module's internal implementation classes.
- If a new bounded context is needed, prefer a new Maven module rather than folding unrelated behavior into an existing one.

### Package Layout
Within a bounded context, follow this package structure:

- `domain`
- `application.workflows`
- `application.port.in`
- `application.port.out`
- `application.service` when orchestration or reusable application logic is needed
- `adapter.in.web`
- `adapter.out.persistence`
- `config`

Keep names aligned with the actual package structure already used in this repository. Do not collapse everything into generic `application` or `adapter` packages when a more specific subpackage already exists.

### Dependency Direction
- `domain` is the core and must not depend on `application`, `adapter`, or framework details.
- `application` coordinates use cases and depends on the domain plus ports.
- `application.workflows` must not depend directly on adapter implementations.
- `adapter.in` drives the application through `application.port.in`.
- `adapter.out` implements `application.port.out`.
- `config` wires the module together without leaking framework concerns into the domain.

These constraints are not aspirational only; the repository already enforces key parts of them with ArchUnit tests. Changes should preserve or strengthen those tests.

## Coding Style

### Kotlin Style
- Write idiomatic Kotlin, not Java written in Kotlin syntax.
- Prefer properties over trivial `get...()` accessors unless a framework interface requires a getter-style method name.
- Use descriptive names based on domain language.
- Prefer immutable data and narrow, specific types over primitive strings and numbers where business meaning exists.
- Prefer sealed interfaces/classes, exhaustive `when` expressions, and explicit variant types over open-ended hierarchies and flag-based modeling.

### Domain Modeling
- Model domain concepts with ADTs, sealed hierarchies, and value objects where appropriate.
- Keep domain objects immutable.
- Push invariants into types and constructors/factories so failures happen as early as possible.
- Prefer compile-time and validation guarantees over scattered runtime checks.
- Make illegal states unrepresentable whenever practical.
- Prefer refined domain primitives built with `@JvmInline value class` when they encode meaningful invariants without adding object overhead.
- Use private constructors plus companion factory methods or extension-based factories to ensure invalid instances cannot be created directly.
- Prefer non-nullability by design. Represent optionality, lifecycle, and state variants with types rather than nullable fields when the distinction is domain-significant.
- Encode valid state transitions in function signatures when possible so the compiler prevents invalid flows.
- Keep construction-time validation centralized near the type being created rather than scattered across workflows, controllers, and adapters.

### Functional Error Handling
- Use Arrow-based error handling patterns already present in the codebase.
- Prefer `Either`, `Raise`, `either {}`, and related helpers for business logic.
- Prefer typesafe error handling to exceptions in domain and workflow code.
- Avoid throwing exceptions in workflows and domain logic except at clear framework boundaries.
- Model failure modes explicitly as sealed error types or other closed algebraic hierarchies.
- Treat `context(_: Raise<...>)` requirements as part of a function's effective signature.
- Use `either {}` at the same architectural points where exception handling would otherwise be introduced, so error handling remains explicit and compile-time enforced.
- Translate low-level errors into higher-level domain or workflow errors at layer and module boundaries rather than leaking infrastructure-shaped errors upward.
- Do not ignore `Either` results or discard the outcome of `either {}` blocks when the error path matters.
- Prefer accumulated validation errors where the caller benefits from seeing all invalid inputs at once, using Arrow combinators such as `zipOrAccumulate`.
- Use context parameters with `Raise` where they improve clarity and match existing repository patterns.
- When handling sealed error types or enums, prefer exhaustive `when` expressions and avoid `else` branches that disable compile-time exhaustiveness checks.

### Coroutines
- Use `suspend` functions for asynchronous work.
- Keep blocking calls out of request and workflow paths.
- Use the repository's coroutine helpers such as `withContextIO` for IO-bound operations where appropriate.

### Comments and Documentation
- Use KDoc for public APIs and for internal components whose behavior is not obvious.
- Add comments for design decisions and non-obvious behavior, not to narrate simple code.
- Keep documentation aligned with the current architecture and package structure.

## Workflow and Use Cases
- Workflows are the main application use-case units.
- Keep workflows focused on orchestration, validation flow, and coordination.
- Put business rules in the domain model or narrow application services, not in controllers or persistence adapters.
- Maintain the repository's command/query/event style for inter-module and application interactions.
- Do not accept broad, weakly-typed domain objects when a narrower state-specific type would express the valid input more precisely.

## Testing Expectations
- Preserve and extend ArchUnit coverage for architectural rules.
- Add or update unit tests for domain and workflow changes.
- Add integration tests when adapter behavior, web behavior, security behavior, or persistence behavior changes.
- Prefer targeted Maven test runs while iterating, then broaden as confidence increases.

Useful commands:

- `mvn test`
- `mvn -pl user test`
- `mvn -pl booking test`
- `mvn -pl payment test`
- `mvn -pl server test`

## Change Guidance For Agents and Humans
- Start by understanding the bounded context and module being changed.
- Make the smallest change that preserves the architecture.
- Do not bypass ports just because another class is easy to import.
- Do not move domain logic into controllers, configs, or persistence adapters.
- Do not weaken package or module boundaries to make a change easier.
- If a change requires a new dependency across a boundary, treat that as an architectural decision and justify it explicitly.

## When Adding New Functionality
- Decide which bounded context owns the behavior first.
- If the behavior belongs to a new bounded context, create a new Maven module and expose only the contract other modules need.
- If the behavior belongs to an existing bounded context, keep the implementation inside that module and expose it through the established ports/workflows/API patterns.
- Update or add ArchUnit tests when introducing new package areas or new module-level rules.

## Source of Truth
The README describes the intended architecture of this project and should remain consistent with this file. When the README and code diverge, prefer the actual enforced architecture in the code and update the documentation.
