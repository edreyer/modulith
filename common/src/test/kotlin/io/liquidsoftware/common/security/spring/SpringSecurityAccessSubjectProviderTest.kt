package io.liquidsoftware.common.security.spring

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.security.acl.ANONYMOUS_SUBJECT_ID
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.security.acl.Permission
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

class SpringSecurityAccessSubjectProviderTest {

  private val provider = SpringSecurityAccessSubjectProvider(ExecutionContext())

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `currentSubject returns anonymous subject when no authentication exists`() {
    val subject = provider.currentSubject()

    assertThat(subject.userId).isEqualTo(ANONYMOUS_SUBJECT_ID)
    assertThat(subject.roles).isEqualTo(emptySet())
  }

  @Test
  fun `currentSubject returns authenticated user id and roles`() {
    authenticate("u_test-user", "ROLE_USER", "ROLE_ADMIN")

    val subject = provider.currentSubject()

    assertThat(subject.userId).isEqualTo("u_test-user")
    assertThat(subject.roles).contains("ROLE_USER")
    assertThat(subject.roles).contains("ROLE_ADMIN")
  }

  @Test
  fun `hasPermission uses current subject`() = runBlocking {
    authenticate("u_test-user", "ROLE_USER")
    val acl = Acl.of("a_test", "u_test-user", AclRole.READER)

    val result = provider.hasPermission(acl, Permission.READ)

    assertThat(result).isEqualTo(true)
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
