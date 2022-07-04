package io.liquidsoftware.base.payment.application.workflows

import arrow.core.Nel
import arrow.core.continuations.either
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentValidationError
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.port.out.toDto
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.toErrString
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
    return either<Nel<ValidationError>, PaymentMethod> {
      ActivePaymentMethod.of(
        userId = request.userId,
        stripePaymentMethodId = request.stripePaymentMethodId,
        lastFour = request.lastFour
      ).bind()
    }
      .fold({
        Result.failure(PaymentValidationError(it.toErrString()))
      }, {
        Result.success(paymentEventPort.handle(PaymentMethodAddedEvent(it.toDto())))
      }).getOrThrow()
  }
}
