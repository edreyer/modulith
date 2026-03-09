package io.liquidsoftware.base.payment.adapter.`in`.web

import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean

@Configuration
@ComponentScan(basePackages = ["io.liquidsoftware.base.payment.adapter.in.web"])
class PaymentWebConfig {

  @Bean
  fun paymentApi(dispatcher: WorkflowDispatcher): PaymentApi = object : PaymentApi {
    override suspend fun addPaymentMethod(command: AddPaymentMethodCommand) =
      dispatcher.dispatch<PaymentMethodAddedEvent>(command)

    override suspend fun makePayment(command: MakePaymentCommand) =
      dispatcher.dispatch<PaymentMadeEvent>(command)
  }
}
