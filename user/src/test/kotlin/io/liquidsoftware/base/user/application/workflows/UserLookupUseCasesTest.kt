package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.ActiveUser
import io.liquidsoftware.base.user.domain.AdminUser
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class UserLookupUseCasesTest {

  @Test
  fun `find user by id returns found event`() = runBlocking {
    val user = activeUser()
    val useCase = FindUserByIdUseCase(
      findUserPort = findUserPort(findById = { id -> user.takeIf { it.id.value == id } })
    )

    val result = useCase.execute(FindUserByIdQuery(user.id.value))
    val event = result.fold({ error("unexpected error: $it") }, { it })

    assertThat(event.userDto.id).isEqualTo(user.id.value)
    assertThat(event.userDto.email).isEqualTo(user.email.value)
    assertThat(event.userDto.msisdn).isEqualTo(user.msisdn.value)
    assertThat(event.userDto.roles).containsExactly(RoleDto.ROLE_USER)
  }

  @Test
  fun `find user by email returns found event`() = runBlocking {
    val user = adminUser()
    val useCase = FindUserByEmailUseCase(
      findUserPort = findUserPort(findByEmail = { email -> user.takeIf { it.email.value == email } })
    )

    val result = useCase.execute(FindUserByEmailQuery(user.email.value))
    val event = result.fold({ error("unexpected error: $it") }, { it })

    assertThat(event.userDto.id).isEqualTo(user.id.value)
    assertThat(event.userDto.roles).containsExactly(RoleDto.ROLE_ADMIN)
  }

  @Test
  fun `find user by msisdn returns found event`() = runBlocking {
    val user = activeUser()
    val useCase = FindUserByMsisdnUseCase(
      findUserPort = findUserPort(findByMsisdn = { msisdn -> user.takeIf { it.msisdn.value == msisdn } })
    )

    val result = useCase.execute(FindUserByMsisdnQuery(user.msisdn.value))
    val event = result.fold({ error("unexpected error: $it") }, { it })

    assertThat(event.userDto.id).isEqualTo(user.id.value)
    assertThat(event.userDto.msisdn).isEqualTo(user.msisdn.value)
  }

  @Test
  fun `lookup use cases return user not found for missing values`() = runBlocking {
    val byId = FindUserByIdUseCase(findUserPort())
    val byEmail = FindUserByEmailUseCase(findUserPort())
    val byMsisdn = FindUserByMsisdnUseCase(findUserPort())

    val idError = byId.execute(FindUserByIdQuery("u_missing")).fold({ it }, { error("expected missing user") })
    val emailError = byEmail.execute(FindUserByEmailQuery("missing@liquidsoftware.io")).fold({ it }, { error("expected missing user") })
    val msisdnError = byMsisdn.execute(FindUserByMsisdnQuery("+15125559999")).fold({ it }, { error("expected missing user") })

    assertThat(idError).isInstanceOf(UserNotFoundError::class)
    assertThat(idError.message).isEqualTo("u_missing")
    assertThat(emailError).isInstanceOf(UserNotFoundError::class)
    assertThat(emailError.message).isEqualTo("missing@liquidsoftware.io")
    assertThat(msisdnError).isInstanceOf(UserNotFoundError::class)
    assertThat(msisdnError.message).isEqualTo("+15125559999")
  }

  @Test
  fun `lookup use cases map port failures to legacy server errors`() = runBlocking {
    val useCase = FindUserByEmailUseCase(
      findUserPort = object : FindUserPort {
        override suspend fun findUserById(userId: String): Either<WorkflowError, User?> =
          error("unexpected findUserById")

        override suspend fun findUserByEmail(email: String): Either<WorkflowError, User?> =
          Either.Left(ServerError("db down"))

        override suspend fun findUserByMsisdn(msisdn: String): Either<WorkflowError, User?> =
          error("unexpected findUserByMsisdn")
      }
    )

    val result = useCase.execute(FindUserByEmailQuery("user@liquidsoftware.io"))

    val error = result.fold({ it }, { error("expected server error") })
    assertThat(error).isInstanceOf(ServerError::class)
    assertThat(error.message).isEqualTo("Server Error: db down")
  }

  private fun findUserPort(
    findById: suspend (String) -> User? = { null },
    findByEmail: suspend (String) -> User? = { null },
    findByMsisdn: suspend (String) -> User? = { null },
  ): FindUserPort = object : FindUserPort {
    override suspend fun findUserById(userId: String): Either<WorkflowError, User?> =
      Either.Right(findById(userId))

    override suspend fun findUserByEmail(email: String): Either<WorkflowError, User?> =
      Either.Right(findByEmail(email))

    override suspend fun findUserByMsisdn(msisdn: String): Either<WorkflowError, User?> =
      Either.Right(findByMsisdn(msisdn))
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

  private fun adminUser(): AdminUser =
    either {
      AdminUser.of(
        id = "u_admin",
        msisdn = "+15125550112",
        email = "admin@liquidsoftware.io",
        encryptedPassword = "encoded-password",
      )
    }.fold({ error("invalid admin user") }, { it })
}
