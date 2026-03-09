package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable

class FetchUserAppointmentsUseCaseTest {

  @Test
  fun `filters cancelled appointments`() = runBlocking {
    val scheduled = buildScheduledAppointment(apptId = "a_scheduled")
    val cancelled = CancelledAppointment.of(buildScheduledAppointment(apptId = "a_cancelled"))
    var pageable: Pageable? = null
    val useCase = FetchUserAppointmentsUseCase(
      findAppointmentPort = TestFindAppointmentPort(
        findByUserIdBlock = { _, requestedPageable ->
          pageable = requestedPageable
          Either.Right(listOf(scheduled, cancelled))
        }
      )
    )

    val result = useCase.execute(FetchUserAppointmentsQuery(userId = "u_test-user", page = 2, size = 5))

    val event = result.fold({ error("unexpected fetch error: ${it.message}") }, { it })
    assertThat(event.appointments).hasSize(1)
    assertThat(event.appointments[0].id).isEqualTo("a_scheduled")
    assertThat(pageable?.pageNumber).isEqualTo(2)
    assertThat(pageable?.pageSize).isEqualTo(5)
  }
}
