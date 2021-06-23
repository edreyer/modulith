package ventures.dvx.base.user.config

import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import ventures.dvx.base.user.api.AdminUserId
import ventures.dvx.base.user.api.RegisterAdminUserCommand
import ventures.dvx.base.user.command.AdminUser
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.spring.YamlPropertySourceFactory

@Configuration
@PropertySource(value = ["classpath:user.yml"], factory = YamlPropertySourceFactory::class)
class UserConfig(
  private val commandGateway: ReactorCommandGateway,
  private val indexRepository: IndexRepository,
  @Value("\${user.forcedMsisdnToken}") val forcedMsisdnToken: String
) {

  val log by LoggerDelegate()

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
        .doOnError { log.info("Admin user already exists") }
        .subscribe()
  }

}
