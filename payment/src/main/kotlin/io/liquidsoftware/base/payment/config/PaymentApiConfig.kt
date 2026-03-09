package io.liquidsoftware.base.payment.config

import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.service.StripeService
import io.liquidsoftware.base.payment.application.workflows.AddPaymentMethodUseCase
import io.liquidsoftware.base.payment.application.workflows.MakePaymentUseCase
import io.liquidsoftware.common.context.ModuleApiRegistration
import io.liquidsoftware.common.context.ModuleApiRegistry
import io.liquidsoftware.common.security.ExecutionContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentApiConfig {

  @Bean
  internal fun paymentApiRegistration(
    executionContext: ExecutionContext,
    findPaymentMethodPort: FindPaymentMethodPort,
    paymentEventPort: PaymentEventPort,
    stripeService: StripeService,
  ): ModuleApiRegistration<PaymentApi> {
    val addPaymentMethodUseCase = AddPaymentMethodUseCase(paymentEventPort)
    val makePaymentUseCase = MakePaymentUseCase(
      executionContext = executionContext,
      findPaymentMethodPort = findPaymentMethodPort,
      paymentEventPort = paymentEventPort,
      stripeService = stripeService,
    )

    return ModuleApiRegistry.register(PaymentApi::class, object : PaymentApi {
      override suspend fun addPaymentMethod(command: AddPaymentMethodCommand) =
        addPaymentMethodUseCase.execute(command)

      override suspend fun makePayment(command: MakePaymentCommand) =
        makePaymentUseCase.execute(command)
    })
  }
}
