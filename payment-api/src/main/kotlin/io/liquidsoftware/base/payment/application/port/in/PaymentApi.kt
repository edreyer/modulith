package io.liquidsoftware.base.payment.application.port.`in`

import io.liquidsoftware.common.workflow.Command
import io.liquidsoftware.common.workflow.Event
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

data class AddPaymentMethodCommand(
  val userId: String,
  val stripePaymentMethodId: String,
  val lastFour: String
) : Command

data class MakePayment(
  val userId: String,
  val paymentMethodId: String,
  val amount: Int
) : Command

sealed interface PaymentMethodEvent

data class PaymentMethodAddedEvent (
  val paymentMethodDto: PaymentMethodDtoOut
) : PaymentMethodEvent, Event()

sealed interface PaymentEvent

data class PaymentMadeEvent (
  val paymentDto: PaymentDtoOut
) : PaymentEvent, Event()

sealed class PaymentError : RuntimeException() {
  abstract val error: String
}

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class PaymentDeclinedError(override val error: String): PaymentError()

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class PaymentValidationError(override val error: String) : PaymentError()
