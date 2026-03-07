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

  private data class TestResource(
    private val resourceAcl: Acl,
  ) : SecuredResource {
    override fun acl(): Acl = resourceAcl
  }

  @Test
  fun `manager can manage owned resource`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.MANAGER)
    val subject = AccessSubject("user-1", emptySet())

    assertTrue(aclChecker.hasPermission(acl, subject, Permission.MANAGE))
  }

  @Test
  fun `acl builder creates same acl as hand-built map`() {
    val dslAcl = acl("appointment-1") {
      manager("user-1")
      reader("assistant-1")
    }

    val handBuiltAcl = Acl(
      resourceId = "appointment-1",
      userRoleMap = mapOf(
        "user-1" to AclRole.MANAGER,
        "assistant-1" to AclRole.READER,
      ),
    )

    assertEquals(handBuiltAcl, dslAcl)
  }

  @Test
  fun `acl builder supports anonymous access`() {
    val acl = acl("appointment-1") {
      manager("user-1")
      anonymousReader()
    }

    assertEquals(AclRole.READER, acl.userRoleMap[ExecutionContext.ANONYMOUS_USER_ID])
    assertEquals(AclRole.MANAGER, acl.userRoleMap["user-1"])
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
  fun `ensureCanRead allows reader access`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.READER)
    val subject = AccessSubject("user-1", emptySet())

    val result = either<AuthorizationError, Unit> {
      aclChecker.ensureCanRead(subject, acl)
    }

    assertInstanceOf(Either.Right::class.java, result)
    Unit
  }

  @Test
  fun `ensureCanWrite raises PermissionDenied for denied access`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.READER)
    val subject = AccessSubject("user-1", emptySet())

    val result = either<AuthorizationError, Unit> {
      aclChecker.ensureCanWrite(subject, acl)
    }

    val error = (result as Either.Left).value
    assertInstanceOf(PermissionDenied::class.java, error)
    assertEquals("appointment-1", error.resourceId)
    assertEquals(Permission.WRITE, error.permission)
    assertEquals("user-1", error.subjectId)
  }

  @Test
  fun `ensureCanManage allows manager access`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.MANAGER)
    val subject = AccessSubject("user-1", emptySet())

    val result = either {
      aclChecker.ensureCanManage(subject, acl)
    }

    assertInstanceOf(Either.Right::class.java, result)
    Unit
  }

  @Test
  fun `subject extension ensureCanRead allows reader access`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.READER)
    val subject = AccessSubject("user-1", emptySet())

    val result = with(aclChecker) {
      either<AuthorizationError, Unit> {
        subject.ensureCanRead(acl)
      }
    }

    assertInstanceOf(Either.Right::class.java, result)
    Unit
  }

  @Test
  fun `subject extension ensureCanWrite raises PermissionDenied for denied access`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.READER)
    val subject = AccessSubject("user-1", emptySet())

    val result = with(aclChecker) {
      either {
        subject.ensureCanWrite(acl)
      }
    }

    val error = (result as Either.Left).value
    assertInstanceOf(PermissionDenied::class.java, error)
    assertEquals("appointment-1", error.resourceId)
    assertEquals(Permission.WRITE, error.permission)
    assertEquals("user-1", error.subjectId)
  }

  @Test
  fun `subject extension ensureCanManage allows manager access`() = runBlocking {
    val acl = Acl.of("appointment-1", "user-1", AclRole.MANAGER)
    val subject = AccessSubject("user-1", emptySet())

    val result = with(aclChecker) {
      either {
        subject.ensureCanManage(acl)
      }
    }

    assertInstanceOf(Either.Right::class.java, result)
    Unit
  }

  @Test
  fun `subject extension ensureCanRead supports secured resources`() = runBlocking {
    val subject = AccessSubject("user-1", emptySet())
    val resource = TestResource(Acl.of("appointment-1", "user-1", AclRole.READER))

    val result = with(aclChecker) {
      either {
        subject.ensureCanRead(resource)
      }
    }

    assertInstanceOf(Either.Right::class.java, result)
    Unit
  }

  @Test
  fun `subject extension ensureCanWrite raises PermissionDenied for secured resources`() = runBlocking {
    val subject = AccessSubject("user-1", emptySet())
    val resource = TestResource(Acl.of("appointment-1", "user-1", AclRole.READER))

    val result = with(aclChecker) {
      either {
        subject.ensureCanWrite(resource)
      }
    }

    val error = (result as Either.Left).value
    assertInstanceOf(PermissionDenied::class.java, error)
    assertEquals("appointment-1", error.resourceId)
    assertEquals(Permission.WRITE, error.permission)
    assertEquals("user-1", error.subjectId)
  }
}
