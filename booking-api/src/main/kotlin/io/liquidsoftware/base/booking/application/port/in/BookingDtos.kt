package io.liquidsoftware.base.booking.application.port.`in`

import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.types.getOrThrow
import java.time.LocalDateTime

sealed class AppointmentDto {
  abstract val id: String

  data class DraftAppointmentDto(
    override val id: String = UserId.create().getOrThrow().value,
    val userId: String,
    val startTime: LocalDateTime,
    val duration: Long
  ) : AppointmentDto()
  data class ScheduledAppointmentDto(private val data: AppointmentDtoData) : AppointmentDto(), AppointmentDtoFields by data
  data class InProgressAppointmentDto(private val data: AppointmentDtoData) : AppointmentDto(), AppointmentDtoFields by data
  data class CompleteAppointmentDto(private val data: AppointmentDtoData, val completeDate: LocalDateTime)
    : AppointmentDto(), AppointmentDtoFields by data
  data class CancelledAppointmentDto(private val data: AppointmentDtoData, val cancelDate: LocalDateTime)
    : AppointmentDto(), AppointmentDtoFields by data

  // Delegates to reduce boilerplate
  internal interface AppointmentDtoFields {
    val id: String
    val userId: String
    val startTime: LocalDateTime
    val duration: Long
  }

  // Delegates to reduce boilerplate
  data class AppointmentDtoData(
    override val id: String,
    override val userId: String,
    override val startTime: LocalDateTime,
    override val duration: Long
  ) : AppointmentDtoFields

}
