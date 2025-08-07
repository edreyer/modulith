# Project Coding Guidelines

## Overview
This project is a Kotlin-based Spring Boot application that follows a hexagonal architecture combined with Domain-Driven Design (DDD) principles. It's designed as a "Modulith" - essentially a microservice architecture deployed as a monolith, providing the benefits of microservices without the operational complexity.

The codebase emphasizes functional programming, type safety, and clean separation of concerns. It uses Kotlin's advanced features like coroutines, context receivers, and algebraic data types to create a robust and maintainable codebase.

## Code Style

### Comments and Documentation
- Use KDoc comments for public APIs and important internal components
- Use single-line comments for implementation details
- Document non-obvious behavior and design decisions
- Include examples in documentation where helpful

```kotlin
/**
 * Delegate UserData class to cut down on copy-pasta in each ADT instance of User
 * see: https://proandroiddev.com/simpler-kotlin-class-hierarchies-using-class-delegation-35464106fed5
 */
internal interface UserFields {
  val id: UserId
  val msisdn: Msisdn
  val email: EmailAddress
  val encryptedPassword: NonEmptyString
}
```

## Naming Conventions

### General Naming Rules
- Use descriptive, meaningful names
- Avoid abbreviations except for common ones (e.g., ID, DTO)
- Use domain terminology consistently

### Class and Interface Naming
- Use PascalCase for class and interface names
- Use nouns for entity classes (User, Appointment)
- Use adjective+noun for state-based classes (ActiveUser, UnregisteredUser)
- Use verb+noun+suffix for workflow classes (RegisterUserWorkflow, CancelAppointmentWorkflow)
- Use noun+suffix for interfaces (FindUserPort, UserEventPort)

```kotlin
// Class naming examples
sealed class User
data class ActiveUser(private val data: UserData) : User(), UserFields by data
internal class RegisterUserWorkflow(/*...*/) : BaseSafeWorkflow<RegisterUserCommand, UserRegisteredEvent>()
internal interface FindUserPort
```

### Function and Method Naming
- Use camelCase for function and method names
- Use verbs for functions that perform actions
- Use "get" prefix for accessor methods
- Use "is/has/can" prefix for boolean methods
- Use "to" prefix for conversion methods

```kotlin
// Function naming examples
fun findUserById(userId: String): User?
fun isValid(value: String): Boolean
fun toUserDto(): UserDto
```

### Variable Naming
- Use camelCase for variable names
- Use descriptive names that indicate purpose
- Use single-letter names only for local variables with limited scope (e.g., loop counters)

```kotlin
// Variable naming examples
val userRepository: UserRepository
val passwordEncoder: PasswordEncoder
for (i in items.indices) { /*...*/ }
```

### File Naming
- Use PascalCase for file names containing a single primary class
- Match the file name to the primary class name
- Use plural for files containing multiple related classes

```kotlin
// File naming examples
User.kt         // Contains User class and related classes
Workflows.kt    // Contains multiple workflow classes
SimpleTypes.kt  // Contains multiple simple type classes
```

### Package Naming
- Use lowercase with dots as separators
- Follow the hexagonal architecture structure:
  - domain: Core domain models
  - application: Application services, ports, and workflows
  - adapter.in: Input adapters (web controllers, etc.)
  - adapter.out: Output adapters (persistence, external services, etc.)

```kotlin
// Package naming examples
io.liquidsoftware.base.user.domain
io.liquidsoftware.base.user.application.workflows
io.liquidsoftware.base.user.adapter.in.web
io.liquidsoftware.base.user.adapter.out.persistence
```

## Architecture Patterns

### Hexagonal Architecture
The project follows a hexagonal architecture (also known as ports and adapters) with the following components:

- **Domain Layer**: Contains the core domain models and business logic
  - Located in the `domain` package
  - Has no dependencies on other layers
  - Uses algebraic data types (sealed classes) to model domain entities

- **Application Layer**: Contains the application services, ports, and workflows
  - Located in the `application` package
  - Depends only on the domain layer
  - Defines ports (interfaces) for communication with adapters
  - Implements workflows that orchestrate domain logic

- **Adapter Layer**: Contains the adapters that connect to external systems
  - Located in the `adapter` package
  - Divided into `in` (driving) and `out` (driven) adapters
  - Implements the ports defined by the application layer

```kotlin
// Domain model example
internal sealed class User: UserFields {
  // Domain logic
}

// Application port example
internal interface FindUserPort {
  suspend fun findUserById(userId: String): User?
}

// Adapter implementation example
internal class UserPersistenceAdapter(
  private val userRepository: UserRepository,
  private val ac: AclChecker
) : FindUserPort, UserEventPort {
  // Implementation
}
```

### CQRS Pattern
The project uses Command Query Responsibility Segregation (CQRS) to separate read and write operations:

- **Commands**: Represent write operations that change state
- **Queries**: Represent read operations that retrieve data
- **Events**: Represent the results of operations

```kotlin
// Command example
data class RegisterUserCommand(
  val msisdn: String,
  val email: String,
  val password: String,
  val role: String
) : Command

// Event example
data class UserRegisteredEvent(
  val userDto: UserDto,
  val password: String
) : Event()
```

### Workflow Pattern
The project uses a workflow pattern to implement use cases:

- Each use case is implemented as a separate workflow class
- Workflows extend BaseSafeWorkflow with specific input and output types
- Workflows orchestrate domain logic but don't implement it directly
- Workflows handle errors using functional error handling

```kotlin
internal class RegisterUserWorkflow(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val userRegisteredPort: UserEventPort
) : BaseSafeWorkflow<RegisterUserCommand, UserRegisteredEvent>() {
  // Implementation
}
```

### Dependency Injection
The project uses Spring's dependency injection:

- Use constructor injection for dependencies
- Mark components with appropriate Spring annotations (@Component, @Service, etc.)
- Keep dependencies minimal and focused

```kotlin
@Component
internal class RegisterUserWorkflow(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val userRegisteredPort: UserEventPort
) : BaseSafeWorkflow<RegisterUserCommand, UserRegisteredEvent>() {
  // Implementation
}
```

### Error Handling
The project uses functional error handling with Arrow:

- Use Arrow's Either type for error handling
- Define specific error types for different error cases
- Use context receivers for propagating errors
- Avoid throwing exceptions in business logic

```kotlin
context(Raise<WorkflowError>)
override suspend fun execute(request: RegisterUserCommand) : UserRegisteredEvent {
  // Implementation that uses raise() for errors
}
```

## Best Practices

### Domain Modeling
- Use algebraic data types (sealed classes) to model domain entities
- Make domain objects immutable
- Use factory methods for object creation with validation
- Ensure domain objects enforce their internal consistency
- Use value objects for primitive types that have business meaning

```kotlin
// Value object example
class NonEmptyString private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    context(Raise<ValidationErrors>)
    fun of(value: String): NonEmptyString = ensure {
      validate(NonEmptyString(value)) {
        validate(NonEmptyString::value).isNotEmpty()
      }
    }.bind()
  }
}

// Domain entity example
internal data class ActiveUser(
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    context(Raise<ValidationErrors>)
    fun of(id: String, msisdn: String, email: String, encryptedPassword: String): ActiveUser =
      ActiveUser(newUserData(id, msisdn, email, encryptedPassword))
  }
}
```

### Service Design
- Keep service methods small and focused on a single responsibility
- Compose complex operations from smaller service methods
- Avoid side effects where possible
- Use descriptive method names that indicate what the method does

```kotlin
// Service method example
private suspend fun validateNewUser(cmd: RegisterUserCommand) : UnregisteredUser =
  findUserPort.findUserByEmail(cmd.email)
    ?.let { raise(UserExistsError("User ${cmd.msisdn} exists")) }
    ?: effect { UnregisteredUser.of(
      msisdn = cmd.msisdn,
      email = cmd.email,
      encryptedPassword = passwordEncoder.encode(cmd.password),
      role = Role.valueOf(cmd.role)
    ) }.fold(
      { raise(WorkflowValidationError(it)) },
      { it }
    )
```

### Asynchronous Programming
- Use suspend functions for asynchronous operations
- Use coroutines for concurrency
- Use withContextIO for IO-bound operations
- Avoid blocking operations

```kotlin
// Asynchronous method example
override suspend fun findUserById(userId: String): User? = withContextIO {
  kotlin.runCatching {
    userRepository.findByUserId(userId)
      ?.toUser()
      ?.also { ac.checkPermission(it.acl(), Permission.READ)}
  }.getOrNull()
}
```

### Security
- Use access control checks consistently
- Run operations with appropriate permissions
- Validate input data thoroughly
- Use secure password handling

```kotlin
// Security example
val result = runAsSuperUser {
  val user = validateNewUser(request)
  userRegisteredPort.handle(
    UserRegisteredEvent(user.toUserDto(), user.encryptedPassword.value)
  )
}
```

## Testing Guidelines

### Architecture Testing
- Use ArchUnit to enforce architectural constraints
- Test that dependencies flow in the correct direction
- Test that packages contain only appropriate classes

```kotlin
// Architecture test example
@Test
fun testDomainPackageDependencies() {
  noClasses()
    .that()
    .resideInAPackage("io.liquidsoftware.base.$module.domain..")
    .should()
    .dependOnClassesThat()
    .resideInAnyPackage("io.liquidsoftware.base.$module.application..")
    .check(
      ClassFileImporter()
        .importPackages("io.liquidsoftware.base.$module..")
    )
}
```

### Unit Testing
- Test domain logic thoroughly
- Use mocks for dependencies
- Test happy paths and error cases
- Use descriptive test names that indicate what's being tested

### Integration Testing
- Test workflows with real or simulated dependencies
- Test adapters with real external systems where possible
- Use test containers for database tests

## Documentation Standards

### Code Documentation
- Document public APIs with KDoc comments
- Document complex algorithms and non-obvious behavior
- Include examples for complex operations
- Reference external resources where appropriate

```kotlin
/**
 * Delegate UserData class to cut down on copy-pasta in each ADT instance of User
 * see: https://proandroiddev.com/simpler-kotlin-class-hierarchies-using-class-delegation-35464106fed5
 */
internal interface UserFields {
  // ...
}
```

### Project Documentation
- Maintain a comprehensive README with project overview
- Document architecture decisions and patterns
- Include setup and development instructions
- Keep documentation up-to-date with code changes

## Language-Specific Practices

### Kotlin Features
- Use data classes for DTOs and value objects
- Use sealed classes for algebraic data types
- Use extension functions for adding behavior to existing classes
- Use property delegation for common patterns
- Use context receivers for cross-cutting concerns
- Use coroutines for asynchronous programming

```kotlin
// Extension function example
fun RegisterUserInputDto.toCommand(): RegisterUserCommand =
  RegisterUserCommand(
    msisdn = this.msisdn,
    email = this.email,
    password = this.password,
    role = this.role.name
  )

// Property delegation example
private val logger by LoggerDelegate()

// Context receiver example
context(Raise<WorkflowError>)
suspend fun execute(request: R): E
```

### Functional Programming
- Use immutable data structures
- Use Arrow for functional programming constructs
- Use monads (Either, Option) for error handling and nullable values
- Use function composition instead of imperative control flow
- Use higher-order functions for behavior parameterization

```kotlin
// Functional error handling example
effect { UnregisteredUser.of(
  msisdn = cmd.msisdn,
  email = cmd.email,
  encryptedPassword = passwordEncoder.encode(cmd.password),
  role = Role.valueOf(cmd.role)
) }.fold(
  { raise(WorkflowValidationError(it)) },
  { it }
)
```

### Type Safety
- Use specific types instead of primitives
- Use sealed classes for representing states
- Use non-nullable types where possible
- Use validation to ensure type invariants

```kotlin
// Type safety example
class NonEmptyString private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    context(Raise<ValidationErrors>)
    fun of(value: String): NonEmptyString = ensure {
      validate(NonEmptyString(value)) {
        validate(NonEmptyString::value).isNotEmpty()
      }
    }.bind()
  }
}
```

## Tools and Configuration

### Build Tools
- Use Maven for build management
- Use the kotlin-maven-plugin for Kotlin compilation
- Enable incremental compilation for faster builds

### Code Quality
- Use ArchUnit for architectural validation
- Follow the coding guidelines consistently
- Review code for adherence to guidelines

### IDE Configuration
- Use IntelliJ IDEA with Kotlin plugin
- Configure code style settings according to guidelines
- Use code inspections to catch common issues

## Examples

### Complete Domain Model Example

```kotlin
internal sealed class User: UserFields {
  fun acl() = Acl.of(id.value, id.value, AclRole.MANAGER)

  companion object {
    context(Raise<ValidationErrors>)
    fun newUserData(
      id: String,
      msisdn: String,
      email: String,
      encryptedPassword: String
    ) = UserData(
      UserId.of(id),
      msisdn.toMsisdn(),
      email.toEmailAddress(),
      encryptedPassword.toNonEmptyString()
    )
  }
}

internal data class UnregisteredUser(
  private val data: UserData,
  val role: Role
) : User(), UserFields by data {
  companion object {
    context(Raise<ValidationErrors>)
    fun of(msisdn: String, email: String, encryptedPassword: String, role: Role): UnregisteredUser =
      UnregisteredUser(
        UserData(
          UserId.create(),
          msisdn.toMsisdn(),
          email.toEmailAddress(),
          encryptedPassword.toNonEmptyString()
        ),
        role)
  }
}

internal data class ActiveUser(
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    context(Raise<ValidationErrors>)
    fun of(id: String, msisdn: String, email: String, encryptedPassword: String): ActiveUser =
      ActiveUser(newUserData(id, msisdn, email, encryptedPassword))
  }
}
```

### Complete Workflow Example

```kotlin
@Component
internal class RegisterUserWorkflow(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val userRegisteredPort: UserEventPort
) : BaseSafeWorkflow<RegisterUserCommand, UserRegisteredEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(Raise<WorkflowError>)
  override suspend fun execute(request: RegisterUserCommand) : UserRegisteredEvent {

    val result = runAsSuperUser {
      val user = validateNewUser(request)
      userRegisteredPort.handle(
        UserRegisteredEvent(user.toUserDto(), user.encryptedPassword.value)
      )
    }
    return result
  }

  context(Raise<WorkflowError>)
  private suspend fun validateNewUser(cmd: RegisterUserCommand) : UnregisteredUser =
    findUserPort.findUserByEmail(cmd.email)
      ?.let { raise(UserExistsError("User ${cmd.msisdn} exists")) }
      ?: effect { UnregisteredUser.of(
        msisdn = cmd.msisdn,
        email = cmd.email,
        encryptedPassword = passwordEncoder.encode(cmd.password),
        role = Role.valueOf(cmd.role)
      ) }.fold(
        { raise(WorkflowValidationError(it)) },
        { it }
      )
}
```

### Complete Controller Example

```kotlin
@RestController
internal class RegisterUserController(val dispatcher: WorkflowDispatcher) {

  @PostMapping("/user/register")
  suspend fun register(@Valid @RequestBody registerUser: RegisterUserInputDto)
    : ResponseEntity<RegisterUserOutputDto> {

    return dispatcher.dispatch<UserRegisteredEvent>(registerUser.toCommand())
      .fold(
        {
          when (it) {
            is UserExistsError -> ResponseEntity.badRequest().body(it.toOutputDto())
            is WorkflowValidationError -> ResponseEntity.badRequest().body(it.toOutputDto())
            else -> ResponseEntity.status(500).body(it.toOutputDto())
          }
        },
        { ResponseEntity.ok(it.toOutputDto()) }
      )
  }

  fun RegisterUserInputDto.toCommand(): RegisterUserCommand =
    RegisterUserCommand(
      msisdn = this.msisdn,
      email = this.email,
      password = this.password,
      role = this.role.name
    )

  fun UserExistsError.toOutputDto(): RegisterUserOutputDto =
    RegisterUserErrorsDto(this.message)

  fun WorkflowValidationError.toOutputDto(): RegisterUserOutputDto =
    RegisterUserErrorsDto(this.message)

  fun WorkflowError.toOutputDto(): RegisterUserOutputDto =
    RegisterUserErrorsDto("Server Error: $this")

  fun UserRegisteredEvent.toOutputDto(): RegisterUserOutputDto = RegisteredUserDto(this.userDto)
}
```
