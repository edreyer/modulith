package io.liquidsoftware.base.booking.adapter.`in`.web

internal object V1BookingPaths {
  const val API = "/api"
  const val V1 = "$API/v1"

  const val AVAILABILITY = "$V1/availability/{date}"

  const val SCHEDULE_APPT = "$V1/appointments/schedule"
  const val CANCEL_APPT = "$V1/appointments/cancel/{appointmentId}"
}
