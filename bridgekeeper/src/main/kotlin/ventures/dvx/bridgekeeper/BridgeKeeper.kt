package ventures.dvx.bridgekeeper

import java.util.function.Supplier

open class ResourceType {
  companion object {
    val WILDCARD = ResourceType()
  }
}

enum class Visibility { // careful, sorted by highest visibility.
  HIDDEN,
  PARTIAL,
  VISIBLE;
}

@JvmInline
value class Operation(val operation: String) {
  companion object {
    val ANY = Operation("ANY")
  }
}

class Permission private constructor(val operations: Set<Operation>, private val viewConstraint: Visibility) {

  class Builder(private val viewConstraint: Visibility) {
    private val operations: MutableSet<Operation> = mutableSetOf()
    fun addOperation(operation: Operation) = apply { operations += operation }
    fun build() = Permission(operations.toSet(), viewConstraint)
  }

  fun merge(other: Permission): Permission =
    Permission(operations + other.operations, maxOf(viewConstraint, other.viewConstraint))

  companion object {
    val EMPTY = Permission(setOf(), Visibility.HIDDEN)
  }
}

data class ResourceTypePermission(val resourceType: ResourceType, val permission: Permission)

open class RoleHandle(val name: String)

class Role private constructor(val roleHandle: RoleHandle, private val resourceTypePermissions: List<ResourceTypePermission>) {

  fun permissionFor(rt: ResourceType): Permission = resourceTypePermissions
    .filter { it.resourceType == rt }
    .map { it.permission }
    .firstOrNull() ?: Permission.EMPTY

  class Builder(private val roleHandle: RoleHandle) {
    private val resourceTypePermissions: MutableList<ResourceTypePermission> = mutableListOf()
    fun addResourceTypePermission(resourceTypePermission: ResourceTypePermission) =
      apply { resourceTypePermissions += resourceTypePermission }
    fun build() = Role(roleHandle, resourceTypePermissions.toList())
  }
}

class RolesRegistry private constructor(val roles: List<Role>) {

  fun permissionFor(party: Party, rt: ResourceType): Permission = with(party.roles) {
    permissionFor(this, rt)
      .merge(permissionFor(this, ResourceType.WILDCARD))
  }

  private fun permissionFor(roleHandles: Set<RoleHandle>, rt: ResourceType): Permission =
    roles
      .filter { it.roleHandle.name in roleHandles.map{ rh -> rh.name }}
      .fold(Permission.EMPTY) { acc, cur -> acc.merge(cur.permissionFor(rt)) }

  class Builder {
    private val roles: MutableList<Role> = mutableListOf()
    fun addRole(role: Role) = apply { roles += role }
    fun build() = RolesRegistry(roles.toList())
  }
}

sealed interface Party {
  val id: String
  val roles: Set<RoleHandle>
}

object ANONYMOUS : Party {
  override val roles: Set<RoleHandle> = setOf()
  override val id = "anonymous"
}

data class OrganizationParty(override val id: String, override val roles: Set<RoleHandle>) : Party
data class UserParty(override val id: String, override val roles: Set<RoleHandle>) : Party
data class SystemParty(val role: RoleHandle) : Party {
  override val id = "SYSTEM"
  override val roles = setOf(role)
}

val ROLE_SYSTEM_USER = object : RoleHandle("ROLE_SYSTEM") {}

object ResourceTypes {
  val EMPTY = object : ResourceType() {}
  val SYSTEM = object : ResourceType() {}
}

class BridgeKeeper(private val appPermissions: RolesRegistry) {

  sealed class AssertionResult {
    abstract fun orElse(block: () -> Unit)
    abstract fun orElseThrow(exSupplier: Supplier<Exception>)
  }

  object Failure : AssertionResult() {
    override fun orElse(block: () -> Unit) = block()
    override fun orElseThrow(exSupplier: Supplier<Exception>): Unit = throw exSupplier.get()
  }

  object Success : AssertionResult() {
    override fun orElse(block: () -> Unit) = Unit
    override fun orElseThrow(exSupplier: Supplier<Exception>) = Unit
  }

  fun assertCanPerform(party: Party, rt: ResourceType, cmdCandidate: String): AssertionResult {
    val permission = appPermissions.permissionFor(party, rt)
    if (Operation(cmdCandidate) !in permission.operations && Operation.ANY !in permission.operations) {
      return Failure
    }
    return Success
  }
}
