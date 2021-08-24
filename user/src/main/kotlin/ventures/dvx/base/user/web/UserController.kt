package ventures.dvx.base.user.web

import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.FindUserByIdQuery
import ventures.dvx.base.user.api.FindUserByUsernameQuery
import ventures.dvx.base.user.api.User
import java.util.*

// DTOs
data class UserDto(val id: UUID, val username: String, val email: String, val roles: List<String>) : OutputDto

@RestController
class UserController(
  private val queryGateway: ReactorQueryGateway
) : BaseUserController() {

  @GetMapping(path = [UserPaths.USER_BY_ID])
  fun getUser(@PathVariable userId: UUID) : Mono<ResponseEntity<OutputDto>> {
    return queryGateway.query(FindUserByIdQuery(userId), User::class.java)
      .mapToResponse { ResponseEntity.ok(UserDto(it.id, it.username, it.email, it.roles)) }
  }

  @GetMapping(path = [UserPaths.USER_BY_USERNAME])
  fun getUser(@PathVariable username: String) : Mono<ResponseEntity<OutputDto>> {
    return queryGateway.query(FindUserByUsernameQuery(username), User::class.java)
      .mapToResponse { ResponseEntity.ok(UserDto(it.id, it.username, it.email, it.roles)) }
  }

}
