package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.DateTimeUnavailableError
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderDtoIn
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ScheduleAppointmentUseCaseTest {

  @Test
  fun `persists scheduled appointment when time is available`() = runBlocking {
    val appointmentEventPort = RecordingAppointmentEventPort()
    val useCase = ScheduleAppointmentUseCase(
      availabilityService = AvailabilityService(),
      findAppointmentPort = TestFindAppointmentPort(findAllForAvailabilityBlock = { Either.Right(emptyList()) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(
      ScheduleAppointmentCommand(
        userId = "u_test-user",
        scheduledTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
        duration = 30,
        workOrder = WorkOrderDtoIn(service = "Oil Change", notes = "Scheduled"),
      )
    )

    val event = result.fold({ error("unexpected scheduling error: ${it.message}") }, { it })
    assertThat(event.appointmentDto.status).isEqualTo(AppointmentStatus.SCHEDULED)
    assertThat(event.appointmentDto.workOrderDto.notes).isEqualTo("Scheduled")
    assertThat(appointmentEventPort.events).hasSize(1)
  }

  @Test
  fun `returns unavailable when time is already booked`() = runBlocking {
    val scheduledTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)
    val useCase = ScheduleAppointmentUseCase(
      availabilityService = AvailabilityService(),
      findAppointmentPort = TestFindAppointmentPort(
        findAllForAvailabilityBlock = { Either.Right(listOf(buildScheduledAppointment(scheduledTime = scheduledTime))) }
      ),
      appointmentEventPort = RecordingAppointmentEventPort(),
    )

    val result = useCase.execute(
      ScheduleAppointmentCommand(
        userId = "u_test-user",
        scheduledTime = scheduledTime,
        duration = 30,
        workOrder = WorkOrderDtoIn(service = "Oil Change", notes = "Scheduled"),
      )
    )

    val error = result.fold({ it }, { error("expected unavailable error") })
    assertThat(error).isInstanceOf(DateTimeUnavailableError::class)
    Unit
  }

  @Test
  fun `maps invalid input to appointment validation error`() = runBlocking {
    val useCase = ScheduleAppointmentUseCase(
      availabilityService = AvailabilityService(),
      findAppointmentPort = TestFindAppointmentPort(findAllForAvailabilityBlock = { Either.Right(emptyList()) }),
      appointmentEventPort = RecordingAppointmentEventPort(),
    )

    val result = useCase.execute(
      ScheduleAppointmentCommand(
        userId = "u_test-user",
        scheduledTime = LocalDateTime.now().plusDays(1),
        duration = 0,
        workOrder = WorkOrderDtoIn(service = "", notes = null),
      )
    )

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(AppointmentValidationError::class)
    Unit
  }
}
