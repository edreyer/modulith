package io.liquidsoftware.common.security.acl

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure

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

const val ANONYMOUS_SUBJECT_ID = "u_anonymous"

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
    reader(ANONYMOUS_SUBJECT_ID)
  }

  fun anonymousWriter() {
    writer(ANONYMOUS_SUBJECT_ID)
  }

  fun anonymousManager() {
    manager(ANONYMOUS_SUBJECT_ID)
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

class AclChecker(
  private val anonymousSubjectId: String = ANONYMOUS_SUBJECT_ID,
  private val globalRoles: Set<String> = setOf(ROLE_ADMIN, ROLE_SYSTEM),
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

  suspend fun hasPermission(acl: Acl, subject: AccessSubject, permission: Permission): Boolean {
    return if (subject.hasGlobalAccess()) {
      true
    } else {
      val role = acl.userRoleMap[subject.userId]
        ?: acl.userRoleMap[anonymousSubjectId]
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

  private fun AccessSubject.hasGlobalAccess(): Boolean =
    roles.any(globalRoles::contains)
}
