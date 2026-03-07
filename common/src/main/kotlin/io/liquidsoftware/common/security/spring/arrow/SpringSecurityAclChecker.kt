package io.liquidsoftware.common.security.spring.arrow

import arrow.core.raise.Raise
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.AuthorizationError
import io.liquidsoftware.common.security.acl.arrow.ensureCanManage
import io.liquidsoftware.common.security.acl.arrow.ensureCanRead
import io.liquidsoftware.common.security.acl.arrow.ensureCanWrite
import io.liquidsoftware.common.security.spring.SpringSecurityAccessSubjectProvider
import org.springframework.stereotype.Component

@Component
class SpringSecurityAclChecker(
  private val accessSubjectProvider: SpringSecurityAccessSubjectProvider,
) {
  private val aclChecker = AclChecker()

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanRead(acl: Acl) {
    aclChecker.ensureCanRead(accessSubjectProvider.currentSubject(), acl)
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanWrite(acl: Acl) {
    aclChecker.ensureCanWrite(accessSubjectProvider.currentSubject(), acl)
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanManage(acl: Acl) {
    aclChecker.ensureCanManage(accessSubjectProvider.currentSubject(), acl)
  }
}
