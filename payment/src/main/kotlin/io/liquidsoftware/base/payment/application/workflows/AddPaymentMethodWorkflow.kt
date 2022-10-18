package io.liquidsoftware.base.payment.application.workflows

import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentValidationError
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.port.out.toDto
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.common.ext.mapError
import io.liquidsoftware.common.ext.toResult
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class AddPaymentMethodWorkflow(
  val paymentEventPort: PaymentEventPort
) : BaseSafeWorkflow<AddPaymentMethodCommand, PaymentMethodAddedEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun execute(request: AddPaymentMethodCommand): PaymentMethodAddedEvent {
    return ActivePaymentMethod.of(
      userId = request.paymentMethod.userId,
      stripePaymentMethodId = request.paymentMethod.stripePaymentMethodId,
      lastFour = request.paymentMethod.lastFour
    )
      .toResult()
      .map { paymentEventPort.handle(PaymentMethodAddedEvent(it.toDto())) }
      .mapError { ex -> PaymentValidationError(ex.message ?: "Payment Error")}
      .getOrThrow()
  }
}
