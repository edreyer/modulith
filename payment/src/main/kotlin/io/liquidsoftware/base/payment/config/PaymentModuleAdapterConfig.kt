package io.liquidsoftware.base.payment.config

import io.liquidsoftware.base.payment.adapter.out.module.LocalPaymentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentModuleAdapterConfig {
  @Bean fun paymentApi(): PaymentApi = LocalPaymentApi()
}
