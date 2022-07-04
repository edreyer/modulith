package io.liquidsoftware.base.payment.config

import io.liquidsoftware.common.config.CommonConfig
import io.liquidsoftware.common.logging.LoggerDelegate
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages = ["io.liquidsoftware.base.payment"])
@ConfigurationPropertiesScan(basePackages = ["io.liquidsoftware.base.payment"])
@Import(
  CommonConfig::class
)
class PaymentModuleConfig {
  private final val logger by LoggerDelegate()

  init {
    logger.info("Starting Payment Module")
  }

}
