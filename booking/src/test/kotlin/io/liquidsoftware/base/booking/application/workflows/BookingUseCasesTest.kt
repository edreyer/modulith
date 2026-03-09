package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStartedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentError
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.DateInPastError
import io.liquidsoftware.base.booking.application.port.`in`.DateTimeUnavailableError
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderDtoIn
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
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
import java.time.LocalTime

class BookingUseCasesTest {

  @Test
  fun `get availability returns remaining times for a future date`() = runBlocking {
    val date = LocalDate.now().plusDays(1)
    val useCase = GetAvailabilityUseCase(
      findAppointmentPort = TestFindAppointmentPort(
        findAllForAvailabilityBlock = {
          Either.Right(listOf(buildScheduledAppointment(scheduledTime = LocalDateTime.of(date, LocalTime.of(9, 0)))))
        }
      ),
      availabilityService = AvailabilityService(),
    )

    val result = useCase.execute(GetAvailabilityQuery(date))

    val event = result.fold({ error("unexpected availability error: ${it.message}") }, { it })
    assertThat(event.times).doesNotContain(LocalTime.of(9, 0))
    assertThat(event.times).contains(LocalTime.of(10, 0))
    Unit
  }

  @Test
  fun `get availability rejects past dates`() = runBlocking {
    val useCase = GetAvailabilityUseCase(
      findAppointmentPort = TestFindAppointmentPort(),
      availabilityService = AvailabilityService(),
    )

    val result = useCase.execute(GetAvailabilityQuery(LocalDate.now().minusDays(1)))

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(DateInPastError::class)
    Unit
  }

  @Test
  fun `schedule appointment persists scheduled appointment when time is available`() = runBlocking {
    val appointmentEventPort = RecordingAppointmentEventPort()
    val useCase = ScheduleAppointmentUseCase(
      availabilityService = AvailabilityService(),
      findAppointmentPort = TestFindAppointmentPort(findAllForAvailabilityBlock = { Either.Right(emptyList()) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(
      ScheduleAppointmentCommand(
        userId = "u_test-user",
        scheduledTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
        duration = 30,
        workOrder = WorkOrderDtoIn(service = "Oil Change", notes = "Scheduled"),
      )
    )

    val event = result.fold({ error("unexpected scheduling error: ${it.message}") }, { it })
    assertThat(event.appointmentDto.status).isEqualTo(AppointmentStatus.SCHEDULED)
    assertThat(event.appointmentDto.workOrderDto.notes).isEqualTo("Scheduled")
    assertThat(appointmentEventPort.events).hasSize(1)
    Unit
  }

  @Test
  fun `schedule appointment returns unavailable when time is already booked`() = runBlocking {
    val scheduledTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)
    val useCase = ScheduleAppointmentUseCase(
      availabilityService = AvailabilityService(),
      findAppointmentPort = TestFindAppointmentPort(
        findAllForAvailabilityBlock = { Either.Right(listOf(buildScheduledAppointment(scheduledTime = scheduledTime))) }
      ),
      appointmentEventPort = RecordingAppointmentEventPort(),
    )

    val result = useCase.execute(
      ScheduleAppointmentCommand(
        userId = "u_test-user",
        scheduledTime = scheduledTime,
        duration = 30,
        workOrder = WorkOrderDtoIn(service = "Oil Change", notes = "Scheduled"),
      )
    )

    val error = result.fold({ it }, { error("expected unavailable error") })
    assertThat(error).isInstanceOf(DateTimeUnavailableError::class)
    Unit
  }

  @Test
  fun `schedule appointment maps invalid input to appointment validation error`() = runBlocking {
    val useCase = ScheduleAppointmentUseCase(
      availabilityService = AvailabilityService(),
      findAppointmentPort = TestFindAppointmentPort(findAllForAvailabilityBlock = { Either.Right(emptyList()) }),
      appointmentEventPort = RecordingAppointmentEventPort(),
    )

    val result = useCase.execute(
      ScheduleAppointmentCommand(
        userId = "u_test-user",
        scheduledTime = LocalDateTime.now().plusDays(1),
        duration = 0,
        workOrder = WorkOrderDtoIn(service = "", notes = null),
      )
    )

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(AppointmentValidationError::class)
    Unit
  }

  @Test
  fun `start appointment persists in progress appointment`() = runBlocking {
    val appointmentEventPort = RecordingAppointmentEventPort()
    val scheduled = buildScheduledAppointment()
    val useCase = StartAppointmentUseCase(
      findAppointmentPort = TestFindAppointmentPort(findScheduledByIdBlock = { Either.Right(scheduled) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(StartAppointmentCommand(scheduled.id.value))

    val event = result.fold({ error("unexpected start error: ${it.message}") }, { it })
    assertThat(event.appointmentDto.status).isEqualTo(AppointmentStatus.IN_PROGRESS)
    assertThat(appointmentEventPort.events).hasSize(1)
    Unit
  }

  @Test
  fun `start appointment returns validation error when scheduled appointment is missing`() = runBlocking {
    val useCase = StartAppointmentUseCase(
      findAppointmentPort = TestFindAppointmentPort(findScheduledByIdBlock = { Either.Right(null) }),
      appointmentEventPort = RecordingAppointmentEventPort(),
    )

    val result = useCase.execute(StartAppointmentCommand("a_missing"))

    val error = result.fold({ it }, { error("expected validation error") })
    assertThat(error).isInstanceOf(AppointmentValidationError::class)
    Unit
  }

  @Test
  fun `complete appointment persists completed appointment`() = runBlocking {
    val appointmentEventPort = RecordingAppointmentEventPort()
    val inProgress = buildInProgressAppointment()
    val useCase = CompleteAppointmentUseCase(
      findAppointmentPort = TestFindAppointmentPort(findStartedByIdBlock = { Either.Right(inProgress) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(CompleteAppointmentCommand(inProgress.id.value, "Complete"))

    val event = result.fold({ error("unexpected complete error: ${it.message}") }, { it })
    assertThat(event.appointmentDto.status).isEqualTo(AppointmentStatus.COMPLETE)
    assertThat(event.appointmentDto.workOrderDto.notes).isEqualTo("Complete")
    assertThat(appointmentEventPort.events).hasSize(1)
    Unit
  }

  @Test
  fun `complete appointment returns not found when in progress appointment is missing`() = runBlocking {
    val useCase = CompleteAppointmentUseCase(
      findAppointmentPort = TestFindAppointmentPort(findStartedByIdBlock = { Either.Right(null) }),
      appointmentEventPort = RecordingAppointmentEventPort(),
    )

    val result = useCase.execute(CompleteAppointmentCommand("a_missing", "Complete"))

    val error = result.fold({ it }, { error("expected not found error") })
    assertThat(error).isInstanceOf(AppointmentNotFoundError::class)
    Unit
  }

  @Test
  fun `cancel appointment persists cancelled appointment`() = runBlocking {
    val appointmentEventPort = RecordingAppointmentEventPort()
    val scheduled = buildScheduledAppointment()
    val useCase = CancelAppointmentUseCase(
      appointmentStateService = AppointmentStateService(),
      findAppointmentPort = TestFindAppointmentPort(findByIdBlock = { Either.Right(scheduled) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(CancelAppointmentCommand(scheduled.id.value, "Cancelled"))

    val event = result.fold({ error("unexpected cancel error: ${it.message}") }, { it })
    assertThat(event.appointmentDto.status).isEqualTo(AppointmentStatus.CANCELLED)
    assertThat(appointmentEventPort.events).hasSize(1)
    Unit
  }

  @Test
  fun `cancel appointment returns error when appointment state cannot be cancelled`() = runBlocking {
    val completed = buildCompletedAppointment()
    val appointmentEventPort = RecordingAppointmentEventPort()
    val useCase = CancelAppointmentUseCase(
      appointmentStateService = AppointmentStateService(),
      findAppointmentPort = TestFindAppointmentPort(findByIdBlock = { Either.Right(completed) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(CancelAppointmentCommand(completed.id.value, "Cancelled"))

    val error = result.fold({ it }, { error("expected cancellation error") })
    assertThat(error).isInstanceOf(CancelAppointmentError::class)
    assertThat(appointmentEventPort.events).hasSize(0)
    Unit
  }

  @Test
  fun `fetch user appointments filters cancelled appointments`() = runBlocking {
    val scheduled = buildScheduledAppointment(apptId = "a_scheduled")
    val cancelled = CancelledAppointment.of(buildScheduledAppointment(apptId = "a_cancelled"))
    var pageable: Pageable? = null
    val useCase = FetchUserAppointmentsUseCase(
      findAppointmentPort = TestFindAppointmentPort(
        findByUserIdBlock = { _, requestedPageable ->
          pageable = requestedPageable
          Either.Right(listOf(scheduled, cancelled))
        }
      )
    )

    val result = useCase.execute(FetchUserAppointmentsQuery(userId = "u_test-user", page = 2, size = 5))

    val event = result.fold({ error("unexpected fetch error: ${it.message}") }, { it })
    assertThat(event.appointments).hasSize(1)
    assertThat(event.appointments[0].id).isEqualTo("a_scheduled")
    assertThat(pageable?.pageNumber).isEqualTo(2)
    assertThat(pageable?.pageSize).isEqualTo(5)
    Unit
  }

  @Test
  fun `pay appointment delegates to payment api and persists paid appointment`() = runBlocking {
    val completed = buildCompletedAppointment()
    val appointmentEventPort = RecordingAppointmentEventPort()
    val recordedCommands = mutableListOf<MakePaymentCommand>()
    val useCase = PayAppointmentUseCase(
      paymentApi = TestPaymentApi(
        makePaymentBlock = { command: MakePaymentCommand ->
          recordedCommands += command
          Either.Right(
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
      ),
      findAppointmentPort = TestFindAppointmentPort(findCompletedByIdBlock = { Either.Right(completed) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(PayAppointmentCommand(completed.id.value, "pm_test-method"))

    val event = result.fold({ error("unexpected payment error: ${it.message}") }, { it })
    assertThat(recordedCommands).isEqualTo(listOf(MakePaymentCommand(paymentMethodId = "pm_test-method", amount = 9000)))
    assertThat(event.appointmentDto.status).isEqualTo(AppointmentStatus.PAID)
    assertThat(event.appointmentDto.paymentId).isEqualTo("p_test-payment")
    assertThat(appointmentEventPort.events).hasSize(1)
    Unit
  }

  @Test
  fun `pay appointment returns not found when completed appointment is missing`() = runBlocking {
    val useCase = PayAppointmentUseCase(
      paymentApi = TestPaymentApi(),
      findAppointmentPort = TestFindAppointmentPort(findCompletedByIdBlock = { Either.Right(null) }),
      appointmentEventPort = RecordingAppointmentEventPort(),
    )

    val result = useCase.execute(PayAppointmentCommand("a_missing", "pm_test-method"))

    val error = result.fold({ it }, { error("expected not found error") })
    assertThat(error).isInstanceOf(AppointmentNotFoundError::class)
    Unit
  }

  @Test
  fun `pay appointment propagates payment decline without persisting appointment`() = runBlocking {
    val completed = buildCompletedAppointment()
    val appointmentEventPort = RecordingAppointmentEventPort()
    val useCase = PayAppointmentUseCase(
      paymentApi = TestPaymentApi(makePaymentBlock = { Either.Left(PaymentDeclinedError("Card declined")) }),
      findAppointmentPort = TestFindAppointmentPort(findCompletedByIdBlock = { Either.Right(completed) }),
      appointmentEventPort = appointmentEventPort,
    )

    val result = useCase.execute(PayAppointmentCommand(completed.id.value, "pm_test-method"))

    val error = result.fold({ it }, { error("expected payment decline") })
    assertThat(error).isInstanceOf(PaymentDeclinedError::class)
    assertThat(appointmentEventPort.events).hasSize(0)
    Unit
  }

  private class TestFindAppointmentPort(
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

  private class RecordingAppointmentEventPort(
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

  private class TestPaymentApi(
    private val makePaymentBlock: suspend (MakePaymentCommand) -> Either<WorkflowError, PaymentMadeEvent> = {
      error("unexpected makePayment")
    },
  ) : PaymentApi {
    override suspend fun addPaymentMethod(command: AddPaymentMethodCommand): Either<WorkflowError, PaymentMethodAddedEvent> =
      error("unexpected addPaymentMethod")

    override suspend fun makePayment(command: MakePaymentCommand): Either<WorkflowError, PaymentMadeEvent> =
      makePaymentBlock(command)
  }

  private fun buildScheduledAppointment(
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

  private fun buildInProgressAppointment(
    scheduledAppointment: ScheduledAppointment = buildScheduledAppointment(),
  ): InProgressAppointment = InProgressAppointment.of(scheduledAppointment)

  private fun buildCompletedAppointment(
    inProgressAppointment: InProgressAppointment = buildInProgressAppointment(),
    notes: String? = "Complete",
  ): CompleteAppointment = CompleteAppointment.of(inProgressAppointment, notes)
}
