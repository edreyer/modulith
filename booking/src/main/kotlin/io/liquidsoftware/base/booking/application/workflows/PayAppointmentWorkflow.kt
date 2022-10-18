package io.liquidsoftware.base.booking.application.workflows

import arrow.core.flatMap
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaymentError
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.port.out.toDto
import io.liquidsoftware.base.booking.domain.PaidAppointment
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.common.ext.toResult
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowDispatcher.log
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class PayAppointmentWorkflow(
  private val ec: ExecutionContext,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<PayAppointmentCommand, AppointmentPaidEvent>() {

  private val logger by LoggerDelegate()

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun execute(request: PayAppointmentCommand): AppointmentPaidEvent {
    // 1) Ensure appt is in correct state
    val completeAppt = findAppointmentPort.findCompletedById(request.appointmentId)
      ?: throw AppointmentPaymentError("Appointment(${request.appointmentId} must be Completed")


    // 2) attempt a payment on the appt
    return WorkflowDispatcher.dispatch<PaymentMadeEvent>(MakePaymentCommand(
      ec.getCurrentUser().id,
      request.paymentMethodId,
      completeAppt.totalDue()
    ))
      .flatMap { PaidAppointment.of(completeAppt, it.paymentDto.paymentId).toResult() }
      .map { appointmentEventPort.handle(AppointmentPaidEvent(it.toDto())) }
      .onFailure {
        log.error("Failed to make payment on appt ${request.appointmentId}", it)
      }.getOrThrow()
  }

}
