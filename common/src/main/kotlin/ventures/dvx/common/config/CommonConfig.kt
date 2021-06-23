package ventures.dvx.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "common")
class CommonConfig(
  val mode: String
) {
  private val DEV_MODE = "dev"
  private val STAGING_MODE = "staging"
  private val PROD_MODE = "prod"

  fun isDev() = DEV_MODE == mode
  fun isStaging() = STAGING_MODE == mode
  fun isProd() = PROD_MODE == mode

}
