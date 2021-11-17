package ventures.dvx.base.server.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ventures.dvx.common.config.CommonConfig

@Configuration
@EntityScan("ventures.dvx.base") // seems to be required at this level
@Import(
  CommonConfig::class
)
class ServerConfig
