package ventures.dvx.base.user.usecase

import arrow.core.ValidatedNel
import org.axonframework.eventhandling.EventHandler
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.InvalidInput
import ventures.dvx.base.user.api.RegisterEndUserCommand
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.base.user.web.RegisterEndUserInputDto
import ventures.dvx.base.user.web.RegisteredUserDto
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.service.sms.SmsService
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.types.toErrStrings
import ventures.dvx.common.workflow.UseCaseRunner
import ventures.dvx.common.workflow.runAsyncMono
import ventures.dxv.base.user.error.UserException

@Component
class RegisterEndUserUseCase(
  val commandGateway: ReactorCommandGateway
) : UseCaseRunner<RegisterEndUserInputDto, Mono<RegisteredUserDto>> {

  val log by LoggerDelegate()

  override suspend fun run(input: RegisterEndUserInputDto): Mono<RegisteredUserDto> = runAsyncMono {
    input.toCommand()
      .fold(
        { errors -> Mono.error(UserException(InvalidInput(errors.toErrStrings()))) },
        { Mono.just(it) }
      )
      .flatMap { commandGateway.send<EndUserId>(it) }
      .map { RegisteredUserDto(it.id) }
  }

  private fun RegisterEndUserInputDto.toCommand(): ValidatedNel<ValidationError, RegisterEndUserCommand> =
    RegisterEndUserCommand.of(
      userId = EndUserId(),
      msisdn = this.msisdn,
      email = this.email,
      firstName = this.firstName,
      lastName = this.lastName
    )

  @EventHandler
  fun handle(event: UserRegistrationStartedEvent, smsService: SmsService) =
    smsService.sendMessage(
      event.token.msisdn.value,
      "Here is your DVX Application Token: ${event.token.token}"
    )

}
