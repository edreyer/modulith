package io.liquidsoftware.base.payment.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.liquidsoftware.base.payment.PaymentMethodId
import io.liquidsoftware.base.payment.application.mapper.toDto
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMethodNotFoundError
import io.liquidsoftware.base.payment.application.port.out.FindPaymentMethodPort
import io.liquidsoftware.base.payment.application.port.out.PaymentEventPort
import io.liquidsoftware.base.payment.application.service.StripeService
import io.liquidsoftware.base.payment.application.service.StripeService.SuccessfulPayment
import io.liquidsoftware.base.payment.domain.Payment
import io.liquidsoftware.base.payment.domain.PaymentMethod
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.toApplicationUseCaseEither
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.toUseCaseError
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowValidationError
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal class MakePaymentUseCase(
  private val executionContext: ExecutionContext,
  private val findPaymentMethodPort: FindPaymentMethodPort,
  private val paymentEventPort: PaymentEventPort,
  private val stripeService: StripeService,
) {

  private val useCase = useCase<MakePaymentCommand> {
    startWith { request ->
      Either.Right(
        MakePaymentState(
          paymentMethodId = request.paymentMethodId,
          amount = request.amount,
          currentUserId = executionContext.getCurrentUser().id,
        )
      )
    }
    then(ValidatePaymentRequestStep("validate-payment-request"))
    then(LoadPaymentMethodStep("load-payment-method", findPaymentMethodPort))
    then(ProcessStripePaymentStep("process-stripe-payment", stripeService))
    then(BuildPaymentStep("build-payment"))
    then(PersistPaymentStep("persist-payment-made", paymentEventPort))
  }

  suspend fun execute(command: MakePaymentCommand): Either<ApplicationError, PaymentMadeEvent> =
    useCase.executeProjected(
      command,
      projector = { result ->
        result.requireState<PaymentMadeState>("persist-payment-made").fold(
          { Either.Left(it) },
          { state -> Either.Right(state.event) },
        )
      },
    ).toApplicationUseCaseEither { domainError ->
      when (domainError.code) {
        PAYMENT_METHOD_NOT_FOUND_CODE -> PaymentMethodNotFoundError(domainError.message)
        PAYMENT_DECLINED_CODE -> PaymentDeclinedError(domainError.message)
        else -> null
      }
    }

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
      either {
        stripeService.makePayment(input.paymentMethod, input.amount)
      }.fold(
        { declinedError ->
          Either.Left(UseCaseError.DomainError(PAYMENT_DECLINED_CODE, declinedError.message))
        },
        { successfulPayment ->
          Either.Right(
            WorkflowResult(
              SuccessfulPaymentState(
                successfulPayment = successfulPayment,
                userId = input.userId,
              ),
              context = context,
            )
          )
        },
      )
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
