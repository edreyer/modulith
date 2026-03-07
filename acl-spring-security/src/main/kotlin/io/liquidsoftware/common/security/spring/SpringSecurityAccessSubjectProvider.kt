package io.liquidsoftware.common.security.spring

import io.liquidsoftware.common.security.acl.AccessSubject
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.Permission
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SpringSecurityAccessSubjectProvider(
  private val resolver: AuthenticationAccessSubjectResolver,
) {
  private val aclChecker = AclChecker()

  fun currentSubject(): AccessSubject =
    resolver.resolve(SecurityContextHolder.getContext().authentication)

  suspend fun hasPermission(acl: Acl, subject: AccessSubject, permission: Permission): Boolean =
    aclChecker.hasPermission(acl, subject, permission)

  suspend fun hasPermission(acl: Acl, permission: Permission): Boolean =
    hasPermission(acl, currentSubject(), permission)
}
