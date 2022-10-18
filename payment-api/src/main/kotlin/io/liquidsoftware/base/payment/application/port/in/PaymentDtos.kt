package io.liquidsoftware.base.payment.application.port.`in`

data class PaymentMethodDtoIn(
  val userId: String,
  val stripePaymentMethodId: String,
  val lastFour: String
)

data class PaymentMethodDtoOut(
  val paymentMethodId: String,
  val userId: String,
  val stripePaymentMethodId: String,
  val lastFour: String
)

data class PaymentDtoIn(
  val userId: String,
  val paymentMethodId: String,
  val amount: Long // value in cents
)

data class PaymentDtoOut(
  val paymentId: String,
  val paymentMethodId: String,
  val userId: String,
  val amount: Long
)
