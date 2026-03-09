package io.liquidsoftware.base.booking.adapter.`in`.web

import io.liquidsoftware.base.booking.application.port.`in`.AppointmentApi
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStartedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityApi
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityRetrievedEvent
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.UserAppointmentsFetchedEvent
import io.liquidsoftware.common.context.ModuleApiRegistry
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["io.liquidsoftware.base.booking.adapter.in.web"])
class BookingWebConfig {

  @Bean
  fun appointmentApi(): AppointmentApi = object : AppointmentApi {
    override suspend fun scheduleAppointment(command: ScheduleAppointmentCommand) =
      ModuleApiRegistry.require(AppointmentApi::class).scheduleAppointment(command)

    override suspend fun startAppointment(command: StartAppointmentCommand) =
      ModuleApiRegistry.require(AppointmentApi::class).startAppointment(command)

    override suspend fun completeAppointment(command: CompleteAppointmentCommand) =
      ModuleApiRegistry.require(AppointmentApi::class).completeAppointment(command)

    override suspend fun cancelAppointment(command: CancelAppointmentCommand) =
      ModuleApiRegistry.require(AppointmentApi::class).cancelAppointment(command)

    override suspend fun payAppointment(command: PayAppointmentCommand) =
      ModuleApiRegistry.require(AppointmentApi::class).payAppointment(command)

    override suspend fun fetchUserAppointments(query: FetchUserAppointmentsQuery) =
      ModuleApiRegistry.require(AppointmentApi::class).fetchUserAppointments(query)
  }

  @Bean
  fun availabilityApi(): AvailabilityApi = object : AvailabilityApi {
    override suspend fun getAvailability(query: GetAvailabilityQuery) =
      ModuleApiRegistry.require(AvailabilityApi::class).getAvailability(query)
  }
}
