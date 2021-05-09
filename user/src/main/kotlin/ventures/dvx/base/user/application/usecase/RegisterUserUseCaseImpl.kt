package ventures.dvx.base.user.application.usecase

import arrow.core.*
import arrow.core.computations.either
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.application.port.`in`.*
import ventures.dvx.base.user.application.port.`in`.RegisterUserError.UserExistsError
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent.ValidUserRegistration
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.domain.UnregisteredUser

class RegisterUserUseCaseImpl(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val saveNewUserPort: SaveNewUserPort
) : RegisterUserUseCase {

  override suspend operator fun invoke(cmd: RegisterUserCommand) :
    Either<RegisterUserError, Nel<RegisterUserEvent>> =
    either {
      val unregisteredUser = validateNewUser(cmd).bind()
      val savedUser = saveNewUserPort.saveNewUser(unregisteredUser)
      ValidUserRegistration(savedUser.username.value, savedUser.email.value).nel()
    }

  private fun validateNewUser(cmd: RegisterUserCommand) : Validated<RegisterUserError, UnregisteredUser> =
    findUserPort.findUserByUsername(cmd.username)
      ?.let { UserExistsError("User ${cmd.username} exists").invalid() }
      ?: UnregisteredUser.of(
        username = cmd.username,
        email = cmd.email,
        encryptedPassword = passwordEncoder.encode(cmd.password)
      )
        .mapLeft { RegisterUserError.UserValidationErrors(it) }


}
