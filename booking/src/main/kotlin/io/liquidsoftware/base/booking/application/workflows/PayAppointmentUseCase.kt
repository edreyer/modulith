package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.PaidAppointment
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.toApplicationUseCaseEither
import io.liquidsoftware.common.application.error.toUseCaseApplicationEither
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.toUseCaseError
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowValidationError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

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

  suspend fun execute(command: PayAppointmentCommand): Either<ApplicationError, AppointmentPaidEvent> =
    useCase.executeProjected(
      PayAppointmentRequest(command.appointmentId, command.paymentMethodId),
      projector = { result ->
        result.requireState<AppointmentPaidPersistedState>("persist-appointment-paid").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    ).toApplicationUseCaseEither(::mapBookingOrPaymentDomainError)

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
        .toUseCaseApplicationEither { applicationError ->
          when (applicationError) {
            is PaymentMethodNotFoundError ->
              UseCaseError.DomainError(PAYMENT_METHOD_NOT_FOUND_CODE, applicationError.message)
            is PaymentDeclinedError ->
              UseCaseError.DomainError(PAYMENT_DECLINED_CODE, applicationError.message)
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
