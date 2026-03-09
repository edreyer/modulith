package io.liquidsoftware.base.user.application.workflows

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class FindUserByIdUseCaseTest {

  @Test
  fun `returns found event`() = runBlocking {
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
  fun `returns user not found for missing id`() = runBlocking {
    val useCase = FindUserByIdUseCase(findUserPort())

    val error = useCase.execute(FindUserByIdQuery("u_missing")).fold({ it }, { error("expected missing user") })

    assertThat(error).isInstanceOf(UserNotFoundError::class)
    assertThat(error.message).isEqualTo("u_missing")
  }
}
