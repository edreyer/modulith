package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStartedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityRetrievedEvent
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
import io.liquidsoftware.base.booking.application.port.`in`.UserAppointmentsFetchedEvent
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderDtoIn
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.PaidAppointment
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.common.types.toErrString
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.legacy.executeLegacyProjected
import io.liquidsoftware.common.usecase.legacy.toUseCaseEither
import io.liquidsoftware.common.usecase.legacy.toUseCaseError
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowValidationError
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal class GetAvailabilityUseCase(
  private val findAppointmentPort: FindAppointmentPort,
  private val availabilityService: AvailabilityService,
) {

  private val useCase = useCase<GetAvailabilityRequest> {
    startWith { request -> Either.Right(AvailabilityRequestState(request.date)) }
    then(ValidateAvailabilityDateStep("validate-availability-date"))
    then(LoadAppointmentsForAvailabilityStep("load-appointments-for-availability", findAppointmentPort))
    then(BuildAvailabilityStep("build-availability", availabilityService))
  }

  suspend fun execute(query: GetAvailabilityQuery): Either<LegacyWorkflowError, AvailabilityRetrievedEvent> =
    useCase.executeLegacyProjected(
      request = query,
      requestMapper = { GetAvailabilityRequest(it.date) },
      projector = { result ->
        result.requireState<AvailabilityRetrievedState>("build-availability").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
      domainErrorMapper = ::mapBookingDomainError,
    )

  private class ValidateAvailabilityDateStep(
    override val id: String,
  ) : UseCaseWorkflow<AvailabilityRequestState, AvailabilityRequestState>() {

    override suspend fun executeWorkflow(
      input: AvailabilityRequestState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AvailabilityRequestState>> =
      if (input.date.isAfter(LocalDate.now())) {
        Either.Right(WorkflowResult(state = input, context = context))
      } else {
        Either.Left(UseCaseError.DomainError(DATE_IN_PAST_CODE, "${input.date} is in the past"))
      }
  }

  private class LoadAppointmentsForAvailabilityStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<AvailabilityRequestState, AvailabilityAppointmentsState>() {

    override suspend fun executeWorkflow(
      input: AvailabilityRequestState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AvailabilityAppointmentsState>> =
      findAppointmentPort.findAllForAvailability(input.date)
        .toUseCaseEither()
        .map { appointments ->
          WorkflowResult(
            state = AvailabilityAppointmentsState(input.date, appointments),
            context = context,
          )
        }
  }

  private class BuildAvailabilityStep(
    override val id: String,
    private val availabilityService: AvailabilityService,
  ) : UseCaseWorkflow<AvailabilityAppointmentsState, AvailabilityRetrievedState>() {

    override suspend fun executeWorkflow(
      input: AvailabilityAppointmentsState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AvailabilityRetrievedState>> =
      Either.Right(
        WorkflowResult(
          state = AvailabilityRetrievedState(
            AvailabilityRetrievedEvent(availabilityService.getAvailability(input.appointments))
          ),
          context = context,
        )
      )
  }
}

internal class ScheduleAppointmentUseCase(
  private val availabilityService: AvailabilityService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) {

  private val useCase = useCase<ScheduleAppointmentRequest> {
    startWith { request ->
      Either.Right(
        ScheduleAppointmentState(
          userId = request.userId,
          scheduledTime = request.scheduledTime,
          duration = request.duration,
          workOrder = request.workOrder,
        )
      )
    }
    then(LoadAppointmentsForScheduleStep("load-appointments-for-availability", findAppointmentPort))
    then(EnsureRequestedTimeAvailableStep("ensure-requested-time-available", availabilityService))
    then(BuildScheduledAppointmentStep("build-scheduled-appointment"))
    then(PersistAppointmentScheduledStep("persist-appointment-scheduled", appointmentEventPort))
  }

  suspend fun execute(command: ScheduleAppointmentCommand): Either<LegacyWorkflowError, AppointmentScheduledEvent> =
    useCase.executeLegacyProjected(
      request = command,
      requestMapper = {
        ScheduleAppointmentRequest(
          userId = it.userId,
          scheduledTime = it.scheduledTime,
          duration = it.duration,
          workOrder = it.workOrder,
        )
      },
      projector = { result ->
        result.requireState<AppointmentScheduledPersistedState>("persist-appointment-scheduled").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
      domainErrorMapper = ::mapBookingDomainError,
    )

  private class LoadAppointmentsForScheduleStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<ScheduleAppointmentState, ScheduleAppointmentAvailabilityState>() {

    override suspend fun executeWorkflow(
      input: ScheduleAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<ScheduleAppointmentAvailabilityState>> =
      findAppointmentPort.findAllForAvailability(input.scheduledTime.toLocalDate())
        .toUseCaseEither()
        .map { appointments ->
          WorkflowResult(
            state = ScheduleAppointmentAvailabilityState(
              userId = input.userId,
              scheduledTime = input.scheduledTime,
              duration = input.duration,
              workOrder = input.workOrder,
              appointmentsForDay = appointments,
            ),
            context = context,
          )
        }
  }

  private class EnsureRequestedTimeAvailableStep(
    override val id: String,
    private val availabilityService: AvailabilityService,
  ) : UseCaseWorkflow<ScheduleAppointmentAvailabilityState, ScheduleAppointmentAvailabilityState>() {

    override suspend fun executeWorkflow(
      input: ScheduleAppointmentAvailabilityState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<ScheduleAppointmentAvailabilityState>> =
      if (availabilityService.isTimeAvailable(input.appointmentsForDay, input.scheduledTime.toLocalTime())) {
        Either.Right(WorkflowResult(state = input, context = context))
      } else {
        Either.Left(
          UseCaseError.DomainError(
            DATE_TIME_UNAVAILABLE_CODE,
            "'${input.scheduledTime.toLocalTime()}' is no longer available.",
          )
        )
      }
  }

  private class BuildScheduledAppointmentStep(
    override val id: String,
  ) : UseCaseWorkflow<ScheduleAppointmentAvailabilityState, ScheduledAppointmentState>() {

    override suspend fun executeWorkflow(
      input: ScheduleAppointmentAvailabilityState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<ScheduledAppointmentState>> =
      either {
        val workOrder = ReadyWorkOrder.of(
          service = input.workOrder.service,
          notes = input.workOrder.notes,
        )
        ScheduledAppointment.of(
          userId = input.userId,
          scheduledTime = input.scheduledTime,
          duration = input.duration,
          workOrder = workOrder,
        )
      }.fold(
        {
          Either.Left(UseCaseError.DomainError(APPOINTMENT_VALIDATION_CODE, it.toErrString()))
        },
        { appointment ->
          Either.Right(WorkflowResult(state = ScheduledAppointmentState(appointment), context = context))
        },
      )
  }

  private class PersistAppointmentScheduledStep(
    override val id: String,
    private val appointmentEventPort: AppointmentEventPort,
  ) : UseCaseWorkflow<ScheduledAppointmentState, AppointmentScheduledPersistedState>() {

    override suspend fun executeWorkflow(
      input: ScheduledAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AppointmentScheduledPersistedState>> =
      appointmentEventPort.handle(AppointmentScheduledEvent(input.appointment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = AppointmentScheduledPersistedState(event), context = context) }
  }
}

internal class StartAppointmentUseCase(
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) {

  private val useCase = useCase<AppointmentIdRequest> {
    startWith { request -> Either.Right(AppointmentIdState(request.appointmentId)) }
    then(LoadScheduledAppointmentStep("load-scheduled-appointment", findAppointmentPort))
    then(BuildInProgressAppointmentStep("build-in-progress-appointment"))
    then(PersistAppointmentStartedStep("persist-appointment-started", appointmentEventPort))
  }

  suspend fun execute(command: StartAppointmentCommand): Either<LegacyWorkflowError, AppointmentStartedEvent> =
    useCase.executeLegacyProjected(
      request = command,
      requestMapper = { AppointmentIdRequest(it.appointmentId) },
      projector = { result ->
        result.requireState<AppointmentStartedPersistedState>("persist-appointment-started").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
      domainErrorMapper = ::mapBookingDomainError,
    )

  private class LoadScheduledAppointmentStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<AppointmentIdState, LoadedScheduledAppointmentState>() {

    override suspend fun executeWorkflow(
      input: AppointmentIdState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedScheduledAppointmentState>> =
      findAppointmentPort.findScheduledById(input.appointmentId)
        .toUseCaseEither()
        .flatMap { appointment ->
          appointment
            ?.let { Either.Right(WorkflowResult(state = LoadedScheduledAppointmentState(it), context = context)) }
            ?: Either.Left(
              UseCaseError.DomainError(
                APPOINTMENT_VALIDATION_CODE,
                "Could not find ready Appointment to start",
              )
            )
        }
  }

  private class BuildInProgressAppointmentStep(
    override val id: String,
  ) : UseCaseWorkflow<LoadedScheduledAppointmentState, InProgressAppointmentState>() {

    override suspend fun executeWorkflow(
      input: LoadedScheduledAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<InProgressAppointmentState>> =
      Either.Right(
        WorkflowResult(
          state = InProgressAppointmentState(InProgressAppointment.of(input.appointment)),
          context = context,
        )
      )
  }

  private class PersistAppointmentStartedStep(
    override val id: String,
    private val appointmentEventPort: AppointmentEventPort,
  ) : UseCaseWorkflow<InProgressAppointmentState, AppointmentStartedPersistedState>() {

    override suspend fun executeWorkflow(
      input: InProgressAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AppointmentStartedPersistedState>> =
      appointmentEventPort.handle(AppointmentStartedEvent(input.appointment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = AppointmentStartedPersistedState(event), context = context) }
  }
}

internal class CompleteAppointmentUseCase(
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) {

  private val useCase = useCase<CompleteAppointmentRequest> {
    startWith { request ->
      Either.Right(CompleteAppointmentState(request.appointmentId, request.notes))
    }
    then(LoadInProgressAppointmentStep("load-in-progress-appointment", findAppointmentPort))
    then(BuildCompletedAppointmentStep("build-completed-appointment"))
    then(PersistAppointmentCompletedStep("persist-appointment-completed", appointmentEventPort))
  }

  suspend fun execute(command: CompleteAppointmentCommand): Either<LegacyWorkflowError, AppointmentCompletedEvent> =
    useCase.executeLegacyProjected(
      request = command,
      requestMapper = { CompleteAppointmentRequest(it.appointmentId, it.notes) },
      projector = { result ->
        result.requireState<AppointmentCompletedPersistedState>("persist-appointment-completed").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
      domainErrorMapper = ::mapBookingDomainError,
    )

  private class LoadInProgressAppointmentStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<CompleteAppointmentState, LoadedInProgressAppointmentState>() {

    override suspend fun executeWorkflow(
      input: CompleteAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedInProgressAppointmentState>> =
      findAppointmentPort.findStartedById(input.appointmentId)
        .toUseCaseEither()
        .flatMap { appointment ->
          appointment
            ?.let {
              Either.Right(
                WorkflowResult(
                  state = LoadedInProgressAppointmentState(appointment = it, notes = input.notes),
                  context = context,
                )
              )
            }
            ?: Either.Left(
              UseCaseError.DomainError(
                APPOINTMENT_NOT_FOUND_CODE,
                "Appointment Not Found. id=${input.appointmentId}, status=IN_PROGRESS",
              )
            )
        }
  }

  private class BuildCompletedAppointmentStep(
    override val id: String,
  ) : UseCaseWorkflow<LoadedInProgressAppointmentState, CompletedAppointmentState>() {

    override suspend fun executeWorkflow(
      input: LoadedInProgressAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<CompletedAppointmentState>> =
      Either.Right(
        WorkflowResult(
          state = CompletedAppointmentState(CompleteAppointment.of(input.appointment, input.notes)),
          context = context,
        )
      )
  }

  private class PersistAppointmentCompletedStep(
    override val id: String,
    private val appointmentEventPort: AppointmentEventPort,
  ) : UseCaseWorkflow<CompletedAppointmentState, AppointmentCompletedPersistedState>() {

    override suspend fun executeWorkflow(
      input: CompletedAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AppointmentCompletedPersistedState>> =
      appointmentEventPort.handle(AppointmentCompletedEvent(input.appointment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = AppointmentCompletedPersistedState(event), context = context) }
  }
}

internal class CancelAppointmentUseCase(
  private val appointmentStateService: AppointmentStateService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) {

  private val useCase = useCase<CancelAppointmentRequest> {
    startWith { request -> Either.Right(CancelAppointmentState(request.appointmentId, request.notes)) }
    then(LoadAppointmentStep("load-appointment-by-id", findAppointmentPort))
    then(CancelAppointmentStep("cancel-appointment-state-transition", appointmentStateService))
    then(PersistAppointmentCancelledStep("persist-appointment-cancelled", appointmentEventPort))
  }

  suspend fun execute(command: CancelAppointmentCommand): Either<LegacyWorkflowError, AppointmentCancelledEvent> =
    useCase.executeLegacyProjected(
      request = command,
      requestMapper = { CancelAppointmentRequest(it.appointmentId, it.notes) },
      projector = { result ->
        result.requireState<AppointmentCancelledPersistedState>("persist-appointment-cancelled").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
      domainErrorMapper = ::mapBookingDomainError,
    )

  private class LoadAppointmentStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<CancelAppointmentState, LoadedAppointmentState>() {

    override suspend fun executeWorkflow(
      input: CancelAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedAppointmentState>> =
      findAppointmentPort.findById(input.appointmentId)
        .toUseCaseEither()
        .flatMap { appointment ->
          appointment
            ?.let {
              Either.Right(
                WorkflowResult(
                  state = LoadedAppointmentState(appointment = it, notes = input.notes),
                  context = context,
                )
              )
            }
            ?: Either.Left(
              UseCaseError.DomainError(
                APPOINTMENT_NOT_FOUND_CODE,
                "Appointment(${input.appointmentId} not found",
              )
            )
        }
  }

  private class CancelAppointmentStep(
    override val id: String,
    private val appointmentStateService: AppointmentStateService,
  ) : UseCaseWorkflow<LoadedAppointmentState, CancelledAppointmentState>() {

    override suspend fun executeWorkflow(
      input: LoadedAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<CancelledAppointmentState>> =
      either<AppointmentError, CancelledAppointment> {
        appointmentStateService.cancel(input.appointment)
      }
        .toUseCaseEither { legacyError ->
          when (legacyError) {
            is CancelAppointmentError -> UseCaseError.DomainError(CANCEL_APPOINTMENT_CODE, legacyError.message)
            else -> null
          }
        }
        .map { appointment -> WorkflowResult(state = CancelledAppointmentState(appointment), context = context) }
  }

  private class PersistAppointmentCancelledStep(
    override val id: String,
    private val appointmentEventPort: AppointmentEventPort,
  ) : UseCaseWorkflow<CancelledAppointmentState, AppointmentCancelledPersistedState>() {

    override suspend fun executeWorkflow(
      input: CancelledAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AppointmentCancelledPersistedState>> =
      appointmentEventPort.handle(AppointmentCancelledEvent(input.appointment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = AppointmentCancelledPersistedState(event), context = context) }
  }
}

internal class FetchUserAppointmentsUseCase(
  private val findAppointmentPort: FindAppointmentPort,
) {

  private val useCase = useCase<FetchUserAppointmentsRequest> {
    startWith { request -> Either.Right(FetchUserAppointmentsState(request.userId, request.page, request.size)) }
    then(LoadUserAppointmentsStep("load-user-appointments", findAppointmentPort))
    then(BuildFetchedAppointmentsStep("emit-user-appointments-fetched"))
  }

  suspend fun execute(query: FetchUserAppointmentsQuery): Either<LegacyWorkflowError, UserAppointmentsFetchedEvent> =
    useCase.executeLegacyProjected(
      request = query,
      requestMapper = { FetchUserAppointmentsRequest(it.userId, it.page, it.size) },
      projector = { result ->
        result.requireState<FetchedAppointmentsState>("emit-user-appointments-fetched").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    )

  private class LoadUserAppointmentsStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<FetchUserAppointmentsState, LoadedUserAppointmentsState>() {

    override suspend fun executeWorkflow(
      input: FetchUserAppointmentsState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedUserAppointmentsState>> =
      findAppointmentPort.findByUserId(input.userId, PageRequest.of(input.page, input.size))
        .toUseCaseEither()
        .map { appointments -> WorkflowResult(state = LoadedUserAppointmentsState(appointments), context = context) }
  }

  private class BuildFetchedAppointmentsStep(
    override val id: String,
  ) : UseCaseWorkflow<LoadedUserAppointmentsState, FetchedAppointmentsState>() {

    override suspend fun executeWorkflow(
      input: LoadedUserAppointmentsState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<FetchedAppointmentsState>> =
      Either.Right(
        WorkflowResult(
          state = FetchedAppointmentsState(
            UserAppointmentsFetchedEvent(
              input.appointments
                .filter { it !is CancelledAppointment }
                .map(Appointment::toDto)
            )
          ),
          context = context,
        )
      )
  }
}

internal class PayAppointmentUseCase(
  private val paymentApi: PaymentApi,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) {

  private val useCase = useCase<PayAppointmentRequest> {
    startWith { request -> Either.Right(PayAppointmentState(request.appointmentId, request.paymentMethodId)) }
    then(LoadCompletedAppointmentStep("load-completed-appointment", findAppointmentPort))
    then(CapturePaymentStep("capture-payment", paymentApi))
    then(BuildPaidAppointmentStep("build-paid-appointment"))
    then(PersistAppointmentPaidStep("persist-appointment-paid", appointmentEventPort))
  }

  suspend fun execute(command: PayAppointmentCommand): Either<LegacyWorkflowError, AppointmentPaidEvent> =
    useCase.executeLegacyProjected(
      request = command,
      requestMapper = { PayAppointmentRequest(it.appointmentId, it.paymentMethodId) },
      projector = { result ->
        result.requireState<AppointmentPaidPersistedState>("persist-appointment-paid").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
      domainErrorMapper = ::mapBookingOrPaymentDomainError,
    )

  private class LoadCompletedAppointmentStep(
    override val id: String,
    private val findAppointmentPort: FindAppointmentPort,
  ) : UseCaseWorkflow<PayAppointmentState, LoadedCompletedAppointmentState>() {

    override suspend fun executeWorkflow(
      input: PayAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedCompletedAppointmentState>> =
      findAppointmentPort.findCompletedById(input.appointmentId)
        .toUseCaseEither()
        .flatMap { appointment ->
          appointment
            ?.let {
              Either.Right(
                WorkflowResult(
                  state = LoadedCompletedAppointmentState(
                    appointment = it,
                    paymentMethodId = input.paymentMethodId,
                  ),
                  context = context,
                )
              )
            }
            ?: Either.Left(
              UseCaseError.DomainError(
                APPOINTMENT_NOT_FOUND_CODE,
                "Appointment(${input.appointmentId} must be Completed",
              )
            )
        }
  }

  private class CapturePaymentStep(
    override val id: String,
    private val paymentApi: PaymentApi,
  ) : UseCaseWorkflow<LoadedCompletedAppointmentState, CapturedPaymentState>() {

    override suspend fun executeWorkflow(
      input: LoadedCompletedAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<CapturedPaymentState>> =
      paymentApi.makePayment(
        MakePaymentCommand(
          paymentMethodId = input.paymentMethodId,
          amount = input.appointment.totalDue(),
        )
      )
        .toUseCaseEither { legacyError ->
          when (legacyError) {
            is PaymentMethodNotFoundError -> UseCaseError.DomainError(PAYMENT_METHOD_NOT_FOUND_CODE, legacyError.message)
            is PaymentDeclinedError -> UseCaseError.DomainError(PAYMENT_DECLINED_CODE, legacyError.message)
            else -> null
          }
        }
        .map { paymentEvent ->
          WorkflowResult(
            state = CapturedPaymentState(
              appointment = input.appointment,
              paymentMadeEvent = paymentEvent,
            ),
            context = context,
          )
        }
  }

  private class BuildPaidAppointmentStep(
    override val id: String,
  ) : UseCaseWorkflow<CapturedPaymentState, PaidAppointmentState>() {

    override suspend fun executeWorkflow(
      input: CapturedPaymentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<PaidAppointmentState>> =
      either {
        PaidAppointment.of(input.appointment, input.paymentMadeEvent.paymentDto.paymentId)
      }.fold(
        { Either.Left(WorkflowValidationError(it).toUseCaseError()) },
        { appointment -> Either.Right(WorkflowResult(state = PaidAppointmentState(appointment), context = context)) },
      )
  }

  private class PersistAppointmentPaidStep(
    override val id: String,
    private val appointmentEventPort: AppointmentEventPort,
  ) : UseCaseWorkflow<PaidAppointmentState, AppointmentPaidPersistedState>() {

    override suspend fun executeWorkflow(
      input: PaidAppointmentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AppointmentPaidPersistedState>> =
      appointmentEventPort.handle(AppointmentPaidEvent(input.appointment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = AppointmentPaidPersistedState(event), context = context) }
  }
}

private data class GetAvailabilityRequest(
  val date: LocalDate,
) : UseCaseCommand

private data class AvailabilityRequestState(
  val date: LocalDate,
) : WorkflowState

private data class AvailabilityAppointmentsState(
  val date: LocalDate,
  val appointments: List<Appointment>,
) : WorkflowState

private data class AvailabilityRetrievedState(
  val event: AvailabilityRetrievedEvent,
) : WorkflowState

private data class ScheduleAppointmentRequest(
  val userId: String,
  val scheduledTime: java.time.LocalDateTime,
  val duration: Long,
  val workOrder: WorkOrderDtoIn,
) : UseCaseCommand

private data class ScheduleAppointmentState(
  val userId: String,
  val scheduledTime: java.time.LocalDateTime,
  val duration: Long,
  val workOrder: WorkOrderDtoIn,
) : WorkflowState

private data class ScheduleAppointmentAvailabilityState(
  val userId: String,
  val scheduledTime: java.time.LocalDateTime,
  val duration: Long,
  val workOrder: WorkOrderDtoIn,
  val appointmentsForDay: List<Appointment>,
) : WorkflowState

private data class ScheduledAppointmentState(
  val appointment: ScheduledAppointment,
) : WorkflowState

private data class AppointmentScheduledPersistedState(
  val event: AppointmentScheduledEvent,
) : WorkflowState

private data class AppointmentIdRequest(
  val appointmentId: String,
) : UseCaseCommand

private data class AppointmentIdState(
  val appointmentId: String,
) : WorkflowState

private data class LoadedScheduledAppointmentState(
  val appointment: ScheduledAppointment,
) : WorkflowState

private data class InProgressAppointmentState(
  val appointment: InProgressAppointment,
) : WorkflowState

private data class AppointmentStartedPersistedState(
  val event: AppointmentStartedEvent,
) : WorkflowState

private data class CompleteAppointmentRequest(
  val appointmentId: String,
  val notes: String?,
) : UseCaseCommand

private data class CompleteAppointmentState(
  val appointmentId: String,
  val notes: String?,
) : WorkflowState

private data class LoadedInProgressAppointmentState(
  val appointment: InProgressAppointment,
  val notes: String?,
) : WorkflowState

private data class CompletedAppointmentState(
  val appointment: CompleteAppointment,
) : WorkflowState

private data class AppointmentCompletedPersistedState(
  val event: AppointmentCompletedEvent,
) : WorkflowState

private data class CancelAppointmentRequest(
  val appointmentId: String,
  val notes: String?,
) : UseCaseCommand

private data class CancelAppointmentState(
  val appointmentId: String,
  val notes: String?,
) : WorkflowState

private data class LoadedAppointmentState(
  val appointment: Appointment,
  val notes: String?,
) : WorkflowState

private data class CancelledAppointmentState(
  val appointment: CancelledAppointment,
) : WorkflowState

private data class AppointmentCancelledPersistedState(
  val event: AppointmentCancelledEvent,
) : WorkflowState

private data class FetchUserAppointmentsRequest(
  val userId: String,
  val page: Int,
  val size: Int,
) : UseCaseCommand

private data class FetchUserAppointmentsState(
  val userId: String,
  val page: Int,
  val size: Int,
) : WorkflowState

private data class LoadedUserAppointmentsState(
  val appointments: List<Appointment>,
) : WorkflowState

private data class FetchedAppointmentsState(
  val event: UserAppointmentsFetchedEvent,
) : WorkflowState

private data class PayAppointmentRequest(
  val appointmentId: String,
  val paymentMethodId: String,
) : UseCaseCommand

private data class PayAppointmentState(
  val appointmentId: String,
  val paymentMethodId: String,
) : WorkflowState

private data class LoadedCompletedAppointmentState(
  val appointment: CompleteAppointment,
  val paymentMethodId: String,
) : WorkflowState

private data class CapturedPaymentState(
  val appointment: CompleteAppointment,
  val paymentMadeEvent: PaymentMadeEvent,
) : WorkflowState

private data class PaidAppointmentState(
  val appointment: PaidAppointment,
) : WorkflowState

private data class AppointmentPaidPersistedState(
  val event: AppointmentPaidEvent,
) : WorkflowState

private fun mapBookingDomainError(domainError: UseCaseError.DomainError): LegacyWorkflowError? =
  when (domainError.code) {
    DATE_IN_PAST_CODE -> DateInPastError(domainError.message)
    DATE_TIME_UNAVAILABLE_CODE -> DateTimeUnavailableError(domainError.message)
    APPOINTMENT_VALIDATION_CODE -> AppointmentValidationError(domainError.message)
    APPOINTMENT_NOT_FOUND_CODE -> AppointmentNotFoundError(domainError.message)
    CANCEL_APPOINTMENT_CODE -> CancelAppointmentError(domainError.message)
    else -> null
  }

private fun mapBookingOrPaymentDomainError(domainError: UseCaseError.DomainError): LegacyWorkflowError? =
  mapBookingDomainError(domainError) ?: when (domainError.code) {
    PAYMENT_METHOD_NOT_FOUND_CODE -> PaymentMethodNotFoundError(domainError.message)
    PAYMENT_DECLINED_CODE -> PaymentDeclinedError(domainError.message)
    else -> null
  }

private const val DATE_IN_PAST_CODE = "DATE_IN_PAST"
private const val DATE_TIME_UNAVAILABLE_CODE = "DATE_TIME_UNAVAILABLE"
private const val APPOINTMENT_VALIDATION_CODE = "APPOINTMENT_VALIDATION"
private const val APPOINTMENT_NOT_FOUND_CODE = "APPOINTMENT_NOT_FOUND"
private const val CANCEL_APPOINTMENT_CODE = "CANCEL_APPOINTMENT"
private const val PAYMENT_METHOD_NOT_FOUND_CODE = "PAYMENT_METHOD_NOT_FOUND"
private const val PAYMENT_DECLINED_CODE = "PAYMENT_DECLINED"
