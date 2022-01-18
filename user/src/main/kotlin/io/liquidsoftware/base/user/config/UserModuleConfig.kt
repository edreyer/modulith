package io.liquidsoftware.base.user.config

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
@ComponentScan(basePackages = ["io.liquidsoftware.base.user"])
@ConfigurationPropertiesScan(basePackages = ["io.liquidsoftware.base.user"])
@Import(
  CommonConfig::class
)
class UserModuleConfig
