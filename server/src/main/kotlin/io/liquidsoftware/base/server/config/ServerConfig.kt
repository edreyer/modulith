package io.liquidsoftware.base.server.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import io.liquidsoftware.base.user.adapter.`in`.web.UserWebConfig
import io.liquidsoftware.common.config.CommonConfig

@Configuration
@EntityScan("io.liquidsoftware.base") // seems to be required at this level
@Import(
  CommonConfig::class,
  UserWebConfig::class
)
class ServerConfig
