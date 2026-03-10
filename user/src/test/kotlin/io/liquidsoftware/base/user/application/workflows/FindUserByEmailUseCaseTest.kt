package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class FindUserByEmailUseCaseTest {

  @Test
  fun `returns found event`() = runBlocking {
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
  fun `returns user not found for missing email`() = runBlocking {
    val useCase = FindUserByEmailUseCase(findUserPort())

    val error = useCase.execute(FindUserByEmailQuery("missing@liquidsoftware.io"))
      .fold({ it }, { error("expected missing user") })

    assertThat(error).isInstanceOf(UserNotFoundError::class)
    assertThat(error.message).isEqualTo("missing@liquidsoftware.io")
  }

  @Test
  fun `maps port failures to application unexpected errors`() = runBlocking {
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
    assertThat(error).isInstanceOf(ApplicationError.Unexpected::class)
    assertThat(error.message).isEqualTo("db down")
  }
}
