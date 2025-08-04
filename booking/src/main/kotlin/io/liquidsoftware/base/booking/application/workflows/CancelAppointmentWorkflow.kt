package io.liquidsoftware.base.booking.application.workflows

import arrow.core.raise.Raise
import arrow.core.raise.result
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import io.liquidsoftware.common.ext.ensureNotNull
import io.liquidsoftware.common.ext.getOrRaise
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class CancelAppointmentWorkflow(
  private val apptStateService: AppointmentStateService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<CancelAppointmentCommand, AppointmentCancelledEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: CancelAppointmentCommand): AppointmentCancelledEvent {
    return ensureNotNull(findAppointmentPort.findById(request.appointmentId)) {
      AppointmentNotFoundError("Appointment(${request.appointmentId} not found")
    }
      .let { result { apptStateService.cancel(it) }.getOrRaise() }
      .let { appointmentEventPort.handle(AppointmentCancelledEvent(it.toDto()))}
  }

}
