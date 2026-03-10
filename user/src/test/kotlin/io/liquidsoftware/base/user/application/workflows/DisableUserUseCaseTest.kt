package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.user.application.port.`in`.DisableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserDisabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class DisableUserUseCaseTest {

  @Test
  fun `persists disabled event for existing user`() = runBlocking {
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
  fun `returns not found when target user is missing`() = runBlocking {
    val useCase = DisableUserUseCase(findUserPort = findUserPort(), userEventPort = userEventPort())

    val error = useCase.execute(DisableUserCommand("u_missing")).fold({ it }, { error("expected missing user") })

    assertThat(error.message).isEqualTo("User not found with ID u_missing")
  }

  @Test
  fun `maps persistence failures to application unexpected errors`() = runBlocking {
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
    assertThat(error).isInstanceOf(ApplicationError.Unexpected::class)
    assertThat(error.message).isEqualTo("db down")
  }
}
