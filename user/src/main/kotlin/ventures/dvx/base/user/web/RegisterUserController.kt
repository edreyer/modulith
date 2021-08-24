package ventures.dvx.base.user.web

import arrow.core.ValidatedNel
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.AdminUserId
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.InvalidInput
import ventures.dvx.base.user.api.RegisterAdminUserCommand
import ventures.dvx.base.user.api.RegisterEndUserCommand
import ventures.dvx.base.user.command.AdminUser
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.axon.security.runAsSuperUser
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.types.toErrString
import ventures.dvx.common.types.toErrStrings
import ventures.dvx.common.validation.Msisdn
import ventures.dxv.base.user.error.UserException
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

// input DTOs

data class RegisterEndUserInputDto(
  @NotEmpty @Msisdn val msisdn: String,
  @NotEmpty @Email val email: String,
  @NotEmpty val firstName: String,
  @NotEmpty val lastName: String
)

// output DTOs

sealed class RegisterUserOutputDto : OutputDto
data class RegisteredUserDto(val id: UUID) : RegisterUserOutputDto()


@RestController
class RegisterUserController(
  private val commandGateway: ReactorCommandGateway,
  private val indexRepository: IndexRepository
) : BaseUserController() {

  val log by LoggerDelegate()

  @PostMapping(path = ["/admin/register"])
  fun registerAdmin()
    : Mono<ResponseEntity<OutputDto>> {
    val adminEmail = "admin@dvx.ventures"
    return indexRepository.findEntityByAggregateNameAndKey(AdminUser.aggregateName(), adminEmail)
      ?.let { Mono.just(it).mapToResponse { ResponseEntity.ok(AdminRegisteredDto) } }
      ?: RegisterAdminUserCommand.of(
        userId = AdminUserId(),
        email = adminEmail, // TODO: Make configurable (YML)
        plainPassword = "DVxR0cks!!!",
        firstName = "DVx",
        lastName = "Admin"
      ).fold({
        log.error("RegisterAdminUserCommand error: ${it.toErrString()}")
        return Mono.error(IllegalStateException())
      },{
        log.info("Setting up default admin: $adminEmail")
        commandGateway.send<AdminUserId>(it)
          .doOnError { log.error("Admin user already exists") }
          .runAsSuperUser()
      })
        .mapToResponse { ResponseEntity.ok(AdminRegisteredDto) }
  }

  @PostMapping(path = ["/user/register"])
  fun register(@Valid @RequestBody input: RegisterEndUserInputDto)
  : Mono<ResponseEntity<OutputDto>> =
    input.toCommand().fold(
      { errors -> Mono.error(UserException(InvalidInput(errors.toErrStrings())))},
      { cmd -> Mono.just(cmd) }
    )
      .flatMap { commandGateway.send<EndUserId>(it) }
      .map { RegisteredUserDto(it.id) }
      .mapToResponse { ResponseEntity.ok(it) }
}

fun RegisterEndUserInputDto.toCommand(): ValidatedNel<ValidationError, RegisterEndUserCommand> =
  RegisterEndUserCommand.of(
    userId = EndUserId(),
    msisdn = this.msisdn,
    email = this.email,
    firstName = this.firstName,
    lastName = this.lastName
  )


