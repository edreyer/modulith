package io.liquidsoftware.common.config

import io.liquidsoftware.common.logging.LoggerDelegate
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "common")
class CommonConfigurationProperties(
  private val mode: String
) {

  val log by LoggerDelegate()

  init {
    log.info("Application mode: $mode")
  }
  private val DEV_MODE = "dev"
  private val STAGING_MODE = "staging"
  private val PROD_MODE = "prod"

  fun isDev() = DEV_MODE == mode
  fun isStaging() = STAGING_MODE == mode
  fun isProd() = PROD_MODE == mode

}
