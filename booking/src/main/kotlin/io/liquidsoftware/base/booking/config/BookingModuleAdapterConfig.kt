package io.liquidsoftware.base.booking.config

import io.liquidsoftware.base.booking.adapter.out.module.LocalAppointmentApi
import io.liquidsoftware.base.booking.adapter.out.module.LocalAvailabilityApi
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentApi
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BookingModuleAdapterConfig {
  @Bean fun appointmentApi(): AppointmentApi = LocalAppointmentApi()

  @Bean fun availabilityApi(): AvailabilityApi = LocalAvailabilityApi()
}
