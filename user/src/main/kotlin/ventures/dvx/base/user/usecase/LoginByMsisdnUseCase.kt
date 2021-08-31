package ventures.dvx.base.user.usecase

import org.axonframework.eventhandling.EventHandler
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.EndUserLoginStartedEvent
import ventures.dvx.base.user.api.InvalidInput
import ventures.dvx.base.user.api.LoginEndUserCommand
import ventures.dvx.base.user.api.UserNotFoundError
import ventures.dvx.base.user.command.EndUser
import ventures.dvx.base.user.web.MsisdnLoginDto
import ventures.dvx.common.axon.command.persistence.IndexJpaEntity
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.service.sms.SmsService
import ventures.dvx.common.types.toErrStrings
import ventures.dvx.common.workflow.UseCaseRunner
import ventures.dvx.common.workflow.runAsyncMono
import ventures.dxv.base.user.error.UserException

@Component
class LoginByMsisdnUseCase(
  private val indexRepository: IndexRepository,
  val commandGateway: ReactorCommandGateway,
) : UseCaseRunner<MsisdnLoginDto, Mono<EndUserId>> {

  override suspend fun run(input: MsisdnLoginDto): Mono<EndUserId> = runAsyncMono {
    val user: Mono<IndexJpaEntity> =
      indexRepository.findEntityByAggregateNameAndKey(EndUser.aggregateName(), input.msisdn)
        ?.let { Mono.just(it) }
        ?: Mono.error(UserException(UserNotFoundError(input.msisdn)))

    user
      .map { LoginEndUserCommand.of(EndUserId(it.id), it.key) }
      .flatMap { it.fold(
        { errors -> Mono.error(UserException(InvalidInput(errors.toErrStrings())))},
        { cmd -> Mono.just(cmd) }
      )}
      .flatMap { commandGateway.send(it) }
  }


  @EventHandler
  fun handle(event: EndUserLoginStartedEvent, smsService: SmsService) =
    smsService.sendMessage(
      event.token.msisdn.value,
      "Here is your DVX Application Token: ${event.token.token}"
    )
}
