package io.liquidsoftware.common.security.acl

import arrow.core.Either
import arrow.core.raise.either
import io.liquidsoftware.common.security.ExecutionContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AclCheckerTest {

  private val aclChecker = AclChecker(ExecutionContext())

  @Test
  fun `manager can manage owned resource`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.MANAGER)
    val subject = AccessSubject("user-1", emptySet())

    assertTrue(aclChecker.hasPermission(acl, subject, Permission.MANAGE))
  }

  @Test
  fun `reader cannot write owned resource`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.READER)
    val subject = AccessSubject("user-1", emptySet())

    assertFalse(aclChecker.hasPermission(acl, subject, Permission.WRITE))
  }

  @Test
  fun `anonymous subject can read resource with anonymous reader access`() = runBlocking {
    val acl = Acl(
      resourceId = "appointment-1",
      userRoleMap = mapOf(ExecutionContext.ANONYMOUS_USER_ID to AclRole.READER),
    )
    val subject = AccessSubject(ExecutionContext.ANONYMOUS_USER_ID, emptySet())

    assertTrue(aclChecker.hasPermission(acl, subject, Permission.READ))
  }

  @Test
  fun `admin bypass grants access`() = runBlocking {
    val acl = Acl.of("appointment-1", "someone-else", AclRole.READER)
    val subject = AccessSubject("admin-user", setOf(AclChecker.ROLE_ADMIN))

    assertTrue(aclChecker.hasPermission(acl, subject, Permission.MANAGE))
  }

  @Test
  fun `ensurePermission raises PermissionDenied for denied access`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.READER)
    val subject = AccessSubject("user-1", emptySet())

    val result = either<AuthorizationError, Unit> {
      aclChecker.ensurePermission(subject, acl, Permission.WRITE)
    }

    val error = (result as Either.Left).value
    assertInstanceOf(PermissionDenied::class.java, error)
    assertEquals("appointment-1", error.resourceId)
    assertEquals(Permission.WRITE, error.permission)
    assertEquals("user-1", error.subjectId)
  }
}
