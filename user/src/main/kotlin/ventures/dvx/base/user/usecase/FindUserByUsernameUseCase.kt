package ventures.dvx.base.user.usecase

import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.FindUserByUsernameQuery
import ventures.dvx.base.user.api.User
import ventures.dvx.common.workflow.UseCaseRunner
import ventures.dvx.common.workflow.runAsyncMono

@Component
class FindUserByUsernameUseCase(
  val queryGateway: ReactorQueryGateway
) : UseCaseRunner<String, Mono<User>> {

  override suspend fun run(input: String) : Mono<User> = runAsyncMono {
    queryGateway.query(FindUserByUsernameQuery(input), User::class.java)
  }

}
