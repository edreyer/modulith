package ventures.dvx.base.user.usecase

import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.FindUserByIdQuery
import ventures.dvx.base.user.api.User
import ventures.dvx.common.workflow.UseCaseRunner
import ventures.dvx.common.workflow.runAsyncMono
import java.util.*

@Component
class FindUserByIdUseCase(
  private val queryGateway: ReactorQueryGateway
) : UseCaseRunner<UUID, Mono<User>> {

  override suspend fun run(input: UUID): Mono<User> = runAsyncMono {
    queryGateway.query(FindUserByIdQuery(input), User::class.java)
  }

}
