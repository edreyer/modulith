package io.liquidsoftware.base.payment.application.port.`in`

import io.liquidsoftware.common.workflow.Command
import io.liquidsoftware.common.workflow.Event
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

data class AddPaymentMethodCommand(
  val paymentMethod: PaymentMethodDtoIn
) : Command

data class MakePaymentCommand(
  val userId: String,
  val paymentMethodId: String,
  val amount: Long
) : Command

sealed interface PaymentMethodEvent

data class PaymentMethodAddedEvent (
  val paymentMethodDto: PaymentMethodDtoOut
) : PaymentMethodEvent, Event()

sealed interface PaymentEvent

data class PaymentMadeEvent (
  val paymentDto: PaymentDtoOut
) : PaymentEvent, Event()

@ResponseStatus(code = HttpStatus.NOT_FOUND)
data class PaymentMethodNotFoundError(override val message: String): WorkflowError(message)

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class PaymentDeclinedError(override val message: String): WorkflowError(message)

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class PaymentValidationError(override val message: String) : WorkflowError(message)
