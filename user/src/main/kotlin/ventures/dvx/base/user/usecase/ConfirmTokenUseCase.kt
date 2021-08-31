package ventures.dvx.base.user.usecase

import arrow.core.ValidatedNel
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.InvalidInput
import ventures.dvx.base.user.api.User
import ventures.dvx.base.user.api.ValidateEndUserTokenCommand
import ventures.dvx.base.user.web.SuccessfulEmailLoginDto
import ventures.dvx.base.user.web.ValidateTokenInputDto
import ventures.dvx.common.security.JwtTokenProvider
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.types.toErrStrings
import ventures.dvx.common.workflow.UseCaseRunner
import ventures.dvx.common.workflow.runAsyncMono
import ventures.dxv.base.user.error.UserException
import java.util.*

@Component
class ConfirmTokenUseCase(
  private val commandGateway: ReactorCommandGateway,
  private val tokenProvider: JwtTokenProvider,
) : UseCaseRunner<ValidateTokenInputDto, Mono<SuccessfulEmailLoginDto>> {

  override suspend fun run(input: ValidateTokenInputDto): Mono<SuccessfulEmailLoginDto> = runAsyncMono {
    input.toCommand().fold(
      { errors -> Mono.error(UserException(InvalidInput(errors.toErrStrings()))) },
      { Mono.just(it) }
    )
      .flatMap { commandGateway.send<User>(it) }
      .map { tokenProvider.createToken(input.userId, it.roles.map { role -> SimpleGrantedAuthority(role) }) }
      .map { SuccessfulEmailLoginDto(it) }
  }

  private fun ValidateTokenInputDto.toCommand(): ValidatedNel<ValidationError, ValidateEndUserTokenCommand> =
    ValidateEndUserTokenCommand.of(
      userId = EndUserId(UUID.fromString(this.userId)),
      msisdn = this.msisdn,
      token = this.token
    )
}
