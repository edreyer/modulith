package io.liquidsoftware.base.payment.application.workflows

import arrow.core.continuations.EffectScope
import arrow.core.continuations.ensureNotNull
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.mapper.toDto
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.service.StripeService
import io.liquidsoftware.base.payment.domain.Payment
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.ext.getOrShift
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
internal class MakePaymentWorkflow(
  val findPaymentMethodPort: FindPaymentMethodPort,
  val paymentEventPort: PaymentEventPort,
  val stripeService: StripeService

) : BaseSafeWorkflow<MakePaymentCommand, PaymentMadeEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: MakePaymentCommand): PaymentMadeEvent {
    val pmId = PaymentMethodId.of(request.paymentMethodId).getOrShift()
    val userId = UserId.of(request.userId).getOrShift()
    val pm = ensureNotNull(findPaymentMethodPort.findByPaymentMethodId(pmId, userId)) {
      PaymentMethodNotFoundError(request.paymentMethodId)
    }

    return stripeService.makePayment(pm, request.amount)
      .let {
        Payment.of(
          paymentMethodId = it.paymentMethod.id.value,
          userId = request.userId,
          amount = it.amount
        )
      }
      .map { paymentEventPort.handle(PaymentMadeEvent(it.toDto())) }
      .getOrShift()
  }
}
