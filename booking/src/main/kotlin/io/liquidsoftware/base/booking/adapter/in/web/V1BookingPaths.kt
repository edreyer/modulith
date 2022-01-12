package io.liquidsoftware.base.booking.adapter.`in`.web

internal object V1BookingPaths {
  const val API = "/api"
  const val V1 = "${io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths.API}/v1"

  const val AVAILABILITY = "${io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths.V1}/availability/{date}"

  const val SCHEDULE_APPT = "${io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths.V1}/appointments/schedule"
  const val CANCEL_APPT = "${io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths.V1}/appointments/cancel/{appointmentId}"
}
