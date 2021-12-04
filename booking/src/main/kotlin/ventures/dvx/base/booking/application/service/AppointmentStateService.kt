package ventures.dvx.base.booking.application.service

import org.springframework.stereotype.Service
import ventures.dvx.base.booking.application.port.`in`.ScheduleAppointmentError.CancelAppointmentError
import ventures.dvx.base.booking.domain.Appointment
import ventures.dvx.base.booking.domain.CancelledAppointment
import ventures.dvx.base.booking.domain.CompleteAppointment
import ventures.dvx.base.booking.domain.DraftAppointment
import ventures.dvx.base.booking.domain.InProgressAppointment
import ventures.dvx.base.booking.domain.ScheduledAppointment

@Service
internal class AppointmentStateService {

  suspend fun schedule(draft: DraftAppointment): ScheduledAppointment =
    ScheduledAppointment.of(draft)

  suspend fun cancel(appt: Appointment): CancelledAppointment = when (appt) {
    is DraftAppointment -> CancelledAppointment.of(appt)
    is ScheduledAppointment -> CancelledAppointment.of(appt)
    is InProgressAppointment -> throw CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} ")
    is CompleteAppointment -> throw CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} ")
    is CancelledAppointment -> throw CancelAppointmentError("Cannot cancel Appt of type:  ${appt.javaClass.name} ")
  }

}
