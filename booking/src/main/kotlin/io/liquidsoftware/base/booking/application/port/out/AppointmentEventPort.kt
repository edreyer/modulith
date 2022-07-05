package io.liquidsoftware.base.booking.application.port.out

import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent

interface AppointmentEventPort {
  suspend fun <T: AppointmentEvent> handle(event: T): T
}
