package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime

internal class TestFindAppointmentPort(
  private val findByIdBlock: suspend (String) -> Either<WorkflowError, Appointment?> = {
    error("unexpected findById")
  },
  private val findScheduledByIdBlock: suspend (String) -> Either<WorkflowError, ScheduledAppointment?> = {
    error("unexpected findScheduledById")
  },
  private val findStartedByIdBlock: suspend (String) -> Either<WorkflowError, InProgressAppointment?> = {
    error("unexpected findStartedById")
  },
  private val findCompletedByIdBlock: suspend (String) -> Either<WorkflowError, CompleteAppointment?> = {
    error("unexpected findCompletedById")
  },
  private val findByUserIdBlock: suspend (String, Pageable) -> Either<WorkflowError, List<Appointment>> = { _, _ ->
    error("unexpected findByUserId")
  },
  private val findAllForAvailabilityBlock: suspend (LocalDate) -> Either<WorkflowError, List<Appointment>> = {
    error("unexpected findAllForAvailability")
  },
) : FindAppointmentPort {
  override suspend fun findById(apptId: String): Either<WorkflowError, Appointment?> = findByIdBlock(apptId)

  override suspend fun findScheduledById(apptId: String): Either<WorkflowError, ScheduledAppointment?> =
    findScheduledByIdBlock(apptId)

  override suspend fun findStartedById(apptId: String): Either<WorkflowError, InProgressAppointment?> =
    findStartedByIdBlock(apptId)

  override suspend fun findCompletedById(apptId: String): Either<WorkflowError, CompleteAppointment?> =
    findCompletedByIdBlock(apptId)

  override suspend fun findByUserId(userId: String, pageable: Pageable): Either<WorkflowError, List<Appointment>> =
    findByUserIdBlock(userId, pageable)

  override suspend fun findAllForAvailability(date: LocalDate): Either<WorkflowError, List<Appointment>> =
    findAllForAvailabilityBlock(date)
}

internal class RecordingAppointmentEventPort(
  private val handleEvent: suspend (AppointmentEvent) -> Either<WorkflowError, AppointmentEvent> = {
    Either.Right(it)
  },
) : AppointmentEventPort {
  val events = mutableListOf<AppointmentEvent>()

  override suspend fun <T : AppointmentEvent> handle(event: T): Either<WorkflowError, T> {
    events += event
    return handleEvent(event).fold(
      { Either.Left(it) },
      {
        @Suppress("UNCHECKED_CAST")
        Either.Right(it as T)
      },
    )
  }
}

internal class TestPaymentApi(
  private val makePaymentBlock: suspend (MakePaymentCommand) -> Either<WorkflowError, PaymentMadeEvent> = {
    error("unexpected makePayment")
  },
) : PaymentApi {
  override suspend fun addPaymentMethod(command: AddPaymentMethodCommand): Either<WorkflowError, PaymentMethodAddedEvent> =
    error("unexpected addPaymentMethod")

  override suspend fun makePayment(command: MakePaymentCommand): Either<WorkflowError, PaymentMadeEvent> =
    makePaymentBlock(command)
}

internal fun buildScheduledAppointment(
  apptId: String = "a_test-appointment",
  userId: String = "u_test-user",
  scheduledTime: LocalDateTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0),
  duration: Long = 30,
  service: String = "Oil Change",
  notes: String? = "Ready",
): ScheduledAppointment =
  either {
    val readyWorkOrder = ReadyWorkOrder.of(service = service, notes = notes)
    ScheduledAppointment.of(
      apptId = apptId,
      userId = userId,
      scheduledTime = scheduledTime,
      duration = duration,
      workOrder = readyWorkOrder,
    )
  }.fold({ error("invalid scheduled appointment test fixture") }, { it })

internal fun buildInProgressAppointment(
  scheduledAppointment: ScheduledAppointment = buildScheduledAppointment(),
): InProgressAppointment = InProgressAppointment.of(scheduledAppointment)

internal fun buildCompletedAppointment(
  inProgressAppointment: InProgressAppointment = buildInProgressAppointment(),
  notes: String? = "Complete",
): CompleteAppointment = CompleteAppointment.of(inProgressAppointment, notes)
