package io.liquidsoftware.base.payment.adapter.`in`.web

import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.common.context.ModuleApiRegistry
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["io.liquidsoftware.base.payment.adapter.in.web"])
class PaymentWebConfig {

  @Bean
  fun paymentApi(): PaymentApi = object : PaymentApi {
    override suspend fun addPaymentMethod(command: AddPaymentMethodCommand) =
      ModuleApiRegistry.require(PaymentApi::class).addPaymentMethod(command)

    override suspend fun makePayment(command: MakePaymentCommand) =
      ModuleApiRegistry.require(PaymentApi::class).makePayment(command)
  }
}
