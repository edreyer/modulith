package io.liquidsoftware.common.security.acl.arrow

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import io.liquidsoftware.common.security.acl.AccessSubject
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.AuthorizationError
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.security.acl.PermissionDenied
import io.liquidsoftware.common.security.acl.SecuredResource

context(_: Raise<AuthorizationError>)
suspend fun AclChecker.ensurePermission(subject: AccessSubject, acl: Acl, permission: Permission) {
  ensure(hasPermission(acl, subject, permission)) {
    PermissionDenied(
      resourceId = acl.resourceId,
      permission = permission,
      subjectId = subject.userId,
    )
  }
}

context(_: Raise<AuthorizationError>)
suspend fun AclChecker.ensureCanRead(subject: AccessSubject, acl: Acl) {
  ensurePermission(subject, acl, Permission.READ)
}

context(_: Raise<AuthorizationError>)
suspend fun AclChecker.ensureCanWrite(subject: AccessSubject, acl: Acl) {
  ensurePermission(subject, acl, Permission.WRITE)
}

context(_: Raise<AuthorizationError>)
suspend fun AclChecker.ensureCanManage(subject: AccessSubject, acl: Acl) {
  ensurePermission(subject, acl, Permission.MANAGE)
}

context(ac: AclChecker, _: Raise<AuthorizationError>)
suspend fun AccessSubject.ensureCanRead(acl: Acl) {
  ac.ensureCanRead(this, acl)
}

context(ac: AclChecker, _: Raise<AuthorizationError>)
suspend fun AccessSubject.ensureCanRead(resource: SecuredResource) {
  ensureCanRead(resource.acl())
}

context(ac: AclChecker, _: Raise<AuthorizationError>)
suspend fun AccessSubject.ensureCanWrite(acl: Acl) {
  ac.ensureCanWrite(this, acl)
}

context(ac: AclChecker, _: Raise<AuthorizationError>)
suspend fun AccessSubject.ensureCanWrite(resource: SecuredResource) {
  ensureCanWrite(resource.acl())
}

context(ac: AclChecker, _: Raise<AuthorizationError>)
suspend fun AccessSubject.ensureCanManage(acl: Acl) {
  ac.ensureCanManage(this, acl)
}

context(ac: AclChecker, _: Raise<AuthorizationError>)
suspend fun AccessSubject.ensureCanManage(resource: SecuredResource) {
  ensureCanManage(resource.acl())
}
