package io.liquidsoftware.base.booking.application.port.`in`

import io.liquidsoftware.base.user.UserId
import java.time.LocalDateTime

sealed class AppointmentDto {
  abstract val id: String

  data class DraftAppointmentDto(
    override val id: String = UserId.create().value,
    val userId: String,
    val startTime: LocalDateTime,
    val duration: Long
  ) : io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto()
  data class ScheduledAppointmentDto(private val data: io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData) : io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto(), io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoFields by data
  data class InProgressAppointmentDto(private val data: io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData) : io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto(), io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoFields by data
  data class CompleteAppointmentDto(private val data: io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData, val completeDate: LocalDateTime)
    : io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto(), io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoFields by data
  data class CancelledAppointmentDto(private val data: io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData, val cancelDate: LocalDateTime)
    : io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto(), io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoFields by data

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
  ) : io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoFields

}
