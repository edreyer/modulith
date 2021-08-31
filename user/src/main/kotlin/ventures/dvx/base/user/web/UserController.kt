package ventures.dvx.base.user.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import ventures.dvx.base.user.usecase.FindUserByIdUseCase
import ventures.dvx.base.user.usecase.FindUserByUsernameUseCase
import java.util.*

// DTOs
data class UserDto(val id: UUID, val username: String, val email: String, val roles: List<String>) : OutputDto

@RestController
class UserController(
  private val findUserByIdUseCase: FindUserByIdUseCase,
  private val findUserByUsernameUseCase: FindUserByUsernameUseCase
) : BaseUserController() {

  @GetMapping(path = [UserPaths.USER_BY_ID])
  suspend fun getUser(@PathVariable userId: UUID) : ResponseEntity<OutputDto> =
    findUserByIdUseCase.run(userId)
      .mapToResponse { ResponseEntity.ok(UserDto(it.id, it.username, it.email, it.roles)) }

  @GetMapping(path = [UserPaths.USER_BY_USERNAME])
  suspend fun getUser(@PathVariable username: String) : ResponseEntity<OutputDto> =
    findUserByUsernameUseCase.run(username)
      .mapToResponse { ResponseEntity.ok(UserDto(it.id, it.username, it.email, it.roles)) }

}
