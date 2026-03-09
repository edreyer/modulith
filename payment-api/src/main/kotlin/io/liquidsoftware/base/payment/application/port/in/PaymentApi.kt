package io.liquidsoftware.base.payment.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.usecase.AppEvent
import io.liquidsoftware.common.usecase.Command
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

data class AddPaymentMethodCommand(
  val userId: String,
  val stripePaymentMethodId: String,
  val lastFour: String
) : Command

data class MakePaymentCommand(
  val paymentMethodId: String,
  val amount: Long
) : Command

sealed interface PaymentMethodEvent

data class PaymentMethodAddedEvent(
  val paymentMethodDto: PaymentMethodDtoOut
) : PaymentMethodEvent, AppEvent()

sealed interface PaymentEvent

data class PaymentMadeEvent(
  val paymentDto: PaymentDtoOut
) : PaymentEvent, AppEvent()

@ResponseStatus(code = HttpStatus.NOT_FOUND)
data class PaymentMethodNotFoundError(override val message: String) : WorkflowError(message)

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class PaymentDeclinedError(override val message: String) : WorkflowError(message)

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class PaymentValidationError(override val message: String) : WorkflowError(message)

interface PaymentApi {
  suspend fun addPaymentMethod(command: AddPaymentMethodCommand): Either<WorkflowError, PaymentMethodAddedEvent>
  suspend fun makePayment(command: MakePaymentCommand): Either<WorkflowError, PaymentMadeEvent>
}
