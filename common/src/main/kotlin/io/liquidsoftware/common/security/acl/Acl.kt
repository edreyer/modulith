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

interface SecuredResource {
  fun acl(): Acl
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

class AclBuilder internal constructor(
  private val resourceId: String,
) {
  private val userRoleMap = linkedMapOf<String, AclRole>()

  fun reader(userId: String) {
    userRoleMap[userId] = AclRole.READER
  }

  fun writer(userId: String) {
    userRoleMap[userId] = AclRole.WRITER
  }

  fun manager(userId: String) {
    userRoleMap[userId] = AclRole.MANAGER
  }

  fun anonymousReader() {
    reader(ExecutionContext.ANONYMOUS_USER_ID)
  }

  fun anonymousWriter() {
    writer(ExecutionContext.ANONYMOUS_USER_ID)
  }

  fun anonymousManager() {
    manager(ExecutionContext.ANONYMOUS_USER_ID)
  }

  internal fun build(): Acl = Acl(
    resourceId = resourceId,
    userRoleMap = userRoleMap.toMap(),
  )
}

fun acl(resourceId: String, init: AclBuilder.() -> Unit): Acl =
  AclBuilder(resourceId).apply(init).build()

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

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanRead(subject: AccessSubject, acl: Acl) {
    ensurePermission(subject, acl, Permission.READ)
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanWrite(subject: AccessSubject, acl: Acl) {
    ensurePermission(subject, acl, Permission.WRITE)
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanManage(subject: AccessSubject, acl: Acl) {
    ensurePermission(subject, acl, Permission.MANAGE)
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanRead(acl: Acl) {
    ensurePermission(acl, Permission.READ)
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanWrite(acl: Acl) {
    ensurePermission(acl, Permission.WRITE)
  }

  context(_: Raise<AuthorizationError>)
  suspend fun ensureCanManage(acl: Acl) {
    ensurePermission(acl, Permission.MANAGE)
  }

  private fun AccessSubject.hasGlobalAccess(): Boolean =
    ROLE_ADMIN in roles || ROLE_SYSTEM in roles
}
