package io.liquidsoftware.common.security.spring.arrow

import arrow.core.Either
import arrow.core.raise.either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.security.acl.AuthorizationError
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.security.acl.PermissionDenied
import io.liquidsoftware.common.security.spring.SpringSecurityAccessSubjectProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

class SpringSecurityAclCheckerTest {

  private val checker = SpringSecurityAclChecker(SpringSecurityAccessSubjectProvider(ExecutionContext()))

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `ensureCanRead uses current subject and succeeds for allowed access`() = runBlocking {
    authenticate("u_test-user", "ROLE_USER")
    val acl = Acl.of("a_test", "u_test-user", AclRole.READER)

    val result = either<AuthorizationError, Unit> {
      checker.ensureCanRead(acl)
    }

    assertThat(result is Either.Right).isEqualTo(true)
  }

  @Test
  fun `ensureCanWrite uses current subject and raises PermissionDenied for denied access`() = runBlocking {
    authenticate("u_test-user", "ROLE_USER")
    val acl = Acl.of("a_test", "u_test-user", AclRole.READER)

    val result = either<AuthorizationError, Unit> {
      checker.ensureCanWrite(acl)
    }

    val error = (result as Either.Left).value
    assertThat(error).isInstanceOf(PermissionDenied::class)
    assertThat(error.resourceId).isEqualTo("a_test")
    assertThat(error.permission).isEqualTo(Permission.WRITE)
    assertThat(error.subjectId).isEqualTo("u_test-user")
  }

  private fun authenticate(userId: String, vararg roles: String) {
    val authorities = roles.map(::SimpleGrantedAuthority)
    val principal = UserDetailsWithId(
      userId,
      User("$userId@example.com", "", authorities)
    )
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
  }
}
