package ventures.dvx.base.user.application.usecase

import arrow.core.*
import arrow.core.computations.either
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.port.`in`.*
import ventures.dvx.base.user.application.port.`in`.RegisterUserError.UserExistsError
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent.ValidUserRegistration
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveUserPort
import ventures.dvx.base.user.domain.UnregisteredUser

@Component
class RegisterUserUseCaseImpl(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val saveUserPort: SaveUserPort
) : RegisterUserUseCase {

  override suspend operator fun invoke(cmd: RegisterUserCommand) :
    Either<RegisterUserError, Nel<RegisterUserEvent>> =
    either {
      validateNewUser(cmd.username).bind()

      val unregisteredUser = UnregisteredUser(
        username = cmd.username,
        email = cmd.email,
        encryptedPassword = passwordEncoder.encode(cmd.password)
      )

      val saved = saveUserPort.saveUser(unregisteredUser)

      ValidUserRegistration(saved.username.value, saved.email.value).nel()
    }

  private suspend fun validateNewUser(username: String) : Either<RegisterUserError, Unit> =
    findUserPort.findUserByUsername(username)
      ?.let { UserExistsError("User ${username} exists").left() }
      ?: Unit.right()

}
