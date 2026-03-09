package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.booking.application.port.`in`.DateInPastError
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class GetAvailabilityUseCaseTest {

  @Test
  fun `returns remaining times for a future date`() = runBlocking {
    val date = LocalDate.now().plusDays(1)
    val useCase = GetAvailabilityUseCase(
      findAppointmentPort = TestFindAppointmentPort(
        findAllForAvailabilityBlock = {
          Either.Right(listOf(buildScheduledAppointment(scheduledTime = LocalDateTime.of(date, LocalTime.of(9, 0)))))
        }
      ),
      availabilityService = AvailabilityService(),
    )

    val result = useCase.execute(GetAvailabilityQuery(date))

    val event = result.fold({ error("unexpected availability error: ${it.message}") }, { it })
    assertThat(event.times).doesNotContain(LocalTime.of(9, 0))
    assertThat(event.times).contains(LocalTime.of(10, 0))
  }

  @Test
  fun `rejects past dates`() = runBlocking {
    val useCase = GetAvailabilityUseCase(
      findAppointmentPort = TestFindAppointmentPort(),
      availabilityService = AvailabilityService(),
    )

    val result = useCase.execute(GetAvailabilityQuery(LocalDate.now().minusDays(1)))

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(DateInPastError::class)
    Unit
  }
}
