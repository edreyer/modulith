package io.liquidsoftware.common.security.spring

import arrow.core.raise.Raise
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.acl.AccessSubject
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.AuthorizationError
import org.springframework.stereotype.Component

@Component
class SpringSecurityAclChecker(
  private val executionContext: ExecutionContext,
) {
  private val aclChecker = AclChecker()

  fun currentSubject(): AccessSubject = executionContext.getAccessSubject()

  suspend fun hasPermission(acl: Acl, subject: AccessSubject, permission: io.liquidsoftware.common.security.acl.Permission): Boolean =
    aclChecker.hasPermission(acl, subject, permission)

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanRead(acl: Acl) {
    aclChecker.ensureCanRead(currentSubject(), acl)
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanWrite(acl: Acl) {
    aclChecker.ensureCanWrite(currentSubject(), acl)
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanManage(acl: Acl) {
    aclChecker.ensureCanManage(currentSubject(), acl)
  }
}
