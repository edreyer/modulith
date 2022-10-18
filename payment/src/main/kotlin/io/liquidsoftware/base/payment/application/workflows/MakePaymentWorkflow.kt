package io.liquidsoftware.base.payment.application.workflows

import arrow.core.flatMap
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.port.out.toDto
import io.liquidsoftware.base.payment.application.service.StripeService
import io.liquidsoftware.base.payment.domain.Payment
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.ext.toResult
import io.liquidsoftware.common.types.getOrThrow
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class MakePaymentWorkflow(
  val findPaymentMethodPort: FindPaymentMethodPort,
  val paymentEventPort: PaymentEventPort,
  val stripeService: StripeService

) : BaseSafeWorkflow<MakePaymentCommand, PaymentMadeEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun execute(request: MakePaymentCommand): PaymentMadeEvent {
    val pmId = PaymentMethodId.of(request.paymentMethodId).getOrThrow()
    val userId = UserId.of(request.userId).getOrThrow()
    val pm = findPaymentMethodPort.findByPaymentMethodId(pmId, userId)
      ?: throw PaymentMethodNotFoundError(request.paymentMethodId)

    return stripeService.makePayment(pm, request.amount)
      .flatMap {
        Payment.of(
          paymentMethodId = it.paymentMethod.id.value,
          userId = request.userId,
          amount = it.amount
        ).toResult()
      }
      .map { paymentEventPort.handle(PaymentMadeEvent(it.toDto())) }
      .getOrThrow()
  }
}
