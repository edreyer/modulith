package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class StartAppointmentUseCaseTest {

  @Test
  fun `persists in progress appointment`() = runBlocking {
    val appointmentEventPort = RecordingAppointmentEventPort()
    val scheduled = buildScheduledAppointment()
    val useCase = StartAppointmentUseCase(
      findAppointmentPort = TestFindAppointmentPort(findScheduledByIdBlock = { Either.Right(scheduled) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(StartAppointmentCommand(scheduled.id.value))

    val event = result.fold({ error("unexpected start error: ${it.message}") }, { it })
    assertThat(event.appointmentDto.status).isEqualTo(AppointmentStatus.IN_PROGRESS)
    assertThat(appointmentEventPort.events).hasSize(1)
  }

  @Test
  fun `returns validation error when scheduled appointment is missing`() = runBlocking {
    val useCase = StartAppointmentUseCase(
      findAppointmentPort = TestFindAppointmentPort(findScheduledByIdBlock = { Either.Right(null) }),
      appointmentEventPort = RecordingAppointmentEventPort(),
    )

    val result = useCase.execute(StartAppointmentCommand("a_missing"))

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(AppointmentValidationError::class)
    Unit
  }
}
