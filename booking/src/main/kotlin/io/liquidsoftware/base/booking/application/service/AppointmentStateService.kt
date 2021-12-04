package io.liquidsoftware.base.booking.application.service

import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError.CancelAppointmentError
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.DraftAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import org.springframework.stereotype.Service

@Service
internal class AppointmentStateService {

  suspend fun schedule(draft: DraftAppointment): ScheduledAppointment = ScheduledAppointment.of(draft)

  suspend fun cancel(appt: Appointment): CancelledAppointment = when (appt) {
    is DraftAppointment -> CancelledAppointment.of(appt)
    is ScheduledAppointment -> CancelledAppointment.of(appt)
    is InProgressAppointment -> throw CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} ")
    is CompleteAppointment -> throw CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} ")
    is CancelledAppointment -> throw CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} ")
  }

}
