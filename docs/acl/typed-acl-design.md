# Typed ACL Design

## Goal

Keep the current in-project ACL model, but evolve it so authorization denial is handled as data instead of exceptions.

This aligns the security layer with the rest of the codebase:

- domain and application logic prefer typed outcomes
- Arrow `Raise` / `Either` is the preferred mechanism for expected failures
- Spring-specific exception mapping should stay at the transport boundary, not inside core authorization logic

## Current state

[`Acl.kt`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/acl/Acl.kt) currently exposes:

- `hasPermission(...) : Boolean`
- `checkPermission(...) : Unit`, which throws `UnauthorizedAccessException`

That means expected authorization denial is currently modeled as an exception and later translated back into [`UnauthorizedWorkflowError`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/workflow/WorkflowErrors.kt).

This works, but it has drawbacks:

- expected denials look exceptional
- adapters need exception translation code
- collection reads are easier to implement with silent filtering patterns
- authorization semantics are harder to compose with Arrow workflows

## Design direction

### 1. Keep the ACL model itself

Do not replace the current `Acl`, `AclRole`, or `Permission` model.

This project already has a simple and understandable authorization shape:

- resources own ACL data
- permissions are `READ`, `WRITE`, `MANAGE`
- admin/system access is handled centrally

That is good enough for the current scope.

### 2. Replace exception-based denial with typed denial

Introduce a security-layer error type that is independent of web and workflow concerns.

Example:

```kotlin
sealed interface AuthorizationError {
  val resourceId: String
  val permission: Permission
}

data class PermissionDenied(
  override val resourceId: String,
  override val permission: Permission
) : AuthorizationError
```

This keeps authorization semantics in the security layer rather than collapsing them immediately into `WorkflowError`.

### 3. Prefer `Raise` for internal ACL enforcement

For internal use, the most natural API is:

```kotlin
context(_: Raise<AuthorizationError>)
suspend fun ensurePermission(acl: Acl, permission: Permission)
```

This should internally use the existing boolean logic from `hasPermission(...)`.

Example:

```kotlin
context(_: Raise<AuthorizationError>)
suspend fun ensurePermission(acl: Acl, permission: Permission) {
  ensure(hasPermission(acl, ec.getUserAccessKeys(), permission)) {
    PermissionDenied(acl.resourceId, permission)
  }
}
```

Why `Raise`:

- matches the rest of the codebase
- composes naturally in workflows and adapters
- avoids building temporary `Either` values unless needed at a boundary

### 4. Materialize `Either` only at the boundary

Where adapter or workflow boundaries already return `Either<WorkflowError, ...>`, translate authorization failures once.

Example shape:

```kotlin
private fun AuthorizationError.toWorkflowError(): WorkflowError =
  UnauthorizedWorkflowError("No access to: $resourceId Permission: $permission")
```

Then at the boundary:

```kotlin
either {
  either<AuthorizationError, Unit> {
    aclChecker.ensurePermission(appointment.acl(), Permission.READ)
  }.fold(
    { raise(it.toWorkflowError()) },
    {}
  )
}
```

If a small project helper is useful, that translation can be wrapped in one repo-specific extension.

## Recommended API surface

### Keep

- `hasPermission(...) : Boolean`

This is useful for explicit policy checks and collection filtering where that behavior is truly intended.

### Add

- `context(_: Raise<AuthorizationError>) suspend fun ensurePermission(...)`

### Remove eventually

- `checkPermission(...)` that throws `UnauthorizedAccessException`

This should be migrated away gradually and then deleted.

## Additional design improvements

These changes are not required for the typed-ACL migration, but they would make the ACL system easier to use and reason about.

### 1. Replace the implicit subject shape with a typed one

Today [`AclChecker`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/acl/Acl.kt) relies on a `List<String>` convention where the first entry is the user ID and the remaining entries are authorities.

That is brittle and not self-documenting.

Recommended direction:

```kotlin
data class AccessSubject(
  val userId: String,
  val roles: Set<String>
)
```

Then the core ACL API can operate on a typed subject instead of positional list semantics.

### 2. Keep convenience APIs, but make the core policy explicit

It is fine for the convenience layer to derive the current subject from [`ExecutionContext`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/ExecutionContext.kt), but the core ACL logic should accept a subject explicitly.

That gives the best of both worlds:

- convenient production call sites
- deterministic tests
- less hidden coupling

Example shape:

```kotlin
suspend fun currentSubject(): AccessSubject

context(_: Raise<AuthorizationError>)
suspend fun ensurePermission(subject: AccessSubject, acl: Acl, permission: Permission)
```

### 3. Add permission-specific convenience helpers

At current call sites, repeated `Permission.READ`, `WRITE`, and `MANAGE` arguments create noise.

Add small wrappers:

```kotlin
context(_: Raise<AuthorizationError>)
suspend fun ensureCanRead(acl: Acl)

context(_: Raise<AuthorizationError>)
suspend fun ensureCanWrite(acl: Acl)

context(_: Raise<AuthorizationError>)
suspend fun ensureCanManage(acl: Acl)
```

These can delegate to the general `ensurePermission(...)` API while making usage cleaner and more intention-revealing.

### 4. Move admin/system bypass into explicit policy code

Current admin/system bypass behavior is hardcoded inside [`AclChecker`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/acl/Acl.kt).

That behavior is valid, but it should be isolated as policy rather than mixed into all permission logic.

This makes future changes easier if authorization rules become more nuanced.

Example direction:

```kotlin
private fun AccessSubject.hasGlobalAccess(): Boolean =
  "ROLE_ADMIN" in roles || "ROLE_SYSTEM" in roles
```

### 5. Add clearer ACL factory methods for common cases

[`Acl.of(...)`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/acl/Acl.kt) is compact but low-level.

Consider adding more expressive factories for common resource shapes:

```kotlin
Acl.ownerManaged(resourceId, ownerUserId)
Acl.readOnly(resourceId, readerUserId)
```

This reduces role-wiring repetition and makes intent clearer at aggregate construction sites.

### 6. Add richer denial metadata

When authorization becomes typed, the error should carry enough structure for debugging and testing.

Recommended fields:

- `resourceId`
- `permission`
- `subjectId`
- optional denial reason, for example:
  - `missing_acl_entry`
  - `anonymous_denied`
  - `insufficient_role`

That is more useful than a plain message string and makes tests less brittle.

### 7. Be deliberate about where ACL is invoked

Current call sites often reach for `aggregate.acl()` and then call the checker.

That is acceptable, but if some modules end up with repetitive authorization checks, a small domain-facing helper may improve readability:

```kotlin
context(_: Raise<AuthorizationError>)
suspend fun Appointment.ensureReadableBy(subject: AccessSubject)
```

This should be used sparingly. The goal is to improve readability, not to blur security and domain responsibilities.

## Kotlin DSL opportunities

There is a real opportunity to make the ACL system more ergonomic with a small Kotlin DSL.

The DSL should improve:

- ACL construction
- permission-check call sites
- optional resource integration

The DSL should not try to become a full policy language.

## Recommended DSL scope

The most useful DSL layers are:

1. ACL builder DSL
2. typed permission-check extensions
3. optional resource-level integration

The least urgent DSL layer is a policy-definition DSL.

## 1. ACL builder DSL

This is the clearest near-term win.

Instead of:

```kotlin
Acl(
  resourceId = appointmentId,
  userRoleMap = mapOf(
    ownerId to AclRole.MANAGER,
    assistantId to AclRole.READER
  )
)
```

the API could become:

```kotlin
val acl = acl(appointmentId) {
  manager(ownerId)
  reader(assistantId)
}
```

Or, if anonymous/default access matters:

```kotlin
val acl = acl(appointmentId) {
  manager(ownerId)
  reader(assistantId)
  anonymousReader()
}
```

Another possible form:

```kotlin
val acl = acl(appointmentId) {
  user(ownerId) can MANAGE
  user(assistantId) can READ
}
```

Recommendation:

- start with the explicit builder form like `manager(userId)` / `reader(userId)`
- avoid a more clever infix DSL unless it adds real value

The explicit builder is easier to read, easier to maintain, and better for public API stability.

## 2. Permission-check extension DSL

This is the second best convenience layer.

Instead of:

```kotlin
aclChecker.ensurePermission(subject, appointment.acl(), Permission.READ)
```

prefer:

```kotlin
subject.canRead(appointment.acl())
subject.canWrite(appointment.acl())
subject.canManage(appointment.acl())
```

Or in a `Raise` context:

```kotlin
context(_: Raise<AuthorizationError>)
subject.ensureCanRead(appointment.acl())
```

This gives cleaner call sites while staying fully typed.

Recommendation:

- prefer named extension functions like `ensureCanRead(...)`
- do not overuse infix operators for permission checks

Good:

```kotlin
subject.ensureCanRead(acl)
subject.ensureCanWrite(acl)
```

Less desirable:

```kotlin
subject canRead acl
subject canWrite acl
```

The infix version is attractive, but the explicit version is usually easier to debug and evolve.

## 3. Resource-level DSL

If secured aggregates expose ACL consistently, resource-level helpers can improve readability further.

Example shape:

```kotlin
interface SecuredResource {
  val acl: Acl
}
```

Then:

```kotlin
context(_: Raise<AuthorizationError>)
subject.ensureCanRead(appointment)
subject.ensureCanWrite(userAccount)
```

This is useful when:

- many aggregates expose ACL
- call sites are repetitive
- the abstraction remains obvious

Recommendation:

- treat this as optional
- only introduce it if it reduces noise without obscuring what is happening

## 4. Policy DSL

If the ACL system becomes reusable outside this project, a small policy-definition DSL may eventually help.

Example:

```kotlin
val policy = policy {
  globalRole("ROLE_SYSTEM") grantsAll()
  globalRole("ROLE_ADMIN") grantsAll()
}
```

This is mainly useful if:

- the admin/system bypass becomes configurable
- the system is extracted into an OSS library

Recommendation:

- do not start here
- this is a later-stage enhancement, not a first implementation step

## DSL boundaries

The DSL should remain Kotlin-first and strongly typed.

Avoid:

- string-based policy expressions
- “sentence-like” syntax that hides types
- DSL magic that makes debugging difficult

Good DSL design for this project should:

- compile down to simple data structures
- remain discoverable in IDE autocomplete
- stay explicit about resource, subject, and permission
- compose cleanly with Arrow `Raise`

## Proposed first-step DSL

If we add DSL support, this is the most pragmatic first version:

### ACL creation

```kotlin
val acl = acl(resourceId) {
  manager(ownerId)
  reader(assistantId)
}
```

### Authorization checks

```kotlin
context(_: Raise<AuthorizationError>)
subject.ensureCanRead(acl)
subject.ensureCanWrite(acl)
subject.ensureCanManage(acl)
```

### Optional resource helpers

```kotlin
context(_: Raise<AuthorizationError>)
subject.ensureCanRead(resource)
```

This gives a meaningful ergonomics improvement without turning the library into a language.

## DSL and OSS extraction

If this becomes an OSS library, the DSL should be part of the value proposition:

- simple ACL construction
- typed permission checks
- optional Spring integration

That is a stronger and more memorable story than just “yet another ACL library.”

The best sequencing is:

1. build the typed core first
2. add the minimal DSL once the core stabilizes
3. only then consider extraction

That way the DSL reflects a stable model rather than driving premature abstraction.

## Boundary policy

### Adapters

Adapters should not rely on thrown authorization exceptions.

Instead:

- call `ensurePermission(...)`
- translate `AuthorizationError` to `WorkflowError` at the adapter boundary

### Workflows

Workflows that already operate in `Raise<WorkflowError>` should not see raw authorization exceptions.

They should only see typed workflow-level authorization failures.

### Controllers

Controllers should continue dealing with `Either<WorkflowError, ...>` and transport mapping.

No controller should need to know about `AuthorizationError` directly.

## Collection-read policy

One important design decision should be made explicitly:

### Option A: fail the whole query on unreadable rows

Use this when the query is conceptually “fetch resources I am allowed to see” and unreadable rows indicate a data or policy inconsistency.

This is the safer default for this codebase.

### Option B: filter to visible rows only

Use this only when the endpoint contract explicitly means “show me what is visible”.

If this behavior is chosen, it should use `hasPermission(...)` directly, not exception-driven filtering.

## Spring integration

The ACL redesign does not require a framework replacement.

If desired later, Spring Security’s `AuthorizationManager` or method-security checks can be layered on top of the typed ACL system, but the core ACL logic should remain framework-neutral.

Recommended direction:

- core ACL stays typed and project-local
- Spring Security remains the HTTP/authentication boundary
- transport mapping to `401` remains at the workflow/controller boundary

## OSS potential

This design may be a good candidate for a small open source library, but only after it is generalized.

Right now the ACL implementation is still application-shaped:

- it depends on project-specific concepts like [`ExecutionContext`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/ExecutionContext.kt)
- authorization denial is still coupled to application/web error types in some paths
- the current code lives inside the repo’s common module rather than behind a clean, reusable API

The promising part is the design angle:

- Kotlin-first
- Arrow-friendly
- simple ACL model
- typed authorization outcomes
- optional Spring integration rather than mandatory Spring coupling

That is specific enough to be interesting as a small OSS project.

## Extraction target

If this is eventually extracted, the reusable design should be split into small, composable artifacts so consumers are not forced into Arrow or Spring.

The target split is:

### `acl-core`

Contains only generic authorization concepts:

- `Acl`
- `AclRole`
- `Permission`
- `AccessSubject`
- `AuthorizationError`
- `PermissionDenied`
- `AclBuilder`
- `acl(...)`
- pure `hasPermission(...)`
- optional `SecuredResource`

This module should depend only on Kotlin.

### `acl-arrow`

Optional Arrow integration for consumers who want typed authorization failures with `Raise` or `Either`.

Contains:

- `ensurePermission(...)`
- `ensureCanRead/Write/Manage(...)`
- `AccessSubject` Arrow extension DSL
- any small `Either` convenience helpers, if they prove worthwhile

This module should depend on `acl-core` and Arrow, but not on Spring.

### `acl-spring-security`

Optional Spring Security integration for consumers who want current-subject lookup but do not want Arrow.

Contains:

- Spring Security `Authentication` -> `AccessSubject`
- current-subject resolution
- optional bean wiring
- optional boolean/current-subject convenience helpers

This module should depend on `acl-core` and Spring Security, but not on Arrow.

### `acl-spring-security-arrow`

Optional bridge for consumers who want both Spring Security and Arrow.

Contains:

- current-subject `Raise` helpers such as:
  - `ensureCanReadCurrent(...)`
  - `ensureCanWriteCurrent(...)`
  - `ensureCanManageCurrent(...)`
- any other convenience API that composes Spring subject lookup with Arrow error handling

This module should depend on `acl-core`, `acl-arrow`, and `acl-spring-security`.

### Example / demo module

A small sample app showing:

- pure Kotlin usage
- Arrow usage
- Spring Security usage
- combined Spring + Arrow usage

## Public API goals

Before extraction, the public API should be intentionally small and generic.

Target surface:

```kotlin
data class AccessSubject(
  val userId: String,
  val roles: Set<String>
)

enum class Permission { READ, WRITE, MANAGE }

data class Acl(
  val resourceId: String,
  val userRoleMap: Map<String, AclRole>
)

sealed interface AuthorizationError

fun hasPermission(subject: AccessSubject, acl: Acl, permission: Permission): Boolean
```

Nice-to-have helpers:

- Arrow helpers such as `ensureCanRead(...)`
- Arrow helpers such as `ensureCanWrite(...)`
- Arrow helpers such as `ensureCanManage(...)`
- expressive ACL factory methods

Anything tied to:

- `ExecutionContext`
- `WorkflowError`
- `UnauthorizedWorkflowError`
- controller/web response behavior

should stay out of the extracted core.

## Adoption strategy

The right order is not “build OSS first.”

The better path is:

1. redesign and stabilize the ACL model inside this repo
2. create in-repo Maven modules that mirror the intended library artifacts
3. migrate this repo to consume those modules as if they were external dependencies
4. extract only once the API feels generic and small

This avoids freezing the wrong abstraction too early.

## Comprehensive plan

The plan below covers both the in-repo redesign and the potential future extraction.

### Phase 0: Design hardening

Goals:

- finalize the typed ACL direction in documentation
- agree on the subject model
- agree on collection-read policy
- agree that authorization denial is expected data, not an exception

Deliverables:

- this design document
- a clear decision on `fail query` vs `filter visible results`

### Phase 1: Introduce typed ACL in-repo

Goals:

- add `AuthorizationError`
- add `AccessSubject`
- add `ensurePermission(...)`
- keep legacy `checkPermission(...)` temporarily for compatibility

Deliverables:

- typed authorization core inside the current repo
- initial tests for `hasPermission(...)` and `ensurePermission(...)`

### Phase 2: Replace implicit subject handling

Goals:

- remove the current `List<String>` convention
- derive a typed `AccessSubject` from `ExecutionContext`
- keep explicit subject-based core APIs for testability

Deliverables:

- a project-local adapter from Spring auth state to `AccessSubject`
- no remaining positional subject conventions

### Phase 3: Migrate adapters and workflows

Goals:

- migrate `user` and `booking` adapters first
- replace exception-based ACL denial with typed mapping
- add `ensureCanRead/Write/Manage(...)` helpers where they improve readability

Deliverables:

- adapters no longer depend on thrown ACL exceptions
- boundary translation to `WorkflowError` occurs exactly once per boundary

### Phase 4: Clean up legacy exception flow

Goals:

- delete `UnauthorizedAccessException` if no longer needed
- delete `checkPermission(...)`
- remove exception-based translation code used only for ACL denial

Deliverables:

- no expected authorization denial represented as thrown exceptions

### Phase 5: Generalize the API

Goals:

- identify project-specific types still leaking into ACL code
- move or isolate them
- reduce the ACL surface to a minimal generic API
- separate pure core APIs from Arrow-only APIs

Deliverables:

- framework-neutral ACL core inside the repo
- Arrow helpers isolated behind an Arrow-specific package
- clear separation between:
  - reusable authorization logic
  - project-specific workflow/web integration

### Phase 6: Create in-repo Maven modules

Goals:

- prove the separation before publishing anything
- create real top-level Maven modules such as:
  - `acl-core`
  - `acl-arrow`
  - `acl-spring-security`
  - `acl-spring-security-arrow`
- make the existing application modules depend on them through the parent reactor

Deliverables:

- buildable local modular split inside this repo
- green tests across modules
- no dependency from the core module on application code

### Phase 7: Consume the new modules in-place

Goals:

- migrate `common`, `user`, `booking`, `payment`, and `server` to consume the new ACL modules
- remove any leftover duplicate ACL code from `common`
- verify the modules behave like publishable dependencies even while still in this repo

Deliverables:

- app code consumes `acl-core`, `acl-arrow`, `acl-spring-security`, and `acl-spring-security-arrow`
- no remaining hidden dependency on old package-local ACL implementation
- full reactor build stays green

### Phase 8: OSS readiness review

Goals:

- decide whether the API is stable enough to publish
- decide whether the value proposition is clear enough

Checklist:

- API is small and generic
- docs explain why this exists vs Spring ACL / OpenFGA / Casbin
- examples cover both pure Kotlin and Spring integration
- naming is generic and not project-branded
- package structure is reusable
- license and maintenance expectations are clear

Deliverables:

- go/no-go decision for open sourcing

### Phase 9: Publish

Goals:

- create separate repository
- move the extracted modules
- add README, docs, examples, and release workflow

Deliverables:

- initial OSS repository
- version `0.x`
- example integration back into this repo as proof of real use

## Migration plan

### Phase 1

- add `AuthorizationError`
- add `ensurePermission(...)` using `Raise`
- add `AccessSubject`
- keep `checkPermission(...)` temporarily

### Phase 2

- replace the current `List<String>` subject convention
- add `ensureCanRead/Write/Manage(...)`
- migrate `user` and `booking` adapters from `checkPermission(...)` to `ensurePermission(...)`
- replace exception translation with typed mapping

### Phase 3

- add structured denial metadata
- update collection-read sites to use either:
  - explicit failure on unreadable rows, or
  - explicit boolean filtering with `hasPermission(...)`

### Phase 4

- introduce clearer ACL factories where they reduce repeated wiring
- delete `UnauthorizedAccessException`
- delete `checkPermission(...)`
- simplify any leftover exception-to-workflow translation related only to ACL denial

### Phase 5

- move `Raise`-based helpers out of the core package
- keep `acl-core` boolean/value oriented
- introduce an in-repo `acl-arrow` shape
- update tests so core tests do not depend on Arrow helpers

### Phase 6

- create new top-level Maven modules in this repo:
  - `acl-core`
  - `acl-arrow`
  - `acl-spring-security`
  - `acl-spring-security-arrow`
- wire them into the parent `pom.xml`
- move code into those modules without changing consumers yet
- add module-local tests and keep them green

### Phase 7

- split Spring current-subject lookup from Spring + Arrow convenience
- migrate existing consumers to the new module dependencies
- delete the old in-place ACL implementation once the new module paths are live
- run full-reactor verification

## Recommendation

Do not adopt a new external ACL library right now.

Instead:

1. keep the current ACL model
2. make authorization denial typed
3. replace the implicit subject convention with a typed subject
4. use `Raise` internally
5. translate to `WorkflowError` only at adapter/workflow boundaries

This preserves the current architecture while bringing authorization into line with the project’s Arrow-based style.

If the design continues to hold up after the in-repo migration, it can then be extracted into a small Kotlin/Arrow-focused OSS library with optional Spring integration.

## Implementation plan

The steps below are designed to be independently verifiable. Each step should be committable on its own and validated with focused tests before moving to the next step.

### Step 1: Add the typed authorization core

Changes:

- add `AuthorizationError`
- add `PermissionDenied`
- add `AccessSubject`
- keep the current `Acl`, `AclRole`, and `Permission`
- keep legacy `checkPermission(...)` temporarily

Tests:

- add unit tests for `AccessSubject`-based permission evaluation
- add unit tests for:
  - owner/manager access
  - reader access
  - anonymous access
  - admin/system bypass
  - denied access

Verification:

- new focused ACL unit tests pass
- existing project test suite stays green

Commit boundary:

- no adapter or workflow call sites changed yet

### Step 2: Introduce `ensurePermission(...)` with `Raise`

Changes:

- add `context(_: Raise<AuthorizationError>) ensurePermission(...)`
- implement it in terms of the existing boolean policy
- do not remove `checkPermission(...)` yet

Tests:

- add tests that `ensurePermission(...)` succeeds for allowed access
- add tests that `ensurePermission(...)` raises `PermissionDenied` for denied access

Verification:

- focused ACL tests pass
- no existing application behavior changes

Commit boundary:

- new API exists, but production call sites are still on the old path

### Step 3: Replace the implicit subject convention

Changes:

- stop using the current `List<String>` convention internally
- add a project-local adapter from [`ExecutionContext`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/ExecutionContext.kt) to `AccessSubject`
- update `hasPermission(...)` and `ensurePermission(...)` to use `AccessSubject`

Tests:

- add tests for `ExecutionContext -> AccessSubject`
- update ACL tests to use `AccessSubject`

Verification:

- ACL unit tests pass
- no behavior regression in security integration tests

Commit boundary:

- still no adapter migration yet

### Step 4: Add convenience permission helpers

Changes:

- add:
  - `ensureCanRead(...)`
  - `ensureCanWrite(...)`
  - `ensureCanManage(...)`
- optionally add overloads for `Acl` and `SecuredResource`

Tests:

- add unit tests confirming each helper delegates to the correct permission

Verification:

- helper tests pass
- no existing production call sites need to change yet

Commit boundary:

- convenience API only

### Step 5: Migrate one adapter end-to-end

Recommended first target:

- [`UserPersistenceAdapter.kt`](/Users/erikdreyer/dev/erik/modulith/user/src/main/kotlin/io/liquidsoftware/base/user/adapter/out/persistence/UserPersistenceAdapter.kt)

Changes:

- replace exception-based ACL checks with typed ACL checks
- map `AuthorizationError` to `UnauthorizedWorkflowError` once at the adapter boundary

Tests:

- update or add adapter contract tests for:
  - allowed read
  - denied read -> `Left(UnauthorizedWorkflowError)`
  - save denied -> `Left(UnauthorizedWorkflowError)`

Verification:

- focused user adapter tests pass
- existing `server` integration tests covering user access still pass

Commit boundary:

- only one module migrated

### Step 6: Migrate booking adapter end-to-end

Changes:

- replace exception-based ACL checks in [`BookingPersistenceAdapter.kt`](/Users/erikdreyer/dev/erik/modulith/booking/src/main/kotlin/io/liquidsoftware/base/booking/adapter/out/persistence/BookingPersistenceAdapter.kt)
- make collection-read policy explicit

Tests:

- update or add booking adapter tests for:
  - denied single read -> `Left`
  - denied collection read -> behavior matches chosen policy
  - repository failure remains distinct from authorization denial

Verification:

- focused booking adapter tests pass
- booking integration tests remain green

Commit boundary:

- collection-read semantics are now explicit

### Step 7: Remove exception-based ACL denial

Changes:

- delete `checkPermission(...)`
- delete `UnauthorizedAccessException` if no longer used
- remove exception-to-workflow translation code that existed only for ACL denial

Tests:

- run all ACL unit tests
- run adapter contract tests
- run auth-related integration tests

Verification:

- no remaining production use of `checkPermission(...)`
- project-wide test suite remains green

Commit boundary:

- typed ACL is now the only ACL path

### Step 8: Add the minimal ACL builder DSL

Changes:

- add:
  - `acl(resourceId) { ... }`
  - builder helpers like `manager(userId)` / `reader(userId)`

Tests:

- add unit tests for DSL-produced ACL values
- confirm DSL output matches hand-built ACL values

Verification:

- DSL unit tests pass
- no behavior change in production code unless call sites are migrated intentionally

Commit boundary:

- DSL only, no core logic changes

### Step 9: Add permission-check extension DSL

Changes:

- add extension helpers such as:
  - `subject.ensureCanRead(acl)`
  - `subject.ensureCanWrite(acl)`
  - `subject.ensureCanManage(acl)`

Tests:

- add tests confirming each extension maps to the right underlying permission

Verification:

- focused DSL tests pass
- optional incremental migration of call sites compiles cleanly

Commit boundary:

- ergonomics-only improvement

### Step 10: Split Arrow APIs from the core package

Changes:

- move `Raise`-based ACL helpers out of the core package
- keep `acl-core` behavior boolean/value oriented
- introduce an in-repo package shape that mirrors the future `acl-arrow` artifact

Tests:

- keep core ACL tests focused on pure boolean/value behavior
- add or move Arrow-specific tests so they validate only the Arrow layer

Verification:

- the pure ACL layer can be tested without Arrow
- Arrow helpers compile and pass their own focused tests

Commit boundary:

- package/API split only, no Maven module creation yet

### Step 11: Split Spring subject resolution from Spring + Arrow convenience

Changes:

- keep Spring current-subject lookup in a Spring-only adapter
- move current-subject `Raise` ergonomics into a separate in-repo bridge package
- make the code structure mirror:
  - `acl-spring-security`
  - `acl-spring-security-arrow`

Tests:

- add or update focused tests for Spring subject resolution
- keep Arrow-specific Spring tests separate from pure Spring adapter tests

Verification:

- Spring-only adapter compiles without Arrow concerns
- Spring+Arrow bridge compiles and passes focused tests

Commit boundary:

- package/API split only, still inside existing Maven modules

### Step 12: Create new top-level Maven modules in this repo

Changes:

- add new parent-module entries for:
  - `acl-core`
  - `acl-arrow`
  - `acl-spring-security`
  - `acl-spring-security-arrow`
- create minimal `pom.xml` files for each
- establish dependency direction:
  - `acl-core`
  - `acl-arrow` -> `acl-core`
  - `acl-spring-security` -> `acl-core`
  - `acl-spring-security-arrow` -> `acl-core`, `acl-arrow`, `acl-spring-security`

Tests:

- each new module should have at least one smoke test or migrated unit test

Verification:

- `mvn test` still resolves the full reactor
- no cyclic dependency exists among the ACL modules

Commit boundary:

- module scaffolding exists, but existing consumers may still use the old package locations

### Step 13: Move ACL code into the new Maven modules

Changes:

- move pure ACL code into `acl-core`
- move Arrow helpers into `acl-arrow`
- move Spring subject resolution into `acl-spring-security`
- move Spring+Arrow bridge code into `acl-spring-security-arrow`
- keep package names stable where possible to minimize churn

Tests:

- migrate the existing ACL tests into the appropriate modules
- keep focused module-local tests green as code moves

Verification:

- each ACL module builds and tests independently
- no module depends on application-specific workflow/web code

Commit boundary:

- code physically moved, but application consumers may still need final dependency rewiring

### Step 14: Migrate this repo to consume the new ACL modules

Changes:

- update `common`, `user`, `booking`, `payment`, and `server` to depend on the new ACL modules
- remove duplicate ACL code from the old locations
- keep app-local mapping such as `AuthorizationError -> WorkflowError` in this repo

Tests:

- run focused tests for affected modules as they migrate
- run end-to-end integration tests for the auth-sensitive flows

Verification:

- full reactor `mvn clean test` passes
- application code now depends on the new Maven modules exactly as a future external consumer would

Commit boundary:

- in-repo modular extraction is complete and ready for OSS readiness review

### Step 15: Evaluate extraction readiness

Changes:

- identify remaining project-specific dependencies in the new ACL modules
- confirm only app-local adapters remain in this repo

Tests:

- verify the ACL modules can be tested without `ExecutionContext`, Spring app wiring, or workflow types leaking across module boundaries

Verification:

- clear module/API boundary exists
- the in-repo modules are ready to be copied into a separate repository with minimal change

Commit boundary:

- still inside this repo, but extraction-ready

## Extraction review

The current ACL design is now much closer to something reusable, but it is not yet a clean OSS library boundary.

### What is generic enough to extract

These pieces are strong candidates for the extracted ACL family:

- `Acl`
- `AclRole`
- `Permission`
- `AccessSubject`
- `AuthorizationError`
- `PermissionDenied`
- `AclBuilder`
- `acl(resourceId) { ... }`
- subject-based permission checks
- subject-based `Raise` helpers
- ACL-level extension DSL

This is the reusable heart of the design:

- typed authorization outcomes
- simple owner/role-based ACLs
- Kotlin-first builder DSL
- Arrow-friendly `Raise` APIs

### What should stay application-specific

These pieces are still tied to this repo and should not move into a generic ACL module as-is:

- [`ExecutionContext.kt`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/ExecutionContext.kt)
- Spring `SecurityContextHolder` integration
- current Spring bean wiring around subject resolution and authorization helpers
- [`AuthorizationError.toWorkflowError()`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/ext/UsefulExtensions.kt)
- `WorkflowError` / `UnauthorizedWorkflowError` mapping
- `workflowBoundary(...)`
- module-specific domain types implementing `SecuredResource`

Those are adapters around the ACL core, not part of the core itself.

### Current design pressure points

If this were extracted today, three parts would still feel awkward:

1. `AclChecker` still mixes pure boolean evaluation with Arrow `Raise` helpers.
   For extraction, that should become:
   - `acl-core`: boolean/value API
   - `acl-arrow`: `Raise` / `Either` helpers

2. The current Spring adapter still combines current-subject lookup with Arrow-flavored authorization ergonomics.
   For extraction, that should become:
   - `acl-spring-security`: subject resolution
   - `acl-spring-security-arrow`: Spring + Arrow bridge

3. `SecuredResource` is useful ergonomically, but it is the most opinionated part of the new DSL.
   A reusable library can support it, but it should probably be optional rather than central.

### Recommended extracted shape

If this becomes a separate library, I would aim for a structure like this:

#### `acl-core`

Contains only pure/domain-level authorization concepts:

- `Acl`
- `AclRole`
- `Permission`
- `AccessSubject`
- `AuthorizationError`
- `PermissionDenied`
- `AclBuilder`
- `acl(...)`
- a pure evaluator / authorizer
- optional `SecuredResource`

Critically, this layer should not know about:

- Spring
- Arrow
- `SecurityContextHolder`
- `WorkflowError`
- Mongo
- HTTP

#### `acl-arrow`

This layer should exist.

The goal is to avoid forcing Spring users to adopt Arrow and avoid forcing Arrow users to adopt Spring.

This module should contain:

- `Raise`-based `ensurePermission(...)`
- `ensureCanRead/Write/Manage(...)`
- `AccessSubject` Arrow extension DSL
- optional `Either` helpers if they prove useful

Dependency direction:

- `acl-core`: pure boolean/value API
- `acl-arrow`: `Raise` / `Either` helpers
- no Spring dependencies here

#### `acl-spring-security`

Framework adapter only:

- resolve current `AccessSubject` from Spring Security
- provide current-subject lookup and optionally boolean convenience wrappers
- optional bean wiring

This is where the current `ExecutionContext` integration logic belongs conceptually.

#### `acl-spring-security-arrow`

This bridge should exist if the library is extracted.

It is the right home for:

- current-subject `Raise` helpers
- ergonomic APIs that combine Spring subject lookup with Arrow error handling

Without this bridge, one of the other modules would need to depend on both Spring and Arrow, which would defeat the point of the split.

#### app-local adapter

This should stay out of the OSS library unless you later extract your workflow model too.

Specifically:

- `AuthorizationError -> WorkflowError`
- Spring MVC / HTTP exception mapping for auth failures

### Recommendation for this repo

Inside this repo, I would keep:

- the pure ACL model in `common.security.acl`
- Spring subject resolution in [`ExecutionContext.kt`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/security/ExecutionContext.kt)
- workflow mapping in [`UsefulExtensions.kt`](/Users/erikdreyer/dev/erik/modulith/common/src/main/kotlin/io/liquidsoftware/common/ext/UsefulExtensions.kt)

But if you want extraction readiness, the next refactor should be:

1. keep `AclChecker` pure and Arrow-free in the core
2. move all `Raise`-based helpers into an Arrow-specific package
3. keep a thin Spring adapter that only resolves the current `AccessSubject`
4. add a Spring+Arrow bridge package for current-subject `Raise` ergonomics

### What I would export today

If forced to define the public API now, I would export only this:

- `Acl`
- `AclRole`
- `Permission`
- `AccessSubject`
- `AuthorizationError`
- `PermissionDenied`
- `acl(resourceId) { ... }`
- `SecuredResource`
- `hasPermission(subject, acl, permission)`
- `ensureCanRead/Write/Manage(subject, acl)`
- `subject.ensureCanRead/Write/Manage(acl)`

I would not export these yet:

- Spring bean wiring
- current-subject lookup
- app-specific role names or defaults if they cannot be configured
- `WorkflowError` mapping
- HTTP status assumptions

### Bottom line

The ACL design is now good enough to extract conceptually, but not yet good enough to publish unchanged.

The key rule is:

- extract the pure authorization model into `acl-core`
- keep Arrow as an optional adapter in `acl-arrow`
- keep Spring subject resolution in `acl-spring-security`
- keep Spring + Arrow ergonomics in `acl-spring-security-arrow`
- keep workflow/web mapping as an app-local adapter

If you do only one more preparation pass before extraction, it should be separating `AclChecker` into:

- a pure Arrow-free authorizer that accepts `AccessSubject`
- an Arrow helper layer
- a Spring-aware current-subject adapter
- a Spring+Arrow bridge layer

## Recommended test map

To keep the migration safe, the test suite should evolve along these lines:

### Core ACL unit tests

Should cover:

- direct permission evaluation
- global role bypass
- anonymous behavior
- typed denial behavior
- DSL builder correctness

### Adapter contract tests

Should cover:

- single-entity read denial
- collection-read denial policy
- write denial
- distinction between authorization failures and infrastructure failures

### Integration tests

Should cover:

- unauthorized access mapped to `401`
- authorized access still works
- authentication still loads users correctly

## Recommended commit strategy

Keep the commits narrow and behavior-focused:

1. typed core
2. `Raise` enforcement API
3. subject model migration
4. user adapter migration
5. booking adapter migration
6. legacy exception removal
7. ACL builder DSL
8. permission-check DSL
9. extraction readiness cleanup

That order keeps every step reviewable and testable on its own.
