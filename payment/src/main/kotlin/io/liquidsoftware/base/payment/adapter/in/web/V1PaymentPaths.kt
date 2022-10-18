package io.liquidsoftware.base.payment.adapter.`in`.web

internal object V1PaymentPaths {
  private const val API = "/api"
  private const val V1 = "$API/v1"

  const val PAYMENT_METHODS = "$V1/payment-methods"
  const val PAYMENT = "$V1/payments"

}
