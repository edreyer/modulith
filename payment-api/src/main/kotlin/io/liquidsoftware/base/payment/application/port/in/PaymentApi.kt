package io.liquidsoftware.base.payment.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.NotFoundApplicationError
import io.liquidsoftware.common.application.error.ValidationApplicationError
import io.liquidsoftware.common.usecase.AppEvent
import io.liquidsoftware.common.usecase.Command

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

data class PaymentMethodNotFoundError(
  override val message: String,
) : NotFoundApplicationError {
  override val code: String = "PAYMENT_METHOD_NOT_FOUND"
  override val metadata: Map<String, String> = emptyMap()
}

data class PaymentDeclinedError(
  override val message: String,
) : ValidationApplicationError {
  override val code: String = "PAYMENT_DECLINED"
  override val metadata: Map<String, String> = emptyMap()
}

data class PaymentValidationError(
  override val message: String,
) : ValidationApplicationError {
  override val code: String = "PAYMENT_VALIDATION_ERROR"
  override val metadata: Map<String, String> = emptyMap()
}

interface PaymentApi {
  suspend fun addPaymentMethod(command: AddPaymentMethodCommand): Either<ApplicationError, PaymentMethodAddedEvent>
  suspend fun makePayment(command: MakePaymentCommand): Either<ApplicationError, PaymentMadeEvent>
}
