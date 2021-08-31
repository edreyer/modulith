package ventures.dvx.base.user.usecase

import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ventures.dvx.base.user.web.EmailLoginDto
import ventures.dvx.base.user.web.SuccessfulEmailLoginDto
import ventures.dvx.common.security.JwtTokenProvider
import ventures.dvx.common.workflow.UseCaseRunner
import ventures.dvx.common.workflow.runAsyncMono

@Component
class LoginByEmailUseCase(
  private val authenticationManager: ReactiveAuthenticationManager,
  private val tokenProvider: JwtTokenProvider,
  ) : UseCaseRunner<EmailLoginDto, Mono<SuccessfulEmailLoginDto>> {

  override suspend fun run(input: EmailLoginDto): Mono<SuccessfulEmailLoginDto> = runAsyncMono {
    authenticationManager.authenticate(
      UsernamePasswordAuthenticationToken(input.email, input.password)
    )
      .map { tokenProvider.createToken(it) }
      .map { SuccessfulEmailLoginDto(it) }
  }

}
