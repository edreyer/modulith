package io.liquidsoftware.base.booking.adapter.`in`.web

internal object V1BookingPaths {
  private const val API = "/api"
  private const val V1 = "$API/v1"

  const val AVAILABILITY = "$V1/availability/{date}"

  const val SCHEDULE_APPT = "$V1/appointments/schedule"
  const val IN_PROGRESS_APPT = "$V1/appointments/in-progress"
  const val COMPLETE_APPT = "$V1/appointments/complete"
  const val CANCEL_APPT = "$V1/appointments/cancel"
  const val PAY_APPT = "$V1/appointments/pay"

  const val GET_USER_APPTS = "$V1/appointments"
}
