package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDtoOut
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime

class PayAppointmentWorkflowTest {

  @Test
  fun `delegates payment through payment api and persists paid appointment`() {
    runBlocking {
      val completedAppointment = buildCompletedAppointment()
      val recordedCommands = mutableListOf<MakePaymentCommand>()
      var persistedEvent: AppointmentPaidEvent? = null

      val workflow = PayAppointmentWorkflow(
        paymentApi = object : PaymentApi {
          override suspend fun addPaymentMethod(command: io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand):
            Either<WorkflowError, PaymentMethodAddedEvent> = error("unexpected addPaymentMethod")

          override suspend fun makePayment(command: MakePaymentCommand): Either<WorkflowError, PaymentMadeEvent> {
            recordedCommands += command
            return Either.Right(
              PaymentMadeEvent(
                PaymentDtoOut(
                  paymentId = "p_test-payment",
                  paymentMethodId = command.paymentMethodId,
                  userId = "u_test-user",
                  amount = command.amount,
                )
              )
            )
          }
        },
        findAppointmentPort = object : FindAppointmentPort {
          override suspend fun findById(apptId: String): Either<WorkflowError, Appointment?> =
            error("unexpected findById")

          override suspend fun findScheduledById(apptId: String): Either<WorkflowError, ScheduledAppointment?> =
            error("unexpected findScheduledById")

          override suspend fun findStartedById(apptId: String): Either<WorkflowError, InProgressAppointment?> =
            error("unexpected findStartedById")

          override suspend fun findCompletedById(apptId: String): Either<WorkflowError, CompleteAppointment?> =
            Either.Right(completedAppointment.takeIf { it.id.value == apptId })

          override suspend fun findByUserId(
            userId: String,
            pageable: Pageable,
          ): Either<WorkflowError, List<Appointment>> = error("unexpected findByUserId")

          override suspend fun findAllForAvailability(date: LocalDate): Either<WorkflowError, List<Appointment>> =
            error("unexpected findAllForAvailability")
        },
        appointmentEventPort = object : AppointmentEventPort {
          override suspend fun <T : AppointmentEvent> handle(event: T): Either<WorkflowError, T> {
            persistedEvent = event as AppointmentPaidEvent
            @Suppress("UNCHECKED_CAST")
            return Either.Right(event as T)
          }
        },
      )

      val result = either {
        workflow.invoke(PayAppointmentCommand(completedAppointment.id.value, "pm_test-method"))
      }.fold({ error("unexpected workflow error: ${it.message}") }, { it })

      assertThat(recordedCommands).isEqualTo(
        listOf(MakePaymentCommand(paymentMethodId = "pm_test-method", amount = 9000))
      )
      assertThat(result.appointmentDto.status.name).isEqualTo("PAID")
      assertThat(result.appointmentDto.paymentId).isEqualTo("p_test-payment")
      assertThat(persistedEvent?.appointmentDto?.paymentId).isEqualTo("p_test-payment")
    }
  }

  @Test
  fun `returns not found when completed appointment does not exist`() {
    runBlocking {
      val workflow = PayAppointmentWorkflow(
        paymentApi = failingPaymentApi(),
        findAppointmentPort = object : FindAppointmentPort {
          override suspend fun findById(apptId: String): Either<WorkflowError, Appointment?> =
            error("unexpected findById")

          override suspend fun findScheduledById(apptId: String): Either<WorkflowError, ScheduledAppointment?> =
            error("unexpected findScheduledById")

          override suspend fun findStartedById(apptId: String): Either<WorkflowError, InProgressAppointment?> =
            error("unexpected findStartedById")

          override suspend fun findCompletedById(apptId: String): Either<WorkflowError, CompleteAppointment?> =
            Either.Right(null)

          override suspend fun findByUserId(
            userId: String,
            pageable: Pageable,
          ): Either<WorkflowError, List<Appointment>> = error("unexpected findByUserId")

          override suspend fun findAllForAvailability(date: LocalDate): Either<WorkflowError, List<Appointment>> =
            error("unexpected findAllForAvailability")
        },
        appointmentEventPort = object : AppointmentEventPort {
          override suspend fun <T : AppointmentEvent> handle(event: T): Either<WorkflowError, T> =
            error("unexpected persistence")
        },
      )

      val result = either {
        workflow.invoke(PayAppointmentCommand("a_missing", "pm_test-method"))
      }

      val error = result.fold({ it }, { error("expected not found error") })
      assertThat(error).isInstanceOf(AppointmentNotFoundError::class)
    }
  }

  @Test
  fun `propagates payment api failure without persisting appointment`() {
    runBlocking {
      val completedAppointment = buildCompletedAppointment()
      var persisted = false

      val workflow = PayAppointmentWorkflow(
        paymentApi = object : PaymentApi {
          override suspend fun addPaymentMethod(command: io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand):
            Either<WorkflowError, PaymentMethodAddedEvent> = error("unexpected addPaymentMethod")

          override suspend fun makePayment(command: MakePaymentCommand): Either<WorkflowError, PaymentMadeEvent> =
            Either.Left(PaymentDeclinedError("Card declined"))
        },
        findAppointmentPort = object : FindAppointmentPort {
          override suspend fun findById(apptId: String): Either<WorkflowError, Appointment?> =
            error("unexpected findById")

          override suspend fun findScheduledById(apptId: String): Either<WorkflowError, ScheduledAppointment?> =
            error("unexpected findScheduledById")

          override suspend fun findStartedById(apptId: String): Either<WorkflowError, InProgressAppointment?> =
            error("unexpected findStartedById")

          override suspend fun findCompletedById(apptId: String): Either<WorkflowError, CompleteAppointment?> =
            Either.Right(completedAppointment)

          override suspend fun findByUserId(
            userId: String,
            pageable: Pageable,
          ): Either<WorkflowError, List<Appointment>> = error("unexpected findByUserId")

          override suspend fun findAllForAvailability(date: LocalDate): Either<WorkflowError, List<Appointment>> =
            error("unexpected findAllForAvailability")
        },
        appointmentEventPort = object : AppointmentEventPort {
          override suspend fun <T : AppointmentEvent> handle(event: T): Either<WorkflowError, T> {
            persisted = true
            return Either.Right(event)
          }
        },
      )

      val result = either {
        workflow.invoke(PayAppointmentCommand(completedAppointment.id.value, "pm_test-method"))
      }

      val error = result.fold({ it }, { error("expected payment failure") })
      assertThat(error).isInstanceOf(PaymentDeclinedError::class)
      assertThat(persisted).isEqualTo(false)
    }
  }

  private fun buildCompletedAppointment(): CompleteAppointment =
    either {
      val readyWorkOrder = ReadyWorkOrder.of(service = "Oil Change", notes = "Ready")
      val scheduledAppointment = ScheduledAppointment.of(
        apptId = "a_test-appointment",
        userId = "u_test-user",
        scheduledTime = LocalDateTime.now().plusHours(2),
        duration = 30,
        workOrder = readyWorkOrder,
      )
      val inProgressAppointment = InProgressAppointment.of(scheduledAppointment)
      CompleteAppointment.of(inProgressAppointment, notes = "Completed")
    }.fold({ error("invalid test appointment") }, { it })

  private fun failingPaymentApi(): PaymentApi = object : PaymentApi {
    override suspend fun addPaymentMethod(command: io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand):
      Either<WorkflowError, PaymentMethodAddedEvent> = error("unexpected addPaymentMethod")

    override suspend fun makePayment(command: MakePaymentCommand): Either<WorkflowError, PaymentMadeEvent> =
      error("unexpected payment call")
  }
}
