package io.liquidsoftware.base.web.integration.booking

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.CompletedSuccessDto
import io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.ScheduleSuccessDto
import io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.StartedSuccessDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentIdDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaymentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderDtoIn
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoIn
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodDtoOut
import io.liquidsoftware.base.web.integration.user.BaseUserWebTest
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.time.LocalDateTime

@TestMethodOrder(OrderAnnotation::class)
class AppointmentTest : BaseUserWebTest() {

  private var appt: AppointmentDtoOut? = null

  @Test
  @Order(1)
  fun scheduleNewAppointment() {
    val apptDto = AppointmentDtoIn(
      duration = 30,
      scheduledTime = LocalDateTime.now(),
      workOrder = WorkOrderDtoIn(
        service = "Oil Change",
        notes = "Scheduled!"
      )
    )

    val outputDto = post("/api/v1/appointments/schedule", apptDto, accessToken)
      .then()
      .statusCode(200)
      .extract().`as`(ScheduleSuccessDto::class.java)
    
    appt = outputDto.appointment
    assertThat(appt?.status).isEqualTo(AppointmentStatus.SCHEDULED)
    assertThat(appt?.workOrderDto?.notes).isEqualTo("Scheduled!")
  }

  @Test
  @Order(2)
  fun ensureNotPossibleToDirectlyCompleteScheduledAppt() {
    val apptDto = AppointmentCompletedDtoIn(appt?.id!!)
    post("/api/v1/appointments/complete", apptDto, accessToken)
      .then()
      .statusCode(412)
  }

  @Test
  @Order(3)
  fun testInProgress() {
    val apptDto = AppointmentIdDtoIn(appt?.id!!)
    val outputDto = post("/api/v1/appointments/in-progress", apptDto, accessToken)
      .then()
      .statusCode(200)
      .extract().`as`(StartedSuccessDto::class.java)

    appt = outputDto.appointment
    assertThat(appt?.status).isEqualTo(AppointmentStatus.IN_PROGRESS)
  }

  @Test
  @Order(4)
  fun testComplete() {
    Thread.sleep(1000)
    val apptDto = AppointmentCompletedDtoIn(appt?.id!!, "Complete!")
    val outputDto = post("/api/v1/appointments/complete", apptDto, accessToken)
      .then()
      .statusCode(200)
      .extract().`as`(CompletedSuccessDto::class.java)

    appt = outputDto.appointment
    assertThat(appt?.status).isEqualTo(AppointmentStatus.COMPLETE)
    assertThat(appt?.workOrderDto?.notes).isEqualTo("Complete!")
  }

  @Test
  @Order(5)
  fun testPayment() {
    val pmIn = PaymentMethodDtoIn(appt?.userId!!, "ABCD", "1234")
    val pmOut = post("/api/v1/payment-methods", pmIn, accessToken)
      .then()
      .statusCode(200)
      .extract().`as`(PaymentMethodDtoOut::class.java)

    val apptDto = AppointmentPaymentDto(appt?.id!!, pmOut.paymentMethodId)
    val outputDto = post("/api/v1/appointments/pay", apptDto, accessToken)
      .then()
      .statusCode(200)
      .extract().`as`(AppointmentPaidEvent::class.java)

    appt = outputDto.appointmentDto
    assertThat(appt?.status).isEqualTo(AppointmentStatus.PAID)
    assertThat(appt?.workOrderDto?.notes).isEqualTo("Complete!")
  }

}
