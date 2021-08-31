package ventures.dvx.base.user.usecase

import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.AdminUserId
import ventures.dvx.base.user.api.RegisterAdminUserCommand
import ventures.dvx.base.user.command.AdminUser
import ventures.dvx.base.user.web.AdminRegisteredDto
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.axon.security.runAsSuperUser
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.types.toErrString
import ventures.dvx.common.workflow.UseCaseRunner
import ventures.dvx.common.workflow.runAsyncMono

@Component
class RegisterAdminUserUseCase(
  val commandGateway: ReactorCommandGateway,
  val indexRepository: IndexRepository
) : UseCaseRunner<Unit, Mono<AdminRegisteredDto>> {

  val log by LoggerDelegate()

  override suspend fun run(input: Unit): Mono<AdminRegisteredDto> = runAsyncMono {
    val adminEmail = "admin@dvx.ventures"

    // See if the user exists in the Query DB
    indexRepository.findEntityByAggregateNameAndKey(AdminUser.aggregateName(), adminEmail)
      // Admin user already exists
      ?.let { Mono.just(AdminRegisteredDto) }
      // Admin user doesn't exist, attempt to create
      ?: RegisterAdminUserCommand.of(
        userId = AdminUserId(),
        email = adminEmail, // TODO: Make configurable (YML)
        plainPassword = "DVxR0cks!!!",
        firstName = "DVx",
        lastName = "Admin"
      ).fold({
        log.error("RegisterAdminUserCommand error: ${it.toErrString()}")
        Mono.error(IllegalStateException())
      }, {
        // Send creation command to Axon
        log.info("Setting up default admin: $adminEmail")
        commandGateway.send<AdminUserId>(it)
          .map { AdminRegisteredDto }
          .doOnError { ex -> log.error("Admin user already exists", ex) }
          .runAsSuperUser()
      })
  }
}
