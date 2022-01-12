package io.liquidsoftware.base.booking.application.service

import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError.CancelAppointmentError
import org.springframework.stereotype.Service

@Service
internal class AppointmentStateService {

  suspend fun schedule(draft: io.liquidsoftware.base.booking.domain.DraftAppointment): io.liquidsoftware.base.booking.domain.ScheduledAppointment =
    io.liquidsoftware.base.booking.domain.ScheduledAppointment.of(draft)

  suspend fun cancel(appt: io.liquidsoftware.base.booking.domain.Appointment): io.liquidsoftware.base.booking.domain.CancelledAppointment = when (appt) {
    is io.liquidsoftware.base.booking.domain.DraftAppointment -> io.liquidsoftware.base.booking.domain.CancelledAppointment.of(appt)
    is io.liquidsoftware.base.booking.domain.ScheduledAppointment -> io.liquidsoftware.base.booking.domain.CancelledAppointment.of(appt)
    is io.liquidsoftware.base.booking.domain.InProgressAppointment -> throw CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} ")
    is io.liquidsoftware.base.booking.domain.CompleteAppointment -> throw CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} ")
    is io.liquidsoftware.base.booking.domain.CancelledAppointment -> throw CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} ")
  }

}
