package io.liquidsoftware.base.user.adapter.out.persistence

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isTrue
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.security.acl.AclChecker
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import java.lang.reflect.Proxy

class UserPersistenceAdapterArrowContractTest {

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `findUserById returns left when acl check fails`() = runBlocking {
    val entity = UserEntity(
      userId = "u_test-user",
      msisdn = "+15125551212",
      email = "user@example.com",
      password = "encoded-password",
      roles = mutableListOf(Role.ROLE_USER)
    )
    val adapter = UserPersistenceAdapter(
      userRepository(findByUserId = { userId -> if (userId == entity.userId) entity else null }),
      AclChecker(ExecutionContext())
    )

    val result = adapter.findUserById(entity.userId)

    assertThat(result is Either.Left).isTrue()
  }

  @Test
  fun `findUserById returns left when repository fails`() = runBlocking {
    val adapter = UserPersistenceAdapter(
      userRepository(findByUserId = { throw DataAccessResourceFailureException("db down") }),
      AclChecker(ExecutionContext())
    )

    val result = adapter.findUserById("u_test-user")

    assertThat(result is Either.Left).isTrue()
  }

  @Test
  fun `handle user registered event returns left when acl check fails`() = runBlocking {
    val adapter = UserPersistenceAdapter(
      userRepository(
        findByUserId = { null },
        save = { it }
      ),
      AclChecker(ExecutionContext())
    )

    val result = adapter.handle(registeredUserEvent())

    assertThat(result is Either.Left).isTrue()
  }

  @Test
  fun `handle user registered event returns left when repository save fails`() = runBlocking {
    authenticate("SYSTEM", "ROLE_SYSTEM")
    val adapter = UserPersistenceAdapter(
      userRepository(
        findByUserId = { null },
        save = { throw DataAccessResourceFailureException("db down") }
      ),
      AclChecker(ExecutionContext())
    )

    val result = adapter.handle(registeredUserEvent())

    assertThat(result is Either.Left).isTrue()
  }

  private fun authenticate(userId: String, role: String) {
    val principal = UserDetailsWithId(
      userId,
      User(userId, "password", listOf(SimpleGrantedAuthority(role)))
    )
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
  }

  private fun registeredUserEvent() = UserRegisteredEvent(
    userDto = UserDto(
      id = "u_test-user",
      email = "user@example.com",
      msisdn = "+15125551212",
      active = true,
      roles = listOf(RoleDto.ROLE_USER)
    ),
    password = "encoded-password"
  )

  private fun userRepository(
    findByUserId: (String) -> UserEntity?,
    save: (UserEntity) -> UserEntity = { it }
  ): UserRepository =
    Proxy.newProxyInstance(
      UserRepository::class.java.classLoader,
      arrayOf(UserRepository::class.java)
    ) { _, method, args ->
      when (method.name) {
        "findByUserId" -> findByUserId(args[0] as String)
        "findByEmail", "findByMsisdn" -> null
        "save" -> save(args[0] as UserEntity)
        "toString" -> "UserRepositoryProxy"
        "hashCode" -> System.identityHashCode(this)
        "equals" -> false
        else -> error("Unexpected repository method: ${method.name}")
      }
    } as UserRepository
}
