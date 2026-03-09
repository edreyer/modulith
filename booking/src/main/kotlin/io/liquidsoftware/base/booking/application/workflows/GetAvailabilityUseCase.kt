package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityRetrievedEvent
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.toWorkflowEither
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError
import java.time.LocalDate

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
    useCase.executeProjected(
      GetAvailabilityRequest(query.date),
      projector = { result ->
        result.requireState<AvailabilityRetrievedState>("build-availability").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    ).toWorkflowEither(::mapBookingDomainError)

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
