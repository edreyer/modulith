package ventures.dvx.base.user.config

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ventures.dvx.common.config.CommonConfig

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages = ["ventures.dvx.base.user"])
@ConfigurationPropertiesScan(basePackages = ["ventures.dvx.base.user"])
@Import(
  CommonConfig::class
)
class UserModuleConfig
