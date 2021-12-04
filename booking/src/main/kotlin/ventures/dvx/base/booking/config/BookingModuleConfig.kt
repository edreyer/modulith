package ventures.dvx.base.booking.config

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ventures.dvx.common.config.CommonConfig
import ventures.dvx.common.logging.LoggerDelegate

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages = ["ventures.dvx.base.booking"])
@ConfigurationPropertiesScan(basePackages = ["ventures.dvx.base.booking"])
@Import(
  CommonConfig::class
)
class BookingModuleConfig {
  val logger by LoggerDelegate()

  init {
    logger.info("Starting Booking Module")
  }

}
