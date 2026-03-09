package io.liquidsoftware.base.booking.adapter.out.module

import io.liquidsoftware.base.booking.application.port.`in`.AppointmentApi
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import io.liquidsoftware.common.context.ModuleApiRegistry

class LocalAppointmentApi : AppointmentApi {
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
