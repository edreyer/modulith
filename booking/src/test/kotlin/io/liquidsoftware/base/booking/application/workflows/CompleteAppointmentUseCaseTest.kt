package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class CompleteAppointmentUseCaseTest {

  @Test
  fun `persists completed appointment`() = runBlocking {
    val appointmentEventPort = RecordingAppointmentEventPort()
    val inProgress = buildInProgressAppointment()
    val useCase = CompleteAppointmentUseCase(
      findAppointmentPort = TestFindAppointmentPort(findStartedByIdBlock = { Either.Right(inProgress) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(CompleteAppointmentCommand(inProgress.id.value, "Complete"))

    val event = result.fold({ error("unexpected complete error: ${it.message}") }, { it })
    assertThat(event.appointmentDto.status).isEqualTo(AppointmentStatus.COMPLETE)
    assertThat(event.appointmentDto.workOrderDto.notes).isEqualTo("Complete")
    assertThat(appointmentEventPort.events).hasSize(1)
  }

  @Test
  fun `returns not found when in progress appointment is missing`() = runBlocking {
    val useCase = CompleteAppointmentUseCase(
      findAppointmentPort = TestFindAppointmentPort(findStartedByIdBlock = { Either.Right(null) }),
      appointmentEventPort = RecordingAppointmentEventPort(),
    )

    val result = useCase.execute(CompleteAppointmentCommand("a_missing", "Complete"))

    val error = result.fold({ it }, { error("expected not found error") })
    assertThat(error).isInstanceOf(AppointmentNotFoundError::class)
    Unit
  }
}
