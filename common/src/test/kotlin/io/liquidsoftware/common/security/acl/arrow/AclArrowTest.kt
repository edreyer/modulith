package io.liquidsoftware.common.security.acl.arrow

import arrow.core.Either
import arrow.core.raise.either
import io.liquidsoftware.common.security.acl.AccessSubject
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.security.acl.AuthorizationError
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.security.acl.PermissionDenied
import io.liquidsoftware.common.security.acl.SecuredResource
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class AclArrowTest {

  private val aclChecker = AclChecker()

  private data class TestResource(
    private val resourceAcl: Acl,
  ) : SecuredResource {
    override fun acl(): Acl = resourceAcl
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
