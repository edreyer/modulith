package io.liquidsoftware.common.security.acl

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import io.liquidsoftware.common.security.ExecutionContext
import org.springframework.stereotype.Component

enum class AclRole {
  READER, WRITER, MANAGER
}

enum class Permission {
  READ, WRITE, MANAGE
}

sealed interface AuthorizationError {
  val resourceId: String
  val permission: Permission
  val subjectId: String
}

data class PermissionDenied(
  override val resourceId: String,
  override val permission: Permission,
  override val subjectId: String,
) : AuthorizationError

data class AccessSubject(
  val userId: String,
  val roles: Set<String>,
)

/**
 * Attribute Based Access Control utility
 */
data class Acl(
  val resourceId: String,
  val userRoleMap: Map<String, AclRole>,
){
  companion object {
    fun of(resourceId: String, userId: String, role: AclRole) =
      Acl(resourceId, mapOf(userId to role))
  }
}

@Component
class AclChecker(
  val ec: ExecutionContext,
) {

  private val rolePermissions: Map<AclRole, Set<Permission>> = mapOf(
    AclRole.READER to setOf(Permission.READ),
    AclRole.WRITER to setOf(Permission.READ, Permission.WRITE),
    AclRole.MANAGER to setOf(Permission.READ, Permission.WRITE, Permission.MANAGE)
  )

  companion object {
    const val ROLE_SYSTEM = "ROLE_SYSTEM"
    const val ROLE_ADMIN = "ROLE_ADMIN"
  }

  fun currentSubject(): AccessSubject = ec.getAccessSubject()

  suspend fun hasPermission(acl: Acl, subject: AccessSubject, permission: Permission): Boolean {
    return if (subject.hasGlobalAccess()) {
      true
    } else {
      val role = acl.userRoleMap[subject.userId]
        ?: acl.userRoleMap[ExecutionContext.ANONYMOUS_USER_ID]
      rolePermissions[role]?.contains(permission) ?: false
    }
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensurePermission(subject: AccessSubject, acl: Acl, permission: Permission) {
    ensure (hasPermission(acl, subject, permission)) {
      PermissionDenied(
        resourceId = acl.resourceId,
        permission = permission,
        subjectId = subject.userId,
      )
    }
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensurePermission(acl: Acl, permission: Permission) {
    ensurePermission(currentSubject(), acl, permission)
  }

  private fun AccessSubject.hasGlobalAccess(): Boolean =
    ROLE_ADMIN in roles || ROLE_SYSTEM in roles
}
