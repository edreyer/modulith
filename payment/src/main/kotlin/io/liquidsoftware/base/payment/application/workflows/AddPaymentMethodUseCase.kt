package io.liquidsoftware.base.payment.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import io.liquidsoftware.base.payment.application.mapper.toDto
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.toUseCaseError
import io.liquidsoftware.common.usecase.toWorkflowEither
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal class AddPaymentMethodUseCase(
  private val paymentEventPort: PaymentEventPort,
) {

  private val useCase = useCase<AddPaymentMethodCommand> {
    startWith { request ->
      Either.Right(
        AddPaymentMethodState(
          userId = request.userId,
          stripePaymentMethodId = request.stripePaymentMethodId,
          lastFour = request.lastFour,
        )
      )
    }
    then(BuildPaymentMethodStep("build-active-payment-method"))
    then(PersistPaymentMethodStep("persist-payment-method-added", paymentEventPort))
  }

  suspend fun execute(command: AddPaymentMethodCommand): Either<LegacyWorkflowError, PaymentMethodAddedEvent> =
    useCase.executeProjected(
      command,
      projector = { result ->
        result.requireState<PaymentMethodAddedState>("persist-payment-method-added").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    ).toWorkflowEither()

  private class BuildPaymentMethodStep(
    override val id: String,
  ) : UseCaseWorkflow<AddPaymentMethodState, ActivePaymentMethodState>() {

    override suspend fun executeWorkflow(
      input: AddPaymentMethodState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<ActivePaymentMethodState>> =
      either {
        ActivePaymentMethod.of(
          userId = input.userId,
          stripePaymentMethodId = input.stripePaymentMethodId,
          lastFour = input.lastFour,
        )
      }.fold(
        { Either.Left(WorkflowValidationError(it).toUseCaseError()) },
        { paymentMethod -> Either.Right(WorkflowResult(ActivePaymentMethodState(paymentMethod), context = context)) },
      )
  }

  private class PersistPaymentMethodStep(
    override val id: String,
    private val paymentEventPort: PaymentEventPort,
  ) : UseCaseWorkflow<ActivePaymentMethodState, PaymentMethodAddedState>() {

    override suspend fun executeWorkflow(
      input: ActivePaymentMethodState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<PaymentMethodAddedState>> =
      paymentEventPort.handle(PaymentMethodAddedEvent(input.paymentMethod.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(PaymentMethodAddedState(event), context = context) }
  }
}

private data class AddPaymentMethodState(
  val userId: String,
  val stripePaymentMethodId: String,
  val lastFour: String,
) : WorkflowState

private data class ActivePaymentMethodState(
  val paymentMethod: ActivePaymentMethod,
) : WorkflowState

private data class PaymentMethodAddedState(
  val event: PaymentMethodAddedEvent,
) : WorkflowState
