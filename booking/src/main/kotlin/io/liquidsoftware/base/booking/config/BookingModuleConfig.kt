package io.liquidsoftware.base.booking.config

import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.common.config.CommonConfig
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages = ["io.liquidsoftware.base.booking"])
@ConfigurationPropertiesScan(basePackages = ["io.liquidsoftware.base.booking"])
@Import(
  CommonConfig::class
)
class BookingModuleConfig {
  private final val logger by LoggerDelegate()

  init {
    logger.info("Starting Booking Module")
  }

  @Bean
  @ConditionalOnMissingBean(PaymentApi::class)
  fun bookingPaymentApi(dispatcher: WorkflowDispatcher): PaymentApi = object : PaymentApi {
    override suspend fun addPaymentMethod(command: AddPaymentMethodCommand) =
      dispatcher.dispatch<PaymentMethodAddedEvent>(command)

    override suspend fun makePayment(command: MakePaymentCommand) =
      dispatcher.dispatch<PaymentMadeEvent>(command)
  }
}
