package io.liquidsoftware.base.booking.application.workflows

import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
import arrow.core.continuations.ensureNotNull
import arrow.core.continuations.toResult
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.port.out.toDto
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import io.liquidsoftware.common.ext.getOrShift
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class CancelAppointmentWorkflow(
  private val apptStateService: AppointmentStateService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<CancelAppointmentCommand, AppointmentCancelledEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: CancelAppointmentCommand): AppointmentCancelledEvent {
    return ensureNotNull(findAppointmentPort.findById(request.appointmentId)) {
      AppointmentNotFoundError("Appointment(${request.appointmentId} not found")
    }
      .let { effect { apptStateService.cancel(it) }.toResult().getOrShift() }
      .let { appointmentEventPort.handle(AppointmentCancelledEvent(it.toDto()))}
  }

}
