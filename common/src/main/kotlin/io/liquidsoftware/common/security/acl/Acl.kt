package io.liquidsoftware.common.security.acl

import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.UnauthorizedAccessException
import org.springframework.stereotype.Component

enum class AclRole {
  READER, WRITER, MANAGER
}

enum class Permission {
  READ, WRITE, MANAGE
}

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

  suspend fun checkPermission(acl: Acl, permission: Permission) {
    if (!hasPermission(acl, ec.getUserAccessKeys(), permission)) {
      throw UnauthorizedAccessException(
        "No access to: ${acl.resourceId} Permission: $permission"
      )
    }
  }

  suspend fun hasPermission(acl: Acl, access: List<String>, permission: Permission): Boolean {
    return if (access.contains(ROLE_ADMIN) || access.contains(ROLE_SYSTEM)) {
      true
    } else {
      val role = acl.userRoleMap[access[0]]
        ?: acl.userRoleMap[ExecutionContext.ANONYMOUS_USER_ID]
      val hasPermission: Boolean = role.let {
        rolePermissions[it]?.contains(permission) ?: false
      }
      hasPermission
    }
  }
}
