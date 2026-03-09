package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.user.application.port.`in`.DisableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.EnableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserDisabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserEnabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.application.port.`in`.UserExistsError
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.base.user.domain.ActiveUser
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.security.crypto.password.PasswordEncoder

class UserCommandUseCasesTest {

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `register user persists encoded event as system user and restores caller context`() = runBlocking {
    SecurityContextHolder.getContext().authentication = authentication("caller-id", "ROLE_USER")

    var persistedAsUserId: String? = null
    val useCase = RegisterUserUseCase(
      passwordEncoder = passwordEncoder { "encoded-password" },
      findUserPort = findUserPort(),
      userEventPort = object : UserEventPort {
        override suspend fun handle(event: UserRegisteredEvent): Either<WorkflowError, UserRegisteredEvent> {
          persistedAsUserId = (
            checkNotNull(SecurityContextHolder.getContext().authentication).principal as UserDetailsWithId
          ).id
          return Either.Right(event)
        }

        override suspend fun <T : UserEvent> handle(event: T): Either<WorkflowError, T> =
          Either.Right(event)
      },
    )

    val result = useCase.execute(
      RegisterUserCommand(
        msisdn = "5125550201",
        email = "new-user@liquidsoftware.io",
        password = "password",
        role = RoleDto.ROLE_USER.name,
      )
    )
    val event = result.fold({ error("unexpected error: $it") }, { it })

    assertThat(event.userDto.email).isEqualTo("new-user@liquidsoftware.io")
    assertThat(event.password).isEqualTo("encoded-password")
    assertThat(event.userDto.roles).isEqualTo(listOf(RoleDto.ROLE_USER))
    assertThat(persistedAsUserId).isEqualTo("SYSTEM")
    assertThat((checkNotNull(SecurityContextHolder.getContext().authentication).principal as UserDetailsWithId).id)
      .isEqualTo("caller-id")
  }

  @Test
  fun `register user returns exists error when email is already taken`() = runBlocking {
    val existingUser = activeUser()
    val useCase = RegisterUserUseCase(
      passwordEncoder = passwordEncoder { "encoded-password" },
      findUserPort = findUserPort(findByEmail = { existingUser }),
      userEventPort = failingUserEventPort(),
    )

    val result = useCase.execute(
      RegisterUserCommand(
        msisdn = "5125550201",
        email = existingUser.email.value,
        password = "password",
        role = RoleDto.ROLE_USER.name,
      )
    )

    val error = result.fold({ it }, { error("expected exists error") })
    assertThat(error).isInstanceOf(UserExistsError::class)
    assertThat(error.message).isEqualTo("User 5125550201 exists")
  }

  @Test
  fun `register user maps invalid input to workflow validation error`() = runBlocking {
    val useCase = RegisterUserUseCase(
      passwordEncoder = passwordEncoder { "encoded-password" },
      findUserPort = findUserPort(),
      userEventPort = failingUserEventPort(),
    )

    val result = useCase.execute(
      RegisterUserCommand(
        msisdn = "not-a-phone-number",
        email = "not-an-email",
        password = "password",
        role = RoleDto.ROLE_USER.name,
      )
    )

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(WorkflowValidationError::class)
    assertThat(error.message).contains("not-a-phone-number")
  }

  @Test
  fun `register user maps persistence failures to legacy server errors`() = runBlocking {
    val useCase = RegisterUserUseCase(
      passwordEncoder = passwordEncoder { "encoded-password" },
      findUserPort = findUserPort(),
      userEventPort = object : UserEventPort {
        override suspend fun handle(event: UserRegisteredEvent): Either<WorkflowError, UserRegisteredEvent> =
          Either.Left(ServerError("db down"))

        override suspend fun <T : UserEvent> handle(event: T): Either<WorkflowError, T> =
          Either.Left(ServerError("db down"))
      },
    )

    val result = useCase.execute(
      RegisterUserCommand(
        msisdn = "5125550201",
        email = "new-user@liquidsoftware.io",
        password = "password",
        role = RoleDto.ROLE_USER.name,
      )
    )

    val error = result.fold({ it }, { error("expected server error") })
    assertThat(error).isInstanceOf(ServerError::class)
    assertThat(error.message).isEqualTo("Server Error: db down")
  }

  @Test
  fun `enable user persists enabled event for existing user`() = runBlocking {
    val user = activeUser()
    val useCase = EnableUserUseCase(
      findUserPort = findUserPort(findById = { user }),
      userEventPort = userEventPort(),
    )

    val result = useCase.execute(EnableUserCommand(user.id.value))
    val event = result.fold({ error("unexpected error: $it") }, { it })

    assertThat(event).isInstanceOf(UserEnabledEvent::class)
    assertThat(event.userDto.id).isEqualTo(user.id.value)
  }

  @Test
  fun `disable user persists disabled event for existing user`() = runBlocking {
    val user = activeUser()
    val useCase = DisableUserUseCase(
      findUserPort = findUserPort(findById = { user }),
      userEventPort = userEventPort(),
    )

    val result = useCase.execute(DisableUserCommand(user.id.value))
    val event = result.fold({ error("unexpected error: $it") }, { it })

    assertThat(event).isInstanceOf(UserDisabledEvent::class)
    assertThat(event.userDto.id).isEqualTo(user.id.value)
  }

  @Test
  fun `user admin use cases return not found when target user is missing`() = runBlocking {
    val enableUseCase = EnableUserUseCase(findUserPort = findUserPort(), userEventPort = userEventPort())
    val disableUseCase = DisableUserUseCase(findUserPort = findUserPort(), userEventPort = userEventPort())

    val enableError = enableUseCase.execute(EnableUserCommand("u_missing")).fold({ it }, { error("expected missing user") })
    val disableError = disableUseCase.execute(DisableUserCommand("u_missing")).fold({ it }, { error("expected missing user") })

    assertThat(enableError.message).isEqualTo("User not found with ID u_missing")
    assertThat(disableError.message).isEqualTo("User not found with ID u_missing")
  }

  @Test
  fun `user admin use cases map persistence failures to legacy server errors`() = runBlocking {
    val user = activeUser()
    val useCase = DisableUserUseCase(
      findUserPort = findUserPort(findById = { user }),
      userEventPort = object : UserEventPort {
        override suspend fun handle(event: UserRegisteredEvent): Either<WorkflowError, UserRegisteredEvent> =
          error("unexpected register event")

        override suspend fun <T : UserEvent> handle(event: T): Either<WorkflowError, T> =
          Either.Left(ServerError("db down"))
      },
    )

    val result = useCase.execute(DisableUserCommand(user.id.value))

    val error = result.fold({ it }, { error("expected server error") })
    assertThat(error).isInstanceOf(ServerError::class)
    assertThat(error.message).isEqualTo("Server Error: db down")
  }

  private fun authentication(userId: String, role: String): UsernamePasswordAuthenticationToken {
    val userDetails = UserDetailsWithId(
      id = userId,
      user = SpringUser(
        "caller@liquidsoftware.io",
        "",
        listOf(SimpleGrantedAuthority(role)),
      )
    )
    return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
  }

  private fun passwordEncoder(encode: (String) -> String?): PasswordEncoder = object : PasswordEncoder {
    override fun encode(rawPassword: CharSequence?): String? = encode(checkNotNull(rawPassword).toString())

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean =
      encodedPassword == encode(rawPassword?.toString().orEmpty())
  }

  private fun findUserPort(
    findById: suspend (String) -> User? = { null },
    findByEmail: suspend (String) -> User? = { null },
  ): FindUserPort = object : FindUserPort {
    override suspend fun findUserById(userId: String): Either<WorkflowError, User?> =
      Either.Right(findById(userId))

    override suspend fun findUserByEmail(email: String): Either<WorkflowError, User?> =
      Either.Right(findByEmail(email))

    override suspend fun findUserByMsisdn(msisdn: String): Either<WorkflowError, User?> =
      error("unexpected findUserByMsisdn")
  }

  private fun userEventPort(): UserEventPort = object : UserEventPort {
    override suspend fun handle(event: UserRegisteredEvent): Either<WorkflowError, UserRegisteredEvent> =
      Either.Right(event)

    override suspend fun <T : UserEvent> handle(event: T): Either<WorkflowError, T> =
      Either.Right(event)
  }

  private fun failingUserEventPort(): UserEventPort = object : UserEventPort {
    override suspend fun handle(event: UserRegisteredEvent): Either<WorkflowError, UserRegisteredEvent> =
      error("user event port should not be invoked")

    override suspend fun <T : UserEvent> handle(event: T): Either<WorkflowError, T> =
      error("user event port should not be invoked")
  }

  private fun activeUser(): ActiveUser =
    either {
      ActiveUser.of(
        id = "u_active",
        msisdn = "+15125550111",
        email = "user@liquidsoftware.io",
        encryptedPassword = "encoded-password",
      )
    }.fold({ error("invalid active user") }, { it })
}
