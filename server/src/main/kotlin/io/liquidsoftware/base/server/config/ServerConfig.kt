package io.liquidsoftware.base.server.config

import io.liquidsoftware.base.booking.adapter.`in`.web.BookingWebConfig
import io.liquidsoftware.base.payment.adapter.`in`.web.PaymentWebConfig
import io.liquidsoftware.base.user.adapter.`in`.web.UserWebConfig
import io.liquidsoftware.common.config.CommonConfig
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EntityScan("io.liquidsoftware.base") // seems to be required at this level
@Import(
  CommonConfig::class,
  UserWebConfig::class,
  BookingWebConfig::class,
  PaymentWebConfig::class
)
class ServerConfig
