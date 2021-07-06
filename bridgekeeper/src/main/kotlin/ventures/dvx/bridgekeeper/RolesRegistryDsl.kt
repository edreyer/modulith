import ventures.dvx.bridgekeeper.Operation
import ventures.dvx.bridgekeeper.Permission
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.ResourceTypePermission
import ventures.dvx.bridgekeeper.Role
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.RolesRegistry
import ventures.dvx.bridgekeeper.Visibility

@DslMarker
annotation class PermissionsMarker

@PermissionsMarker
fun rolesPermissionRegistry(init: CreateRolesRegistry.() -> Unit): RolesRegistry =
  CreateRolesRegistry(RolesRegistry.Builder())
    .also { it.init() }
    .getRolesRegistry()

@PermissionsMarker
class CreateRolesRegistry(private val builder: RolesRegistry.Builder) {

  fun role(roleHandle: RoleHandle, init: CreateRole.() -> Unit): CreateRole =
    CreateRole(Role.Builder(roleHandle))
    .also { it.init() }
    .also { builder.addRole(it.getRole()) }

  fun getRolesRegistry(): RolesRegistry = builder.build()
}

@PermissionsMarker
class CreateRole(private val builder: Role.Builder) {

  fun getRole(): Role = builder.build()

  fun resourceType(resourceType: ResourceType, init: AddPermission.() -> Unit): AddPermission =
    AddPermission()
    .also { it.init() }
    .also { ResourceTypePermission(resourceType, it.getPermission())
      .let { rtp -> builder.addResourceTypePermission(rtp) }
    }
}

@PermissionsMarker
class AddPermission {

  private var operations: Set<Operation> = mutableSetOf()

  var visibility = Visibility.HIDDEN

  fun getPermission(): Permission = visibility
    .let { Permission.Builder(it) }
    .also { builder -> operations.map { builder.addOperation(it) } }
    .build()

  fun operations(init: AddOperation.() -> Unit): AddOperation {
    val op = AddOperation()
      .also { it.init() }
    operations = op.getOperations()
    return op
  }
}

@PermissionsMarker
class AddOperation {

  private var operations: MutableSet<Operation> = mutableSetOf()

  fun getOperations(): Set<Operation> = operations

  operator fun String.unaryPlus(): String {
    operations += Operation(this)
    return this
  }
}
