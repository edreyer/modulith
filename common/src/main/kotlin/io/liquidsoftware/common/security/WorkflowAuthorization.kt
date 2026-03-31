package io.liquidsoftware.common.security

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import arrow.core.raise.context.raise
import arrow.core.raise.either
import io.liquidsoftware.common.ext.toWorkflowError
import io.liquidsoftware.common.security.acl.AccessDenied
import io.liquidsoftware.common.security.acl.AccessSubject
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.AuthorizationError
import io.liquidsoftware.common.security.acl.DenialContext
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.security.acl.SecuredResource
import io.liquidsoftware.common.security.acl.authorizer
import io.liquidsoftware.common.security.acl.arrow.ensureCanManage
import io.liquidsoftware.common.security.acl.arrow.ensureCanRead
import io.liquidsoftware.common.security.acl.arrow.ensureCanWrite
import io.liquidsoftware.common.security.spring.SpringSecurityAccessSubjectProvider
import io.liquidsoftware.common.workflow.WorkflowError

private val aclChecker = AclChecker()

private val securedResourceAccess = authorizer<AccessSubject, SecuredResource> {
  canManage { subject, resource ->
    aclChecker.hasPermission(resource.acl(), subject, Permission.MANAGE)
  }
  canWrite { subject, resource ->
    aclChecker.hasPermission(resource.acl(), subject, Permission.WRITE)
  }
  canRead { subject, resource ->
    aclChecker.hasPermission(resource.acl(), subject, Permission.READ)
  }
}

context(_: Raise<WorkflowError>)
suspend fun SpringSecurityAccessSubjectProvider.ensureCurrentUserCanRead(acl: Acl) {
  either {
    aclChecker.ensureCanRead(currentSubject(), acl)
  }.fold(
    { raise(it.toWorkflowError()) },
    {},
  )
}

context(_: Raise<WorkflowError>)
suspend fun SpringSecurityAccessSubjectProvider.ensureCurrentUserCanWrite(acl: Acl) {
  either {
    aclChecker.ensureCanWrite(currentSubject(), acl)
  }.fold(
    { raise(it.toWorkflowError()) },
    {},
  )
}

context(_: Raise<WorkflowError>)
suspend fun SpringSecurityAccessSubjectProvider.ensureCurrentUserCanManage(acl: Acl) {
  either {
    aclChecker.ensureCanManage(currentSubject(), acl)
  }.fold(
    { raise(it.toWorkflowError()) },
    {},
  )
}

context(_: Raise<WorkflowError>)
suspend fun SpringSecurityAccessSubjectProvider.ensureCurrentUserCanRead(resource: SecuredResource) {
  ensureCurrentUserHasAccess(resource, Permission.READ)
}

context(_: Raise<WorkflowError>)
suspend fun SpringSecurityAccessSubjectProvider.ensureCurrentUserCanWrite(resource: SecuredResource) {
  ensureCurrentUserHasAccess(resource, Permission.WRITE)
}

context(_: Raise<WorkflowError>)
suspend fun SpringSecurityAccessSubjectProvider.ensureCurrentUserCanManage(resource: SecuredResource) {
  ensureCurrentUserHasAccess(resource, Permission.MANAGE)
}

context(_: Raise<WorkflowError>)
private suspend fun SpringSecurityAccessSubjectProvider.ensureCurrentUserHasAccess(
  resource: SecuredResource,
  permission: Permission,
) {
  val subject = currentSubject()
  either<AuthorizationError, Unit> {
    ensure(securedResourceAccess.hasAccess(subject, resource, permission)) {
      AccessDenied(
        permission = permission,
        context = DenialContext.Acl(
          resourceId = resource.acl().resourceId,
          subjectId = subject.userId,
        ),
      )
    }
  }.fold(
    { raise(it.toWorkflowError()) },
    {},
  )
}
