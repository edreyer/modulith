package io.liquidsoftware.common.security.acl

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

data class Acl(
  val resourceId: String,
  val userRoleMap: Map<String, AclRole>,
) {
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

class AclChecker(
  private val anonymousSubjectId: String = ANONYMOUS_SUBJECT_ID,
  private val globalRoles: Set<String> = setOf(ROLE_ADMIN, ROLE_SYSTEM),
) {

  private val rolePermissions: Map<AclRole, Set<Permission>> = mapOf(
    AclRole.READER to setOf(Permission.READ),
    AclRole.WRITER to setOf(Permission.READ, Permission.WRITE),
    AclRole.MANAGER to setOf(Permission.READ, Permission.WRITE, Permission.MANAGE),
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

  private fun AccessSubject.hasGlobalAccess(): Boolean =
    roles.any(globalRoles::contains)
}
