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
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean

@Configuration
@ComponentScan(basePackages = ["io.liquidsoftware.base.booking.adapter.in.web"])
class BookingWebConfig {

  @Bean
  fun appointmentApi(dispatcher: WorkflowDispatcher): AppointmentApi = object : AppointmentApi {
    override suspend fun scheduleAppointment(command: ScheduleAppointmentCommand) =
      dispatcher.dispatch<AppointmentScheduledEvent>(command)

    override suspend fun startAppointment(command: StartAppointmentCommand) =
      dispatcher.dispatch<AppointmentStartedEvent>(command)

    override suspend fun completeAppointment(command: CompleteAppointmentCommand) =
      dispatcher.dispatch<AppointmentCompletedEvent>(command)

    override suspend fun cancelAppointment(command: CancelAppointmentCommand) =
      dispatcher.dispatch<AppointmentCancelledEvent>(command)

    override suspend fun payAppointment(command: PayAppointmentCommand) =
      dispatcher.dispatch<AppointmentPaidEvent>(command)

    override suspend fun fetchUserAppointments(query: FetchUserAppointmentsQuery) =
      dispatcher.dispatch<UserAppointmentsFetchedEvent>(query)
  }

  @Bean
  fun availabilityApi(dispatcher: WorkflowDispatcher): AvailabilityApi = object : AvailabilityApi {
    override suspend fun getAvailability(query: GetAvailabilityQuery) =
      dispatcher.dispatch<AvailabilityRetrievedEvent>(query)
  }
}
