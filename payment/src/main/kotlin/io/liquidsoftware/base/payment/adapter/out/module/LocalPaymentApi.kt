package io.liquidsoftware.base.payment.adapter.out.module

import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.common.context.ModuleApiRegistry

class LocalPaymentApi : PaymentApi {
  override suspend fun addPaymentMethod(command: AddPaymentMethodCommand) =
    ModuleApiRegistry.require(PaymentApi::class).addPaymentMethod(command)

  override suspend fun makePayment(command: MakePaymentCommand) =
    ModuleApiRegistry.require(PaymentApi::class).makePayment(command)
}
