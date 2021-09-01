package ventures.dvx.base.user.usecase

import kotlinx.coroutines.reactor.awaitSingle
import org.axonframework.eventhandling.EventHandler
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.EndUserLoginStartedEvent
import ventures.dvx.base.user.api.InvalidInput
import ventures.dvx.base.user.api.LoginEndUserCommand
import ventures.dvx.base.user.web.MsisdnLoginDto
import ventures.dvx.common.axon.security.runAsSuperUser
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.service.sms.SmsService
import ventures.dvx.common.types.toErrStrings
import ventures.dvx.common.workflow.UseCaseRunner
import ventures.dvx.common.workflow.runAsyncMono
import ventures.dxv.base.user.error.UserException

@Component
class LoginByMsisdnUseCase(
  private val commandGateway: ReactorCommandGateway,
  private val findUserByUsernameUseCase: FindUserByUsernameUseCase
) : UseCaseRunner<MsisdnLoginDto, Mono<EndUserId>> {

  val log by LoggerDelegate()

  override suspend fun run(input: MsisdnLoginDto): Mono<EndUserId> = runAsyncMono {
    findUserByUsernameUseCase.run(input.msisdn)
      .runAsSuperUser() // user is not logged in yet, so we have to run as superuser
      .awaitSingle()
      .let { LoginEndUserCommand.of(EndUserId(it.id), input.msisdn) }
      .fold(
        { errors -> Mono.error(UserException(InvalidInput(errors.toErrStrings())))},
        { cmd -> Mono.just(cmd) }
      )
      .flatMap { commandGateway.send(it) }
  }


  @EventHandler
  fun handle(event: EndUserLoginStartedEvent, smsService: SmsService) =
    smsService.sendMessage(
      event.token.msisdn.value,
      "Here is your DVX Application Token: ${event.token.token}"
    )

}
