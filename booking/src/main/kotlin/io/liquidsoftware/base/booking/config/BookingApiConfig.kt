package io.liquidsoftware.base.booking.config

import io.liquidsoftware.base.booking.adapter.out.module.LocalPaymentApi
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentApi
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityApi
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.base.booking.application.workflows.CancelAppointmentUseCase
import io.liquidsoftware.base.booking.application.workflows.CompleteAppointmentUseCase
import io.liquidsoftware.base.booking.application.workflows.FetchUserAppointmentsUseCase
import io.liquidsoftware.base.booking.application.workflows.GetAvailabilityUseCase
import io.liquidsoftware.base.booking.application.workflows.PayAppointmentUseCase
import io.liquidsoftware.base.booking.application.workflows.ScheduleAppointmentUseCase
import io.liquidsoftware.base.booking.application.workflows.StartAppointmentUseCase
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.common.context.ModuleApiRegistration
import io.liquidsoftware.common.context.ModuleApiRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BookingApiConfig {

  @Bean internal fun paymentApi(): PaymentApi = LocalPaymentApi()

  @Bean
  internal fun appointmentApiRegistration(
    paymentApi: PaymentApi,
    appointmentStateService: AppointmentStateService,
    findAppointmentPort: FindAppointmentPort,
    appointmentEventPort: AppointmentEventPort,
    availabilityService: AvailabilityService,
  ): ModuleApiRegistration<AppointmentApi> {
    val scheduleAppointmentUseCase = ScheduleAppointmentUseCase(
      availabilityService = availabilityService,
      findAppointmentPort = findAppointmentPort,
      appointmentEventPort = appointmentEventPort,
    )
    val startAppointmentUseCase = StartAppointmentUseCase(findAppointmentPort, appointmentEventPort)
    val completeAppointmentUseCase = CompleteAppointmentUseCase(findAppointmentPort, appointmentEventPort)
    val cancelAppointmentUseCase = CancelAppointmentUseCase(
      appointmentStateService = appointmentStateService,
      findAppointmentPort = findAppointmentPort,
      appointmentEventPort = appointmentEventPort,
    )
    val payAppointmentUseCase = PayAppointmentUseCase(
      paymentApi = paymentApi,
      findAppointmentPort = findAppointmentPort,
      appointmentEventPort = appointmentEventPort,
    )
    val fetchUserAppointmentsUseCase = FetchUserAppointmentsUseCase(findAppointmentPort)

    return ModuleApiRegistry.register(AppointmentApi::class, object : AppointmentApi {
      override suspend fun scheduleAppointment(command: ScheduleAppointmentCommand) =
        scheduleAppointmentUseCase.execute(command)

      override suspend fun startAppointment(command: StartAppointmentCommand) =
        startAppointmentUseCase.execute(command)

      override suspend fun completeAppointment(command: CompleteAppointmentCommand) =
        completeAppointmentUseCase.execute(command)

      override suspend fun cancelAppointment(command: CancelAppointmentCommand) =
        cancelAppointmentUseCase.execute(command)

      override suspend fun payAppointment(command: PayAppointmentCommand) =
        payAppointmentUseCase.execute(command)

      override suspend fun fetchUserAppointments(query: FetchUserAppointmentsQuery) =
        fetchUserAppointmentsUseCase.execute(query)
    })
  }

  @Bean
  internal fun availabilityApiRegistration(
    findAppointmentPort: FindAppointmentPort,
    availabilityService: AvailabilityService,
  ): ModuleApiRegistration<AvailabilityApi> {
    val getAvailabilityUseCase = GetAvailabilityUseCase(findAppointmentPort, availabilityService)

    return ModuleApiRegistry.register(AvailabilityApi::class, object : AvailabilityApi {
      override suspend fun getAvailability(query: GetAvailabilityQuery) =
        getAvailabilityUseCase.execute(query)
    })
  }
}
