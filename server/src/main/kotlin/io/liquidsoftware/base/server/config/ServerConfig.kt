package io.liquidsoftware.base.server.config

import io.liquidsoftware.base.booking.adapter.`in`.web.BookingWebConfig
import io.liquidsoftware.base.booking.config.BookingModuleAdapterConfig
import io.liquidsoftware.base.payment.adapter.`in`.web.PaymentWebConfig
import io.liquidsoftware.base.payment.config.PaymentModuleAdapterConfig
import io.liquidsoftware.base.user.adapter.`in`.web.UserWebConfig
import io.liquidsoftware.base.user.config.UserModuleAdapterConfig
import io.liquidsoftware.common.config.CommonConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
  CommonConfig::class,
  UserModuleAdapterConfig::class,
  UserWebConfig::class,
  BookingModuleAdapterConfig::class,
  BookingWebConfig::class,
  PaymentModuleAdapterConfig::class,
  PaymentWebConfig::class
)
class ServerConfig
