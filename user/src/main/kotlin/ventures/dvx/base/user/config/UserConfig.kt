package ventures.dvx.base.user.config

import org.axonframework.common.caching.Cache
import org.axonframework.common.caching.WeakReferenceCache
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import ventures.dvx.base.user.api.AdminUserId
import ventures.dvx.base.user.api.RegisterAdminUserCommand
import ventures.dvx.base.user.command.AdminUser
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.logging.LoggerDelegate

@Configuration
class UserConfig(
  val commandGateway: ReactorCommandGateway,
  val indexRepository: IndexRepository
) {

  val log by LoggerDelegate()

  @Bean
  fun userCache(): Cache {
    // TODO replace with proper caching
    return WeakReferenceCache()
  }

  @EventListener
  fun initializeAdmin(event: ContextRefreshedEvent) {
    val adminEmail = "admin@dvx.ventures"
    log.trace("Setting up default admin: $adminEmail")

    indexRepository.findEntityByAggregateNameAndKey(AdminUser.aggregateName(), adminEmail)
      ?: commandGateway.send<Unit>(RegisterAdminUserCommand(
        userId = AdminUserId(),
        email = adminEmail, // TODO: Make configurable (YML)
        plainPassword = "DVxR0cks!!!",
        firstName = "DVx",
        lastName = "Admin"
      ))
        .doOnError { ex -> log.info("Admin user already exists") }
        .subscribe()
  }

}
