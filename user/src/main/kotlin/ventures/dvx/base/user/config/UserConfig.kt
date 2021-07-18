package ventures.dvx.base.user.config

import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import rolesPermissionRegistry
import ventures.dvx.base.user.api.AdminUserId
import ventures.dvx.base.user.api.FindUserByIdQuery
import ventures.dvx.base.user.api.FindUserByUsernameQuery
import ventures.dvx.base.user.api.RegisterAdminUserCommand
import ventures.dvx.base.user.command.AdminUser
import ventures.dvx.base.user.command.UserRole
import ventures.dvx.base.user.config.UserConfig.ResourceTypes.MY_USER
import ventures.dvx.base.user.config.UserConfig.ResourceTypes.NOT_MY_USER
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.Visibility
import ventures.dvx.bridgekeeper.fns.className
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.axon.security.ROLE_SYSTEM
import ventures.dvx.common.axon.security.runAsSuperUser
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.types.toErrString

@Configuration
class UserConfig(
  private val commandGateway: ReactorCommandGateway,
  private val indexRepository: IndexRepository,
  private val userConfigProperties: UserConfigProperties,
) {

  @ConstructorBinding
  @ConfigurationProperties(prefix = "user")
  class UserConfigProperties(
    val forcedMsisdnToken: String
  )

  val log by LoggerDelegate()

  val forcedMsisdnToken
    get() = userConfigProperties.forcedMsisdnToken

  @EventListener
  fun initializeAdmin(event: ContextRefreshedEvent) {
    val adminEmail = "admin@dvx.ventures"

    indexRepository.findEntityByAggregateNameAndKey(AdminUser.aggregateName(), adminEmail)
      ?: RegisterAdminUserCommand.of(
        userId = AdminUserId(),
        email = adminEmail, // TODO: Make configurable (YML)
        plainPassword = "DVxR0cks!!!",
        firstName = "DVx",
        lastName = "Admin"
      ).fold({
        log.error("RegisterAdminUserCommand error: ${it.toErrString()}")
      },{
        log.info("Setting up default admin: $adminEmail")
        commandGateway.send<Unit>(it)
          .doOnError { log.info("Admin user already exists") }
          .runAsSuperUser()
      })

  }

  object UserRoles {
    val ROLE_ADMIN = object: RoleHandle(UserRole.ADMIN.toString()) {}
    val ROLE_END_USER = object: RoleHandle(UserRole.USER.toString()) {}
  }

  object ResourceTypes {
    val ADMIN = object : ResourceType() {}
    val NOT_ADMIN = object : ResourceType() {}

    val MY_USER = object : ResourceType() {}
    val NOT_MY_USER = object : ResourceType() {}
  }

  @Bean("roleHandleMap")
  fun roleHandleMap() = mapOf(
    ROLE_SYSTEM.name to ROLE_SYSTEM,
    UserRole.ADMIN.toString() to UserRoles.ROLE_ADMIN,
    UserRole.USER.toString() to UserRoles.ROLE_END_USER
  )

  // Bridgekeeper
  @Bean("userBridgeKeeper")
  fun userBridgeKeeper() = rolesPermissionRegistry {
    role(UserRoles.ROLE_ADMIN) {
      resourceType(MY_USER) {
        visibility = Visibility.VISIBLE
        operations {
          +className<RegisterAdminUserCommand>()
          +className<FindUserByIdQuery>()
          +className<FindUserByUsernameQuery>()
        }
      }
      resourceType(NOT_MY_USER) {
        visibility = Visibility.VISIBLE
        operations {
          +className<RegisterAdminUserCommand>()
          +className<FindUserByIdQuery>()
          +className<FindUserByUsernameQuery>()
        }
      }
    }
    role(UserRoles.ROLE_END_USER) {
      resourceType(MY_USER) {
        visibility = Visibility.VISIBLE
        operations {
          +className<FindUserByIdQuery>()
        }
      }
    }
    role(ROLE_SYSTEM) {
      resourceType(MY_USER) {
        visibility = Visibility.VISIBLE
        operations {
          +className<RegisterAdminUserCommand>()
        }
      }
    }
  }.let { BridgeKeeper(it) }

}
