package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.application.port.`in`.UserExistsError
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder

class RegisterUserUseCaseTest {

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `persists encoded event as system user and restores caller context`() = runBlocking {
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
  fun `returns exists error when email is already taken`() = runBlocking {
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
  fun `maps invalid input to application validation error`() = runBlocking {
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
    assertThat(error).isInstanceOf(ApplicationError.Validation::class)
    assertThat(error.message).contains("not-a-phone-number")
  }

  @Test
  fun `maps persistence failures to application unexpected errors`() = runBlocking {
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
    assertThat(error).isInstanceOf(ApplicationError.Unexpected::class)
    assertThat(error.message).isEqualTo("db down")
  }
}
