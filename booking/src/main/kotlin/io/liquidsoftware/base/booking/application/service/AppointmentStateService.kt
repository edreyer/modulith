package io.liquidsoftware.base.booking.application.service

import arrow.core.raise.Raise
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentError
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentError
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.PaidAppointment
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import org.springframework.stereotype.Service

@Service
internal class AppointmentStateService {

  context(Raise<AppointmentError>)
  suspend fun cancel(appt: Appointment): CancelledAppointment = when (appt) {
    is ScheduledAppointment -> CancelledAppointment.of(appt)
    is InProgressAppointment -> CancelledAppointment.of(appt)
    is CompleteAppointment -> raise(CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} "))
    is PaidAppointment -> raise(CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} "))
    is CancelledAppointment -> appt
  }

}
