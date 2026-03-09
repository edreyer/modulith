# Refactoring This Repo to the OSS `workflow` Library

## Goal

Replace the custom workflow framework currently implemented in this repository with the OSS `workflow` library from [`edreyer/workflow`](https://github.com/edreyer/workflow), and migrate every current application workflow to a `useCase {}` implementation without weakening module boundaries.

This document is based on:

- the current local modulith codebase
- the current custom workflow implementation in `common`
- the local checkout of the OSS workflow library at `/Users/erikdreyer/dev/erik/workflow`
- the OSS workflow README at [`Readme.md`](https://github.com/edreyer/workflow/blob/main/Readme.md)

## Executive Summary

This migration is feasible, but it is not just a class-by-class replacement.

The custom framework is currently doing five distinct jobs:

1. defining request/event/error types
2. defining workflow execution semantics (`BaseSafeWorkflow`)
3. providing runtime dispatch (`WorkflowRegistry` + `WorkflowDispatcher`)
4. integrating with Spring (`WorkflowConfig`, Spring Integration gateway)
5. acting as an app-wide error model (`WorkflowError` subclasses in API modules, ports, adapters, controllers, and security)

The OSS library only solves job 2 well, and deliberately solves it differently:

- workflows are `Workflow<I : WorkflowState, O : WorkflowState>`
- orchestration is explicit via `useCase { startWith ... then ... }`
- results are `Either<io.liquidsoftware.workflow.WorkflowError, UseCaseEvents>`
- composition is centered on typed state transitions, not request-to-single-event handlers

That means the migration should be done in two layers:

1. decouple adapters and cross-module calls from the current generic dispatcher
2. re-implement each current workflow as an explicit `useCase {}` behind stable input-port interfaces

The strongest migration path is:

- keep CQRS semantics with local `Command` / `Query` marker interfaces that extend the OSS library `UseCaseCommand`
- add explicit input-port interfaces in `*-api`
- inject those ports into controllers and security instead of `WorkflowDispatcher`
- migrate each current workflow to a top-level `...UseCase` plus one or more internal stateful workflow steps
- remove `WorkflowRegistry`, `WorkflowDispatcher`, Spring Integration workflow wiring, and the old `common.workflow` package last

## Migration Status

- [x] Phase 1: extract explicit input-port boundaries from `WorkflowDispatcher` consumers
- [x] Phase 2: add the OSS workflow dependency to `common` and introduce `common.usecase`
- [x] Phase 3.1: migrate `SystemFindUserByEmail` to an OSS-backed `useCase {}` implementation
- [x] Phase 3.2: add temporary legacy bridge shims in `common.usecase.legacy`
- [x] Phase 3.3: migrate the remaining user lookup flows
- [x] Phase 3.4: migrate remaining user commands
- [x] Phase 3.5: migrate payment use cases
- [ ] Phase 3.6: migrate booking use cases
- [ ] Phase 4: remove the old workflow framework and Spring Integration workflow wiring

Completed so far:

- `*-api` seams now isolate controllers, security, and cross-module calls from the generic dispatcher
- `common` now exposes the OSS-library compatibility layer in `io.liquidsoftware.common.usecase`
- `common` now also exposes temporary legacy bridge helpers in `io.liquidsoftware.common.usecase.legacy`
- the root Maven build explicitly targets JVM 21 for Kotlin compilation so the OSS library compiles cleanly in this repo
- `SystemFindUserByEmail` is the first real migrated use case and is verified through `server` integration tests
- `FindUserById`, `FindUserByEmail`, and `FindUserByMsisdn` now also run on internal OSS-backed use cases while their legacy dispatcher workflows remain as thin adapters
- `RegisterUser`, `EnableUser`, and `DisableUser` now also run on internal OSS-backed use cases while their legacy dispatcher workflows remain as thin adapters
- `AddPaymentMethod` and `MakePayment` now also run on internal OSS-backed use cases while their legacy dispatcher workflows remain as thin adapters

What we learned from the first migrated use case:

- the OSS library itself is not the hard part; the repetitive work is bridging between legacy `common.workflow` request/event/error types and the new `common.usecase` / OSS types
- because the `*-api` modules still expose legacy `Command`, `Query`, `Event`, and `WorkflowError`, each migrated use case currently needs:
  - an internal OSS command/query type
  - a temporary legacy-to-OSS error bridge
  - sometimes a thin adapter workflow so parent/child Spring contexts keep working during the transition
- that repeated glue is now centralized in a temporary `io.liquidsoftware.common.usecase.legacy` package and should be deleted near the end of Phase 4
- once several workflows in the same bounded context share the same orchestration shape, it is worth extracting an internal OSS-backed base use case to keep the migrated code uniform instead of cloning one-off `useCase {}` chains
- access-sensitive behavior from the old workflows has to be preserved explicitly during migration; for example `RegisterUser` needed `runAsSuperUser` to wrap the whole migrated use case, not just the persistence step, because duplicate-user detection also depends on elevated access
- security-sensitive ownership rules should stay inside the bounded context that owns them; for example `MakePayment` still derives the paying user from `ExecutionContext` inside `payment`, then carries that user id through the OSS-backed flow instead of trusting a cross-module caller to supply it

## Current Workflow Inventory

### Custom framework surface

The current custom framework lives primarily in:

- [`common/src/main/kotlin/io/liquidsoftware/common/workflow/Workflow.kt`](../common/src/main/kotlin/io/liquidsoftware/common/workflow/Workflow.kt)
- [`common/src/main/kotlin/io/liquidsoftware/common/workflow/WorkflowRegistry.kt`](../common/src/main/kotlin/io/liquidsoftware/common/workflow/WorkflowRegistry.kt)
- [`common/src/main/kotlin/io/liquidsoftware/common/workflow/WorkflowDispatcher.kt`](../common/src/main/kotlin/io/liquidsoftware/common/workflow/WorkflowDispatcher.kt)
- [`common/src/main/kotlin/io/liquidsoftware/common/workflow/integration/DispatcherSupport.kt`](../common/src/main/kotlin/io/liquidsoftware/common/workflow/integration/DispatcherSupport.kt)
- [`common/src/main/kotlin/io/liquidsoftware/common/config/WorkflowConfig.kt`](../common/src/main/kotlin/io/liquidsoftware/common/config/WorkflowConfig.kt)
- [`common/src/main/kotlin/io/liquidsoftware/common/workflow/WorkflowErrors.kt`](../common/src/main/kotlin/io/liquidsoftware/common/workflow/WorkflowErrors.kt)

### Current application workflows

User module:

- [`user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/RegisterUserWorkflow.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/RegisterUserWorkflow.kt)
- [`user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/EnableUserWorkflow.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/EnableUserWorkflow.kt)
- [`user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/DisableUserWorkflow.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/DisableUserWorkflow.kt)
- [`user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/FindUserByIdWorkflow.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/FindUserByIdWorkflow.kt)
- [`user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/FindUserByEmailWorkflow.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/FindUserByEmailWorkflow.kt)
- [`user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/FindUserByMsisdnWorkflow.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/FindUserByMsisdnWorkflow.kt)
- [`user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/SystemFindUserByEmailWorkflow.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/SystemFindUserByEmailWorkflow.kt)

Payment module:

- [`payment/src/main/kotlin/io/liquidsoftware/base/payment/application/workflows/AddPaymentMethodWorkflow.kt`](../payment/src/main/kotlin/io/liquidsoftware/base/payment/application/workflows/AddPaymentMethodWorkflow.kt)
- [`payment/src/main/kotlin/io/liquidsoftware/base/payment/application/workflows/MakePaymentWorkflow.kt`](../payment/src/main/kotlin/io/liquidsoftware/base/payment/application/workflows/MakePaymentWorkflow.kt)

Booking module:

- [`booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/GetAvailabilityWorkflow.kt`](../booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/GetAvailabilityWorkflow.kt)
- [`booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/ScheduleAppointmentWorkflow.kt`](../booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/ScheduleAppointmentWorkflow.kt)
- [`booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/StartAppointmentWorkflow.kt`](../booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/StartAppointmentWorkflow.kt)
- [`booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/CompleteAppointmentWorkflow.kt`](../booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/CompleteAppointmentWorkflow.kt)
- [`booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/CancelAppointmentWorkflow.kt`](../booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/CancelAppointmentWorkflow.kt)
- [`booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/FetchUserAppointmentsWorkflow.kt`](../booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/FetchUserAppointmentsWorkflow.kt)
- [`booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/PayAppointmentWorkflow.kt`](../booking/src/main/kotlin/io/liquidsoftware/base/booking/application/workflows/PayAppointmentWorkflow.kt)

## What the OSS Library Gives Us

From the OSS workflow library:

- `Workflow<I, O>` where both `I` and `O` are `WorkflowState`
- `useCase {}` DSL with `startWith`, `then`, `thenIf`, `parallel`, `parallelJoin`, `thenLaunch`, and `awaitLaunched`
- typed state passing between steps
- step-level execution metadata and a structured sealed error model
- explicit orchestration instead of hidden runtime dispatch

The current repo is already compatible with the library runtime baseline:

- modulith: Java 21, Kotlin 2.3.10, Arrow 2.2.2
- workflow library: Java 21, Kotlin 2.3.0, Arrow 2.2.0

So dependency compatibility is not the problem.

The main problems are semantic and architectural.

## The Important Mismatches

### 1. The current app treats `WorkflowError` as an extensible domain error hierarchy

Today:

- `WorkflowError` is an open app class extending `RuntimeException`
- domain-specific errors in `user-api`, `booking-api`, and `payment-api` subclass it
- some of those error types carry `@ResponseStatus`

The OSS library:

- uses a sealed `WorkflowError`
- does not let the app define `UserExistsError`, `AppointmentNotFoundError`, `PaymentDeclinedError`, etc. as subclasses
- does not make `WorkflowError` a `Throwable`

This is the single biggest migration issue.

Implications:

- current `@ResponseStatus`-based control flow will stop working
- [`common/src/main/kotlin/io/liquidsoftware/common/web/ControllerSupport.kt`](../common/src/main/kotlin/io/liquidsoftware/common/web/ControllerSupport.kt) cannot keep throwing workflow errors because the library errors are not exceptions
- controller code that branches on specific workflow subclasses must be redesigned

### 2. The current app relies on generic runtime dispatch

Today:

- controllers and security inject `WorkflowDispatcher`
- `WorkflowRegistry` auto-registers handlers by request type
- one workflow can call another module through the dispatcher

The OSS library wants the opposite:

- explicit use-case composition
- direct injection of use-case entry points
- no runtime registry or message gateway

### 3. The current app models workflows as request -> single event

The OSS library models workflows as:

- input state -> output state
- plus zero or more emitted events

So every current use case needs explicit internal state types.

### 4. Query semantics are app-local, not library-local

The library has `UseCaseCommand`. It does not distinguish commands vs queries.

That is easy to solve locally, but it is still a design decision we should make explicitly.

## Recommended Target Architecture

### Keep CQRS semantics with a small compatibility layer in `common`

Introduce a new app-local package, for example `io.liquidsoftware.common.usecase`, with wrappers around the OSS library:

```kotlin
package io.liquidsoftware.common.usecase

import io.liquidsoftware.workflow.Event
import io.liquidsoftware.workflow.UseCaseCommand
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

interface Command : UseCaseCommand
interface Query : UseCaseCommand

abstract class AppEvent(
  final override val id: String = UUID.randomUUID().toString(),
  final override val timestamp: Instant = Clock.System.now(),
) : Event
```

This preserves:

- CQRS naming
- ergonomic event construction
- a single place where the app touches the OSS library’s command/event contracts

### Replace generic dispatcher calls with explicit input ports

Instead of rebuilding a registry on top of the new library, define input-port interfaces in `*-api` and inject them directly.

Recommended direction:

- `user-api` exposes `RegisterUserApi`, `FindUserApi`, `UserAdminApi`, `SystemFindUserByEmailApi`
- `payment-api` exposes `PaymentApi`
- `booking-api` exposes `BookingApi` and/or `GetAvailabilityApi`

That means:

- controllers call explicit use-case ports
- `SecurityConfig` calls `SystemFindUserByEmailApi`
- `PayAppointmentUseCase` calls `PaymentApi.makePayment(...)`
- the dispatcher disappears instead of being re-created

This is also more consistent with the repo’s own hexagonal rules: adapter-in should drive `application.port.in`, not a generic message bus.

### Top-level type naming

Use the following naming split:

- top-level orchestrators: `RegisterUserUseCase`, `MakePaymentUseCase`, `PayAppointmentUseCase`
- internal OSS-library workflow steps: `EnsureUserDoesNotExistStep`, `LoadPaymentMethodStep`, `PersistPaidAppointmentStep`

That avoids having both a “workflow” concept from the old app and a `Workflow<I, O>` concept from the OSS library competing under the same names.

### State placement

Prefer one of these two patterns:

1. private state classes nested inside the `...UseCase.kt` file for simple use cases
2. `application.workflows.steps` for reusable step workflows, with state classes kept private to the use case unless genuinely shared

Do not create a new generic dumping-ground package.

### Use the OSS library only inside the application layer

Controllers and security should not deal with `UseCaseEvents` directly.

Instead:

- each input-port implementation owns a `private val useCase = useCase<...> { ... }`
- the input-port implementation calls `useCase.execute(command)`
- it extracts the final typed domain event expected by the port

Example helper:

```kotlin
inline fun <reified E : Event> UseCaseEvents.requireLastEvent(useCaseId: String): Either<WorkflowError, E> =
  events.filterIsInstance<E>().lastOrNull()
    ?.let { Either.Right(it) }
    ?: Either.Left(
      WorkflowError.CompositionError(
        "Use case $useCaseId did not emit ${E::class.simpleName}",
        IllegalStateException("Missing expected event")
      )
    )
```

This keeps `UseCaseEvents` as an internal orchestration detail.

## Error Model Strategy

This migration needs an explicit decision before implementation starts.

### Option A: Recommended if you are willing to change the OSS library first

Add a built-in domain/business error variant to the OSS library.

Minimal example:

```kotlin
sealed class WorkflowError {
  data class ValidationError(val message: String) : WorkflowError()
  data class ExecutionError(val message: String) : WorkflowError()
  data class DomainError(
    val code: String,
    val message: String,
    val metadata: Map<String, String> = emptyMap()
  ) : WorkflowError()
  ...
}
```

Why this is the cleanest fit for this repo:

- this repo already relies heavily on typed domain failures
- controllers currently distinguish not-found, validation, unauthorized, and business-rule failures
- the ACL work already assumes boundary translation to workflow-level authorization failures
- it avoids flattening `UserExistsError`, `AppointmentNotFoundError`, `CancelAppointmentError`, `PaymentDeclinedError`, and authorization failures into unstructured messages

If you choose this option, I would migrate the app to the library after the library change is published.

### Option B: Migrate against the OSS library as it exists today

If the library stays unchanged, the app should stop treating workflow errors as typed domain subtypes and instead centralize mapping from the library error taxonomy to HTTP responses.

That means:

- remove `WorkflowError` subclasses from the API modules
- replace `@ResponseStatus`-annotated workflow errors with explicit controller mapping
- use `ValidationError` for input and invariant failures
- use `ExecutionError` for business failures and not-found cases
- use `ExceptionError` for infrastructure failures
- use a central `UseCaseErrorMapper` in `common.web`

This is workable, but weaker than what the repo currently has.

### My recommendation

Best overall path:

1. make the small OSS library error-model improvement first
2. then migrate this repo

If you do not want to touch the OSS library first, the rest of this document still applies, but every place that currently depends on typed workflow subclasses needs to be rewritten around explicit error mapping.

## Refactoring Sequence

### [x] Phase 1: Introduce explicit input ports before touching workflow internals

Do this while the old custom workflow system still exists.

Add input-port interfaces in `*-api`, for example:

```kotlin
interface PaymentApi {
  suspend fun addPaymentMethod(command: AddPaymentMethodCommand): Either<WorkflowError, PaymentMethodAddedEvent>
  suspend fun makePayment(command: MakePaymentCommand): Either<WorkflowError, PaymentMadeEvent>
}
```

Then:

- change controllers to inject these ports instead of `WorkflowDispatcher`
- change `SecurityConfig` to inject `SystemFindUserByEmailApi`
- change `PayAppointmentWorkflow` to inject `PaymentApi` instead of `WorkflowDispatcher`

This step is valuable even before the OSS library migration because it removes the framework coupling from the web/security boundaries.

Status:

- completed
- controllers, security, and `PayAppointmentWorkflow` now depend on explicit API seams instead of `WorkflowDispatcher`
- important runtime constraint discovered: parent `server` and child bounded-context modules live in separate Spring contexts, so bridge beans must respect that topology

### [x] Phase 2: Add the OSS library dependency and compatibility wrappers

Recommended dependency placement:

- add `io.liquidsoftware:workflow:0.5.0-SNAPSHOT` to `common`
- let `common` expose the app-local `Command`, `Query`, and `AppEvent` wrappers
- let downstream modules continue depending on `common`

Do not add the OSS library ad hoc to every module unless we discover a concrete need.

Status:

- completed
- the repo now has `io.liquidsoftware.common.usecase`
- the root `pom.xml` now sets Kotlin `jvmTarget` to `${maven.compiler.release}` so the OSS library’s Java 21 inline APIs compile cleanly in this build

### [ ] Phase 3: Migrate use cases module by module

Recommended order:

1. `user` queries and security lookup
2. remaining `user` commands
3. `payment`
4. `booking`
5. delete the old custom workflow framework

Why this order:

- `SecurityConfig` depends on user lookup
- `booking` depends on payment for `PayAppointment`
- once ports are explicit, `booking` can move cleanly after `payment`

Current sub-status:

- [x] 3.1 migrate `SystemFindUserByEmail`
- [x] 3.2 introduce temporary legacy bridge shims in `common.usecase.legacy`
- [x] 3.3 migrate remaining user lookup flows
- [x] 3.4 migrate remaining user commands
- [x] 3.5 migrate payment use cases
- [ ] 3.6 migrate booking use cases

Current temporary bridge files:

- [`common/src/main/kotlin/io/liquidsoftware/common/usecase/legacy/LegacyWorkflowErrorBridge.kt`](../common/src/main/kotlin/io/liquidsoftware/common/usecase/legacy/LegacyWorkflowErrorBridge.kt)
- [`common/src/main/kotlin/io/liquidsoftware/common/usecase/legacy/LegacyUseCaseAdapters.kt`](../common/src/main/kotlin/io/liquidsoftware/common/usecase/legacy/LegacyUseCaseAdapters.kt)

These exist only to reduce repeated glue during the migration. They should shrink over time and disappear entirely once:

- API modules stop exposing legacy `common.workflow` request/event/error types
- dispatcher-backed adapter workflows are no longer needed
- controllers, security, and application ports can deal directly with the OSS-library-facing contracts

Current user lookup migration shape:

- [`user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/UserLookupUseCases.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/UserLookupUseCases.kt) now centralizes the shared OSS-backed lookup chain for:
  - `FindUserById`
  - `FindUserByEmail`
  - `FindUserByMsisdn`
- the existing legacy workflows remain in place only as thin dispatcher adapters so parent/child Spring-context boundaries and public API contracts stay stable during the transition

Current user command migration shape:

- [`user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/UserCommandUseCases.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/application/workflows/UserCommandUseCases.kt) now centralizes the migrated OSS-backed command flows for:
  - `RegisterUser`
  - `EnableUser`
  - `DisableUser`
- `EnableUser` and `DisableUser` share a common internal OSS-backed base use case because they have the same load-and-persist orchestration shape
- `RegisterUser` remains separate because it combines duplicate detection, password encoding, domain construction, validation mapping, and elevated execution via `runAsSuperUser`

Current payment migration shape:

- [`payment/src/main/kotlin/io/liquidsoftware/base/payment/application/workflows/PaymentUseCases.kt`](../payment/src/main/kotlin/io/liquidsoftware/base/payment/application/workflows/PaymentUseCases.kt) now centralizes the migrated OSS-backed payment flows for:
  - `AddPaymentMethod`
  - `MakePayment`
- the existing legacy workflows remain in place only as thin dispatcher adapters so `payment-api` contracts and module-context boundaries stay stable during the transition
- `MakePayment` captures the authenticated user inside the payment bounded context before entering the OSS-backed workflow so payment-method ownership checks stay local to `payment`

### [ ] Phase 4: Remove the old framework

Delete or retire:

- `common.workflow.*`
- Spring Integration workflow gateway wiring
- `spring-boot-starter-integration` if nothing else still needs it
- dispatcher-based tests and utilities

## Shared Migration Rules for Every Use Case

### Rule 1: `startWith` should shape input, not hide orchestration

Good uses of `startWith`:

- copy command/query values into an initial state
- capture authenticated user id at the boundary
- convert a string role to a domain enum if trivial

Bad uses of `startWith`:

- doing persistence
- calling multiple ports
- hiding the entire use case in one lambda

### Rule 2: keep `ExecutionContext` at the boundary when possible

The current repo often reaches into `ExecutionContext` deep in workflows.

Prefer:

- controller or use-case wrapper captures the current user id
- `startWith` seeds it into state
- subsequent steps are deterministic

Exception:

- if security rules intentionally require resolution inside the bounded context, capture it in the top-level use-case implementation, not in arbitrary deep steps

### Rule 3: use step workflows for business actions, not service dumping

Good step workflows:

- `LoadUserByEmailStep`
- `EnsureAppointmentTimeAvailableStep`
- `PersistPaymentStep`

Bad step workflows:

- `UserServiceWorkflow`
- `BookingEverythingStep`

### Rule 4: do not use `parallel` or `parallelJoin` where the current flow is purely linear

Most current use cases are linear.

Immediate migration should use:

- `startWith`
- `then`
- very occasional `thenIf`

The richer OSS DSL becomes useful later for new use cases like notifications, telemetry, fan-out validation, and post-commit side effects.

### Rule 5: keep cross-module calls on API ports

`PayAppointmentUseCase` must depend on `payment-api`, not `payment`.

That was already true structurally, and the migration must preserve it.

## Workflow-by-Workflow Migration Plan

## User Module

### 1. Register user

Current implementation:

- checks for existing user by email
- encodes password
- constructs `UnregisteredUser`
- persists through `UserEventPort`
- runs under `runAsSuperUser`

Recommended top-level use case:

- `RegisterUserUseCase : RegisterUserApi`

Suggested state model:

- `RegisterUserState(msisdn, email, rawPassword, role)`
- `PasswordEncodedState(msisdn, email, encodedPassword, role)`
- `UnregisteredUserState(user: UnregisteredUser)`

Suggested chain:

```kotlin
private val useCase = useCase<RegisterUserCommand> {
  startWith { cmd ->
    Either.Right(
      RegisterUserState(
        msisdn = cmd.msisdn,
        email = cmd.email,
        rawPassword = cmd.password,
        role = Role.valueOf(cmd.role)
      )
    )
  }
  then(EnsureUserDoesNotExistStep("ensure-user-does-not-exist"))
  then(EncodePasswordStep("encode-password"))
  then(BuildUnregisteredUserStep("build-unregistered-user"))
  then(PersistRegisteredUserStep("persist-registered-user"))
}
```

Notes:

- keep `runAsSuperUser { ... }` at the `RegisterUserApi` boundary, not inside the internal step workflows
- `PersistRegisteredUserStep` emits `UserRegisteredEvent`
- this is a good early candidate for future `thenLaunch` after persistence if you later add email, audit, or analytics side effects

### 2. Enable user

Current implementation:

- load user by id
- raise not found if absent
- persist `UserEnabledEvent`

Recommended top-level use case:

- `EnableUserUseCase : UserAdminApi`

Suggested state model:

- `UserIdState(userId)`
- `FoundUserState(user)`

Suggested chain:

```kotlin
useCase<EnableUserCommand> {
  startWith { cmd -> Either.Right(UserIdState(cmd.userId)) }
  then(LoadUserByIdStep("load-user-by-id"))
  then(PersistUserEnabledStep("persist-user-enabled"))
}
```

### 3. Disable user

Mirror of enable-user.

Suggested chain:

```kotlin
useCase<DisableUserCommand> {
  startWith { cmd -> Either.Right(UserIdState(cmd.userId)) }
  then(LoadUserByIdStep("load-user-by-id"))
  then(PersistUserDisabledStep("persist-user-disabled"))
}
```

### 4. Find user by id

Current implementation is a single read step.

Recommended top-level use case:

- `FindUserByIdUseCase : FindUserApi`

Suggested state model:

- `FindUserByIdState(userId)`

Suggested chain:

```kotlin
useCase<FindUserByIdQuery> {
  startWith { query -> Either.Right(FindUserByIdState(query.userId)) }
  then(FindUserByIdStep("find-user-by-id"))
}
```

`FindUserByIdStep` should:

- load from `FindUserPort`
- raise not-found
- emit `UserFoundEvent`
- return the same or enriched state

### 5. Find user by email

Same pattern as find-by-id.

Suggested state:

- `FindUserByEmailState(email)`

Suggested chain:

```kotlin
useCase<FindUserByEmailQuery> {
  startWith { query -> Either.Right(FindUserByEmailState(query.email)) }
  then(FindUserByEmailStep("find-user-by-email"))
}
```

### 6. Find user by msisdn

Same pattern again.

Suggested state:

- `FindUserByMsisdnState(msisdn)`

Suggested chain:

```kotlin
useCase<FindUserByMsisdnQuery> {
  startWith { query -> Either.Right(FindUserByMsisdnState(query.msisdn)) }
  then(FindUserByMsisdnStep("find-user-by-msisdn"))
}
```

### 7. System find user by email

Status:

- completed
- implemented as an internal OSS-backed `SystemFindUserByEmailUseCase`
- the existing `SystemFindUserByEmailWorkflow` remains temporarily as a thin adapter so dispatcher-based callers and parent/child Spring context wiring continue to work during the transition

Current implementation:

- load user by email
- convert domain user to Spring `UserDetailsWithId`
- used by `SecurityConfig`

Recommended top-level use case:

- `SystemFindUserByEmailUseCase : SystemFindUserByEmailApi`

Suggested state model:

- `SystemLookupState(email)`
- `FoundUserState(user)`

Suggested chain:

```kotlin
useCase<SystemFindUserByEmailQuery> {
  startWith { query -> Either.Right(SystemLookupState(query.email)) }
  then(LoadUserByEmailStep("load-user-by-email"))
  then(MapUserToUserDetailsStep("map-user-to-user-details"))
}
```

Notes:

- this is the first user use case I would migrate because `SecurityConfig` should stop using `WorkflowDispatcher`
- the Spring Security-specific mapping should stay in the user bounded context, not in `server`
- in practice this migration also proved the need for temporary legacy shims:
  - the public API contract still returns legacy `common.workflow.WorkflowError`
  - the public query type still implements the legacy `Query` marker
  - an internal OSS command/query plus error bridge were needed to keep the API stable

## Payment Module

### 8. Add payment method

Current implementation:

- build `ActivePaymentMethod`
- persist through `PaymentEventPort`

Recommended top-level use case:

- `AddPaymentMethodUseCase : PaymentApi`

Suggested state model:

- `AddPaymentMethodState(userId, stripePaymentMethodId, lastFour)`
- `ActivePaymentMethodState(paymentMethod)`

Suggested chain:

```kotlin
useCase<AddPaymentMethodCommand> {
  startWith { cmd ->
    Either.Right(
      AddPaymentMethodState(
        userId = cmd.userId,
        stripePaymentMethodId = cmd.stripePaymentMethodId,
        lastFour = cmd.lastFour
      )
    )
  }
  then(BuildActivePaymentMethodStep("build-active-payment-method"))
  then(PersistPaymentMethodAddedStep("persist-payment-method-added"))
}
```

### 9. Make payment

Current implementation:

- validate `paymentMethodId`
- derive the acting user from `ExecutionContext`
- load payment method for that user
- call `StripeService.makePayment`
- build domain `Payment`
- persist `PaymentMadeEvent`

Recommended top-level use case:

- `MakePaymentUseCase : PaymentApi`

Suggested state model:

- `MakePaymentState(rawPaymentMethodId, amount, currentUserId)`
- `ValidatedPaymentRequestState(paymentMethodId: PaymentMethodId, userId: UserId, amount)`
- `LoadedPaymentMethodState(paymentMethod, userId, amount)`
- `AuthorizedChargeState(paymentMethod, userId, amount)`
- `PersistablePaymentState(payment)`

Suggested chain:

```kotlin
useCase<MakePaymentCommand> {
  startWith { cmd ->
    Either.Right(
      MakePaymentState(
        rawPaymentMethodId = cmd.paymentMethodId,
        amount = cmd.amount,
        currentUserId = executionContext.getCurrentUser().id
      )
    )
  }
  then(ValidatePaymentRequestStep("validate-payment-request"))
  then(LoadPaymentMethodStep("load-payment-method"))
  then(ProcessStripePaymentStep("process-stripe-payment"))
  then(BuildPaymentAggregateStep("build-payment-aggregate"))
  then(PersistPaymentMadeStep("persist-payment-made"))
}
```

Notes:

- keep the security-sensitive “who is paying” rule inside the payment bounded context
- do not change `MakePaymentCommand` to carry user id again
- if the OSS library remains unchanged, map `PaymentMethodNotFoundError` and `PaymentDeclinedError` to the chosen library error strategy at this boundary

## Booking Module

### 10. Get availability

Current implementation:

- validate requested date is not in the past
- load appointments for that date
- compute available times
- emit `AvailabilityRetrievedEvent`

Recommended top-level use case:

- `GetAvailabilityUseCase : GetAvailabilityApi`

Suggested state model:

- `AvailabilityRequestState(date)`
- `AvailabilityAppointmentsState(date, appointments)`

Suggested chain:

```kotlin
useCase<GetAvailabilityQuery> {
  startWith { query -> Either.Right(AvailabilityRequestState(query.date)) }
  then(ValidateAvailabilityDateStep("validate-availability-date"))
  then(LoadAppointmentsForAvailabilityStep("load-appointments-for-availability"))
  then(BuildAvailabilityStep("build-availability"))
}
```

### 11. Schedule appointment

Current implementation:

- load all appointments for the day
- ensure requested time is still available
- build `ReadyWorkOrder`
- build `ScheduledAppointment`
- persist `AppointmentScheduledEvent`

Recommended top-level use case:

- `ScheduleAppointmentUseCase : BookingApi`

Suggested state model:

- `ScheduleAppointmentState(userId, scheduledTime, duration, workOrderDto)`
- `AvailabilityCheckedState(userId, scheduledTime, duration, workOrderDto, appointmentsForDay)`
- `ScheduledAppointmentState(appointment)`

Suggested chain:

```kotlin
useCase<ScheduleAppointmentCommand> {
  startWith { cmd ->
    Either.Right(
      ScheduleAppointmentState(
        userId = cmd.userId,
        scheduledTime = cmd.scheduledTime,
        duration = cmd.duration,
        workOrderDto = cmd.workOrder
      )
    )
  }
  then(LoadAppointmentsForAvailabilityStep("load-appointments-for-availability"))
  then(EnsureRequestedTimeAvailableStep("ensure-requested-time-available"))
  then(BuildScheduledAppointmentStep("build-scheduled-appointment"))
  then(PersistAppointmentScheduledStep("persist-appointment-scheduled"))
}
```

Notes:

- keep availability logic in `AvailabilityService`
- the step workflows should orchestrate, not absorb domain rules that already belong in `ScheduledAppointment.of(...)`

### 12. Start appointment

Current implementation:

- load scheduled appointment
- convert to `InProgressAppointment`
- persist `AppointmentStartedEvent`

Recommended top-level use case:

- `StartAppointmentUseCase : BookingApi`

Suggested state model:

- `AppointmentIdState(appointmentId)`
- `ScheduledAppointmentState(appointment)`
- `InProgressAppointmentState(appointment)`

Suggested chain:

```kotlin
useCase<StartAppointmentCommand> {
  startWith { cmd -> Either.Right(AppointmentIdState(cmd.appointmentId)) }
  then(LoadScheduledAppointmentStep("load-scheduled-appointment"))
  then(BuildInProgressAppointmentStep("build-in-progress-appointment"))
  then(PersistAppointmentStartedStep("persist-appointment-started"))
}
```

### 13. Complete appointment

Current implementation:

- load in-progress appointment
- build completed appointment with notes
- persist `AppointmentCompletedEvent`

Recommended top-level use case:

- `CompleteAppointmentUseCase : BookingApi`

Suggested state model:

- `CompleteAppointmentState(appointmentId, notes)`
- `InProgressAppointmentState(appointment, notes)`
- `CompletedAppointmentState(appointment)`

Suggested chain:

```kotlin
useCase<CompleteAppointmentCommand> {
  startWith { cmd -> Either.Right(CompleteAppointmentState(cmd.appointmentId, cmd.notes)) }
  then(LoadInProgressAppointmentStep("load-in-progress-appointment"))
  then(BuildCompletedAppointmentStep("build-completed-appointment"))
  then(PersistAppointmentCompletedStep("persist-appointment-completed"))
}
```

### 14. Cancel appointment

Current implementation:

- load appointment
- call `AppointmentStateService.cancel`
- persist `AppointmentCancelledEvent`

Recommended top-level use case:

- `CancelAppointmentUseCase : BookingApi`

Suggested state model:

- `CancelAppointmentState(appointmentId, notes)`
- `LoadedAppointmentState(appointment, notes)`
- `CancelledAppointmentState(appointment)`

Suggested chain:

```kotlin
useCase<CancelAppointmentCommand> {
  startWith { cmd -> Either.Right(CancelAppointmentState(cmd.appointmentId, cmd.notes)) }
  then(LoadAppointmentByIdStep("load-appointment-by-id"))
  then(CancelAppointmentStateTransitionStep("cancel-appointment-state-transition"))
  then(PersistAppointmentCancelledStep("persist-appointment-cancelled"))
}
```

### 15. Fetch user appointments

Current implementation:

- load appointments for user + page
- filter cancelled appointments
- map to DTOs
- emit `UserAppointmentsFetchedEvent`

Recommended top-level use case:

- `FetchUserAppointmentsUseCase : BookingApi`

Suggested state model:

- `FetchUserAppointmentsState(userId, page, size)`
- `LoadedUserAppointmentsState(appointments)`

Suggested chain:

```kotlin
useCase<FetchUserAppointmentsQuery> {
  startWith { query ->
    Either.Right(FetchUserAppointmentsState(query.userId, query.page, query.size))
  }
  then(LoadUserAppointmentsStep("load-user-appointments"))
  then(FilterCancelledAppointmentsStep("filter-cancelled-appointments"))
  then(EmitUserAppointmentsFetchedStep("emit-user-appointments-fetched"))
}
```

For this use case, `FilterCancelledAppointmentsStep` may simply keep state as a list of active appointments plus emit the final event in the same step if you prefer not to split it further.

### 16. Pay appointment

This is the most important migration in the entire repo.

Current implementation:

- load completed appointment
- call payment module through `WorkflowDispatcher`
- build `PaidAppointment`
- persist `AppointmentPaidEvent`

Recommended top-level use case:

- `PayAppointmentUseCase : BookingApi`

Required architectural change first:

- inject `PaymentApi`, not `WorkflowDispatcher`

Suggested state model:

- `PayAppointmentState(appointmentId, paymentMethodId)`
- `CompletedAppointmentState(appointment, paymentMethodId)`
- `PaymentCapturedState(appointment, paymentMadeEvent)`
- `PaidAppointmentState(appointment)`

Suggested chain:

```kotlin
useCase<PayAppointmentCommand> {
  startWith { cmd -> Either.Right(PayAppointmentState(cmd.appointmentId, cmd.paymentMethodId)) }
  then(LoadCompletedAppointmentStep("load-completed-appointment"))
  then(CapturePaymentStep("capture-payment"))
  then(BuildPaidAppointmentStep("build-paid-appointment"))
  then(PersistAppointmentPaidStep("persist-appointment-paid"))
}
```

`CapturePaymentStep` should:

- call `paymentApi.makePayment(MakePaymentCommand(...))`
- map the returned `PaymentMadeEvent` into the next booking state
- never depend on payment implementation classes

This step is where the new architecture pays off:

- booking orchestrates through a stable `payment-api` contract
- no workflow registry is needed
- no hidden runtime dispatch remains

## Immediate Candidates for Advanced OSS DSL Features

Most current use cases are linear, so the initial migration should stay linear.

Still, a few natural future uses already exist:

- `thenLaunch`
  - register user: send welcome email, analytics, audit trail
  - make payment: send receipt, analytics
  - pay appointment: send payment confirmation

- `parallel`
  - post-commit side effects that should run together but not modify primary state

- `parallelJoin`
  - not strongly justified by the current code as written
  - could become useful only if future use cases genuinely need multiple independent reads that both produce downstream state

## Controller and Security Refactor

### Controllers

Current controllers should stop depending on `WorkflowDispatcher`:

- [`user/src/main/kotlin/io/liquidsoftware/base/user/adapter/in/web/RegisterUserController.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/adapter/in/web/RegisterUserController.kt)
- [`user/src/main/kotlin/io/liquidsoftware/base/user/adapter/in/web/api/v1/UserV1Controller.kt`](../user/src/main/kotlin/io/liquidsoftware/base/user/adapter/in/web/api/v1/UserV1Controller.kt)
- [`booking/src/main/kotlin/io/liquidsoftware/base/booking/adapter/in/web/api/v1/AppointmentController.kt`](../booking/src/main/kotlin/io/liquidsoftware/base/booking/adapter/in/web/api/v1/AppointmentController.kt)
- [`booking/src/main/kotlin/io/liquidsoftware/base/booking/adapter/in/web/api/v1/AvailabilityController.kt`](../booking/src/main/kotlin/io/liquidsoftware/base/booking/adapter/in/web/api/v1/AvailabilityController.kt)
- [`payment/src/main/kotlin/io/liquidsoftware/base/payment/adapter/in/web/api/v1/PaymentMethodController.kt`](../payment/src/main/kotlin/io/liquidsoftware/base/payment/adapter/in/web/api/v1/PaymentMethodController.kt)

Target shape:

- inject the explicit port interface
- call the specific use case
- map `WorkflowError` to HTTP response with an explicit mapper

Do not rebuild `throwIfSpringError()` around library errors.

### Security

[`server/src/main/kotlin/io/liquidsoftware/base/server/config/SecurityConfig.kt`](../server/src/main/kotlin/io/liquidsoftware/base/server/config/SecurityConfig.kt) should inject `SystemFindUserByEmailApi`.

Target shape:

- `UserDetailsService` calls `systemFindUserByEmailApi.findByEmail(SystemFindUserByEmailQuery(username))`
- on `Left`, translate to `UsernameNotFoundException`
- on `Right`, return `UserDetailsWithId`

This removes the workflow framework from Spring Security configuration entirely.

## Adapter and Port Refactor

The OSS library migration is easiest if we also clean up how `WorkflowError` leaks through ports.

Recommended first-pass approach:

- keep outbound ports returning `Either<WorkflowError, ...>` during the migration
- migrate helper functions like `bindValidation()` and `workflowBoundary()` to the OSS error type
- keep the port APIs stable while orchestrators are rewritten

After the migration is complete, reconsider whether outbound ports should return library `WorkflowError` directly or a narrower app-local failure model.

## Deletion Checklist

After all use cases are migrated and all controllers/security code uses explicit input ports, remove:

- `common/workflow/Workflow.kt`
- `common/workflow/WorkflowRegistry.kt`
- `common/workflow/WorkflowDispatcher.kt`
- `common/workflow/WorkflowErrors.kt`
- `common/workflow/integration/DispatcherSupport.kt`
- `common/config/WorkflowConfig.kt`
- old tests centered on `WorkflowDispatcher`
- the Spring Integration dependency if nothing else in the app still needs it

## Testing Plan

For each migrated use case:

1. keep or add unit tests for the top-level `...UseCase`
2. add focused tests for non-trivial step workflows
3. keep current web integration tests unchanged at the HTTP contract level as much as possible
4. add explicit tests for event extraction from `UseCaseEvents`
5. add one cross-module integration test for `PayAppointmentUseCase` calling `PaymentApi`

Recommended execution order while iterating:

- targeted module tests first
- then `server` integration tests for affected HTTP/security paths

## Recommended Implementation Order

If I were doing the actual refactor, I would do it in this order:

1. add explicit input-port interfaces in `*-api`
2. move controllers and `SecurityConfig` off `WorkflowDispatcher`
3. add `workflow` dependency and `common.usecase` wrappers
4. migrate user query use cases
5. migrate remaining user use cases
6. migrate payment use cases
7. migrate booking use cases except `PayAppointment`
8. migrate `PayAppointment` after `PaymentApi` is in place
9. delete the old workflow framework and Spring Integration wiring
10. clean up tests and documentation

## Open Decisions

These are the only decisions I think should be made explicitly before coding:

1. Do we want to enhance the OSS library error model first so this repo can keep typed domain/business failures?
2. Do we want grouped input-port interfaces per bounded context, or one interface per use case?
3. Do we want the first migration pass to keep outbound ports on `Either<WorkflowError, ...>`, or change that at the same time?

My answers:

1. yes, if you want the cleanest end state
2. grouped per bounded context is fine if the interface remains small and cohesive
3. keep outbound ports on `Either<WorkflowError, ...>` for the first pass, then revisit

## Bottom Line

The OSS library is a good fit for this repo’s direction, but the migration should treat `useCase {}` as a new application boundary model, not as a drop-in replacement for `BaseSafeWorkflow`.

The clean design is:

- explicit input ports
- explicit use-case orchestrators
- internal typed workflow steps
- no runtime registry
- no generic dispatcher
- no Spring Integration workflow gateway

If we do that, the repo ends up closer to its documented architecture than it is today, not just on a newer workflow implementation.
