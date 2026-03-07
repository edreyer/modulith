package io.liquidsoftware.base.web.integration.booking

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.AvailabileTimesDto
import io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.ScheduleSuccessDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderDtoIn
import io.liquidsoftware.base.test.BaseWebTest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AvailabilityTest : BaseWebTest() {

  @Test
  fun `availability and scheduling work even when another user already has an appointment that day`() {
    val date = LocalDate.now().plusDays(1)
    val firstTime = LocalDateTime.of(date, LocalTime.of(9, 0))
    val secondTime = LocalDateTime.of(date, LocalTime.of(10, 0))

    val firstUserToken = authorizeUser("availability-owner@liquidsoftware.io", "5125553301").accessToken
    val secondUserToken = authorizeUser("availability-other@liquidsoftware.io", "5125553302").accessToken

    val firstAppointment = post(
      "/api/v1/appointments/schedule",
      AppointmentDtoIn(
        duration = 30,
        scheduledTime = firstTime,
        workOrder = WorkOrderDtoIn(service = "Oil Change", notes = "first")
      ),
      firstUserToken
    )
      .then()
      .statusCode(200)
      .extract()
      .`as`(ScheduleSuccessDto::class.java)
      .appointment

    val availability = get("/api/v1/availability/$date", secondUserToken)
      .then()
      .statusCode(200)
      .extract()
      .`as`(AvailabileTimesDto::class.java)

    assertThat(availability.times).doesNotContain(LocalTime.of(9, 0))
    assertThat(availability.times).contains(LocalTime.of(10, 0))

    val secondAppointment = post(
      "/api/v1/appointments/schedule",
      AppointmentDtoIn(
        duration = 30,
        scheduledTime = secondTime,
        workOrder = WorkOrderDtoIn(service = "Tire Rotation", notes = "second")
      ),
      secondUserToken
    )
      .then()
      .statusCode(200)
      .extract()
      .`as`(ScheduleSuccessDto::class.java)
      .appointment

    assertThat(secondAppointment.scheduledTime).isEqualTo(secondTime)
    assertThat(secondAppointment.userId).isNotEqualTo(firstAppointment.userId)
  }
}
