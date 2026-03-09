package io.liquidsoftware.base.user.application.workflows

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.user.application.port.`in`.EnableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserEnabledEvent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class EnableUserUseCaseTest {

  @Test
  fun `persists enabled event for existing user`() = runBlocking {
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
  fun `returns not found when target user is missing`() = runBlocking {
    val useCase = EnableUserUseCase(findUserPort = findUserPort(), userEventPort = userEventPort())

    val error = useCase.execute(EnableUserCommand("u_missing")).fold({ it }, { error("expected missing user") })

    assertThat(error.message).isEqualTo("User not found with ID u_missing")
  }
}
