package io.liquidsoftware.base.booking.application.workflows

import arrow.core.raise.Raise
import arrow.core.raise.either
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.PaidAppointment
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.common.ext.bind
import io.liquidsoftware.common.ext.ensureNotNull
import io.liquidsoftware.common.ext.raise
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import io.liquidsoftware.common.workflow.WorkflowValidationError
import org.springframework.stereotype.Component

@Component
internal class PayAppointmentWorkflow(
  private val ec: ExecutionContext,
  private val dispatcher: WorkflowDispatcher,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<PayAppointmentCommand, AppointmentPaidEvent>() {

  private val log by LoggerDelegate()

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: PayAppointmentCommand): AppointmentPaidEvent {
    // 1) Ensure appt is in correct state
    val completeAppt = ensureNotNull(findAppointmentPort.findCompletedById(request.appointmentId)) {
      AppointmentNotFoundError("Appointment(${request.appointmentId} must be Completed")
    }

    // 2) attempt a payment on the appt
    return dispatcher.dispatch<PaymentMadeEvent>(MakePaymentCommand(
      userId = ec.getCurrentUser().id,
      paymentMethodId = request.paymentMethodId,
      amount = completeAppt.totalDue()
    ))
      .map { either { PaidAppointment.of(completeAppt, it.paymentDto.paymentId) }.fold(
        { raise(WorkflowValidationError(it)) },
        { it }
      )}
      .map { appointmentEventPort.handle(AppointmentPaidEvent(it.toDto())) }
      .onLeft { ex ->
        log.error("Failed to make payment on appt ${request.appointmentId}", ex)
      }.bind()

  }

}
