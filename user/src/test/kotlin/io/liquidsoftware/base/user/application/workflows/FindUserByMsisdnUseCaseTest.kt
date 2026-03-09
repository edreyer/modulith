package io.liquidsoftware.base.user.application.workflows

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class FindUserByMsisdnUseCaseTest {

  @Test
  fun `returns found event`() = runBlocking {
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
  fun `returns user not found for missing msisdn`() = runBlocking {
    val useCase = FindUserByMsisdnUseCase(findUserPort())

    val error = useCase.execute(FindUserByMsisdnQuery("+15125559999")).fold({ it }, { error("expected missing user") })

    assertThat(error).isInstanceOf(UserNotFoundError::class)
    assertThat(error.message).isEqualTo("+15125559999")
  }
}
