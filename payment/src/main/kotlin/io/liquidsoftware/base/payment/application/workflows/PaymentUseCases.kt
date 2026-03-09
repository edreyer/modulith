package io.liquidsoftware.base.payment.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.mapper.toDto
import io.liquidsoftware.base.payment.application.port.`in`.AddPaymentMethodCommand
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodAddedEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.service.StripeService
import io.liquidsoftware.base.payment.application.service.StripeService.SuccessfulPayment
import io.liquidsoftware.base.payment.domain.ActivePaymentMethod
import io.liquidsoftware.base.payment.domain.Payment
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.legacy.executeLegacyProjected
import io.liquidsoftware.common.usecase.legacy.toUseCaseEither
import io.liquidsoftware.common.usecase.legacy.toUseCaseError
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal class AddPaymentMethodUseCase(
  private val paymentEventPort: PaymentEventPort,
) {

  private val useCase = useCase<AddPaymentMethodRequest> {
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
    useCase.executeLegacyProjected(
      request = command,
      requestMapper = { request ->
        AddPaymentMethodRequest(
          userId = request.userId,
          stripePaymentMethodId = request.stripePaymentMethodId,
          lastFour = request.lastFour,
        )
      },
      projector = { result ->
        result.requireState<PaymentMethodAddedState>("persist-payment-method-added").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    )

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

internal class MakePaymentUseCase(
  private val executionContext: ExecutionContext,
  private val findPaymentMethodPort: FindPaymentMethodPort,
  private val paymentEventPort: PaymentEventPort,
  private val stripeService: StripeService,
) {

  private val useCase = useCase<MakePaymentRequest> {
    startWith { request ->
      Either.Right(
        MakePaymentState(
          paymentMethodId = request.paymentMethodId,
          amount = request.amount,
          currentUserId = request.currentUserId,
        )
      )
    }
    then(ValidatePaymentRequestStep("validate-payment-request"))
    then(LoadPaymentMethodStep("load-payment-method", findPaymentMethodPort))
    then(ProcessStripePaymentStep("process-stripe-payment", stripeService))
    then(BuildPaymentStep("build-payment"))
    then(PersistPaymentStep("persist-payment-made", paymentEventPort))
  }

  suspend fun execute(command: MakePaymentCommand): Either<LegacyWorkflowError, PaymentMadeEvent> =
    useCase.executeLegacyProjected(
      request = command,
      requestMapper = { request ->
        MakePaymentRequest(
          paymentMethodId = request.paymentMethodId,
          amount = request.amount,
          currentUserId = executionContext.getCurrentUser().id,
        )
      },
      projector = { result ->
        result.requireState<PaymentMadeState>("persist-payment-made").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
      domainErrorMapper = { domainError ->
        when (domainError.code) {
          PAYMENT_METHOD_NOT_FOUND_CODE -> PaymentMethodNotFoundError(domainError.message)
          PAYMENT_DECLINED_CODE -> PaymentDeclinedError(domainError.message)
          else -> null
        }
      },
    )

  private class ValidatePaymentRequestStep(
    override val id: String,
  ) : UseCaseWorkflow<MakePaymentState, ValidatedPaymentState>() {

    override suspend fun executeWorkflow(
      input: MakePaymentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<ValidatedPaymentState>> =
      either {
        ValidatedPaymentState(
          paymentMethodId = PaymentMethodId.of(input.paymentMethodId),
          userId = UserId.of(input.currentUserId),
          amount = input.amount,
        )
      }.fold(
        { Either.Left(WorkflowValidationError(it).toUseCaseError()) },
        { state -> Either.Right(WorkflowResult(state, context = context)) },
      )
  }

  private class LoadPaymentMethodStep(
    override val id: String,
    private val findPaymentMethodPort: FindPaymentMethodPort,
  ) : UseCaseWorkflow<ValidatedPaymentState, LoadedPaymentMethodState>() {

    override suspend fun executeWorkflow(
      input: ValidatedPaymentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<LoadedPaymentMethodState>> =
      findPaymentMethodPort.findByPaymentMethodId(input.paymentMethodId, input.userId)
        .toUseCaseEither()
        .flatMap { paymentMethod ->
          paymentMethod
            ?.let {
              Either.Right(
                WorkflowResult(
                  LoadedPaymentMethodState(
                    paymentMethod = it,
                    userId = input.userId,
                    amount = input.amount,
                  ),
                  context = context,
                )
              )
            }
            ?: Either.Left(UseCaseError.DomainError(PAYMENT_METHOD_NOT_FOUND_CODE, input.paymentMethodId.value))
        }
  }

  private class ProcessStripePaymentStep(
    override val id: String,
    private val stripeService: StripeService,
  ) : UseCaseWorkflow<LoadedPaymentMethodState, SuccessfulPaymentState>() {

    override suspend fun executeWorkflow(
      input: LoadedPaymentMethodState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<SuccessfulPaymentState>> =
      either<LegacyWorkflowError, SuccessfulPayment> {
        stripeService.makePayment(input.paymentMethod, input.amount)
      }
        .toUseCaseEither { legacyError ->
          when (legacyError) {
            is PaymentDeclinedError -> UseCaseError.DomainError(PAYMENT_DECLINED_CODE, legacyError.message)
            else -> null
          }
        }
        .map { successfulPayment ->
          WorkflowResult(
            SuccessfulPaymentState(
              successfulPayment = successfulPayment,
              userId = input.userId,
            ),
            context = context,
          )
        }
  }

  private class BuildPaymentStep(
    override val id: String,
  ) : UseCaseWorkflow<SuccessfulPaymentState, PaymentAggregateState>() {

    override suspend fun executeWorkflow(
      input: SuccessfulPaymentState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<PaymentAggregateState>> =
      either {
        Payment.of(
          paymentMethodId = input.successfulPayment.paymentMethod.id.value,
          userId = input.userId.value,
          amount = input.successfulPayment.amount,
        )
      }.fold(
        { Either.Left(WorkflowValidationError(it).toUseCaseError()) },
        { payment -> Either.Right(WorkflowResult(PaymentAggregateState(payment), context = context)) },
      )
  }

  private class PersistPaymentStep(
    override val id: String,
    private val paymentEventPort: PaymentEventPort,
  ) : UseCaseWorkflow<PaymentAggregateState, PaymentMadeState>() {

    override suspend fun executeWorkflow(
      input: PaymentAggregateState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<PaymentMadeState>> =
      paymentEventPort.handle(PaymentMadeEvent(input.payment.toDto()))
        .toUseCaseEither()
        .map { event -> WorkflowResult(PaymentMadeState(event), context = context) }
  }
}

private data class AddPaymentMethodRequest(
  val userId: String,
  val stripePaymentMethodId: String,
  val lastFour: String,
) : UseCaseCommand

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

private data class MakePaymentRequest(
  val paymentMethodId: String,
  val amount: Long,
  val currentUserId: String,
) : UseCaseCommand

private data class MakePaymentState(
  val paymentMethodId: String,
  val amount: Long,
  val currentUserId: String,
) : WorkflowState

private data class ValidatedPaymentState(
  val paymentMethodId: PaymentMethodId,
  val userId: UserId,
  val amount: Long,
) : WorkflowState

private data class LoadedPaymentMethodState(
  val paymentMethod: PaymentMethod,
  val userId: UserId,
  val amount: Long,
) : WorkflowState

private data class SuccessfulPaymentState(
  val successfulPayment: SuccessfulPayment,
  val userId: UserId,
) : WorkflowState

private data class PaymentAggregateState(
  val payment: Payment,
) : WorkflowState

private data class PaymentMadeState(
  val event: PaymentMadeEvent,
) : WorkflowState

private const val PAYMENT_METHOD_NOT_FOUND_CODE = "PAYMENT_METHOD_NOT_FOUND"
private const val PAYMENT_DECLINED_CODE = "PAYMENT_DECLINED"
