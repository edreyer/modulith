package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.payment.application.port.`in`.MakePaymentCommand
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDeclinedError
import io.liquidsoftware.base.payment.application.port.`in`.PaymentDtoOut
import io.liquidsoftware.base.payment.application.port.`in`.PaymentMadeEvent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PayAppointmentUseCaseTest {

  @Test
  fun `delegates to payment api and persists paid appointment`() = runBlocking {
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
  }

  @Test
  fun `returns not found when completed appointment is missing`() = runBlocking {
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
  fun `propagates payment decline without persisting appointment`() = runBlocking {
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
}
