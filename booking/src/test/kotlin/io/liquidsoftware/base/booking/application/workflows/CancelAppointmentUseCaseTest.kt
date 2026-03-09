package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentError
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class CancelAppointmentUseCaseTest {

  @Test
  fun `persists cancelled appointment`() = runBlocking {
    val appointmentEventPort = RecordingAppointmentEventPort()
    val scheduled = buildScheduledAppointment()
    val useCase = CancelAppointmentUseCase(
      appointmentStateService = AppointmentStateService(),
      findAppointmentPort = TestFindAppointmentPort(findByIdBlock = { Either.Right(scheduled) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(CancelAppointmentCommand(scheduled.id.value, "Cancelled"))

    val event = result.fold({ error("unexpected cancel error: ${it.message}") }, { it })
    assertThat(event.appointmentDto.status).isEqualTo(AppointmentStatus.CANCELLED)
    assertThat(appointmentEventPort.events).hasSize(1)
  }

  @Test
  fun `returns error when appointment state cannot be cancelled`() = runBlocking {
    val completed = buildCompletedAppointment()
    val appointmentEventPort = RecordingAppointmentEventPort()
    val useCase = CancelAppointmentUseCase(
      appointmentStateService = AppointmentStateService(),
      findAppointmentPort = TestFindAppointmentPort(findByIdBlock = { Either.Right(completed) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(CancelAppointmentCommand(completed.id.value, "Cancelled"))

    val error = result.fold({ it }, { error("expected cancellation error") })
    assertThat(error).isInstanceOf(CancelAppointmentError::class)
    assertThat(appointmentEventPort.events).hasSize(0)
  }
}
