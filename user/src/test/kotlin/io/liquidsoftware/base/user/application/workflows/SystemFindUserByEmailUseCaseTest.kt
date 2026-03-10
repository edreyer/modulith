package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.AdminUser
import io.liquidsoftware.base.user.domain.DisabledUser
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SystemFindUserByEmailUseCaseTest {

  @Test
  fun `returns enabled admin user details when user exists`() = runBlocking {
    val user = adminUser()
    val useCase = SystemFindUserByEmailUseCase(
      findUserPort = findUserPort { email -> user.takeIf { it.email.value == email } }
    )

    val result = useCase.execute(SystemFindUserByEmailQuery(user.email.value))
    val event = result.fold({ error("unexpected error: $it") }, { it })

    assertThat(event.userDetailsDto.id).isEqualTo(user.id.value)
    assertThat(event.userDetailsDto.username).isEqualTo(user.email.value)
    assertThat(event.userDetailsDto.password).isEqualTo(user.encryptedPassword.value)
    assertThat(event.userDetailsDto.user.isEnabled).isTrue()
    assertThat(event.userDetailsDto.authorities.map { it.authority }).containsExactly("ROLE_ADMIN")
  }

  @Test
  fun `returns disabled user details without authorities for disabled user`() = runBlocking {
    val user = disabledUser()
    val useCase = SystemFindUserByEmailUseCase(
      findUserPort = findUserPort { email -> user.takeIf { it.email.value == email } }
    )

    val result = useCase.execute(SystemFindUserByEmailQuery(user.email.value))
    val event = result.fold({ error("unexpected error: $it") }, { it })

    assertThat(event.userDetailsDto.id).isEqualTo(user.id.value)
    assertThat(event.userDetailsDto.user.isEnabled).isFalse()
    assertThat(event.userDetailsDto.authorities.map { it.authority }).isEqualTo(emptyList())
  }

  @Test
  fun `returns user not found when no user matches the email`() = runBlocking {
    val useCase = SystemFindUserByEmailUseCase(
      findUserPort = findUserPort { null }
    )

    val result = useCase.execute(SystemFindUserByEmailQuery("missing@liquidsoftware.io"))

    val error = result.fold({ it }, { error("expected user not found") })
    assertThat(error).isInstanceOf(UserNotFoundError::class)
    assertThat(error.message).isEqualTo("missing@liquidsoftware.io")
  }

  @Test
  fun `maps port failures to application unexpected errors`() = runBlocking {
    val useCase = SystemFindUserByEmailUseCase(
      findUserPort = object : FindUserPort {
        override suspend fun findUserById(userId: String): Either<WorkflowError, User?> =
          error("unexpected findUserById")

        override suspend fun findUserByEmail(email: String): Either<WorkflowError, User?> =
          Either.Left(ServerError("db down"))

        override suspend fun findUserByMsisdn(msisdn: String): Either<WorkflowError, User?> =
          error("unexpected findUserByMsisdn")
      }
    )

    val result = useCase.execute(SystemFindUserByEmailQuery("user@liquidsoftware.io"))

    val error = result.fold({ it }, { error("expected server error") })
    assertThat(error).isInstanceOf(ApplicationError.Unexpected::class)
    assertThat(error.message).isEqualTo("db down")
  }

  private fun findUserPort(
    findByEmail: suspend (String) -> User?,
  ): FindUserPort = object : FindUserPort {
    override suspend fun findUserById(userId: String): Either<WorkflowError, User?> =
      error("unexpected findUserById")

    override suspend fun findUserByEmail(email: String): Either<WorkflowError, User?> =
      Either.Right(findByEmail(email))

    override suspend fun findUserByMsisdn(msisdn: String): Either<WorkflowError, User?> =
      error("unexpected findUserByMsisdn")
  }

  private fun adminUser(): AdminUser =
    either {
      AdminUser.of(
        id = "u_admin",
        msisdn = "+15125550101",
        email = "admin@liquidsoftware.io",
        encryptedPassword = "encoded-password",
      )
    }.fold({ error("invalid admin user") }, { it })

  private fun disabledUser(): DisabledUser =
    either {
      DisabledUser.of(
        id = "u_disabled",
        msisdn = "+15125550102",
        email = "disabled@liquidsoftware.io",
        encryptedPassword = "encoded-password",
        role = Role.ROLE_USER,
      )
    }.fold({ error("invalid disabled user") }, { it })
}
