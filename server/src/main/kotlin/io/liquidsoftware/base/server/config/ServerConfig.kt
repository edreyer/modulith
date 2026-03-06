package io.liquidsoftware.base.server.config

import io.liquidsoftware.base.booking.adapter.`in`.web.BookingWebConfig
import io.liquidsoftware.base.payment.adapter.`in`.web.PaymentWebConfig
import io.liquidsoftware.base.user.adapter.`in`.web.UserWebConfig
import io.liquidsoftware.common.config.CommonConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
  CommonConfig::class,
  UserWebConfig::class,
  BookingWebConfig::class,
  PaymentWebConfig::class
)
class ServerConfig
