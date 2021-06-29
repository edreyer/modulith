import ventures.dvx.bridgekeeper.*

@DslMarker
annotation class PermissionsMarker

@PermissionsMarker
fun rolesPermissionRegistry(init: CreateRolesRegistry.() -> Unit): RolesRegistry {
    var block = CreateRolesRegistry(RolesRegistry.Builder())
    block.init()
    return block.getRolesRegistry()
}

@PermissionsMarker
class CreateRolesRegistry(private val builder: RolesRegistry.Builder) {

    fun role(roleHandle: RoleHandle, init: CreateRole.() -> Unit): CreateRole {
        var block = CreateRole(Role.Builder(roleHandle))
        block.init()
        builder.addRole(block.getRole())
        return block
    }

    fun getRolesRegistry(): RolesRegistry {
        return builder.build()
    }
}

@PermissionsMarker
class CreateRole(private val builder: Role.Builder) {

    fun getRole():Role {
        return builder.build()
    }

    fun resourceType(resourceType: ResourceType, init: AddPermission.() -> Unit): AddPermission {
        var block = AddPermission()
        block.init()
        builder.addResourceTypePermission(ventures.dvx.bridgekeeper.ResourceTypePermission.Builder(resourceType, block.getPermission()).build())
        return block
    }
}

@PermissionsMarker
class AddPermission() {

    private var operations: Set<Operation> = mutableSetOf()

    var visibility = Visibility.HIDDEN

    fun getPermission(): Permission {
        var builder = Permission.Builder(visibility)
        operations.map { builder.addOperation(it) }
        return builder.build()
    }

    fun operations(init: AddOperation.() -> Unit): AddOperation {
        var op = AddOperation()
        op.init()
        operations = op.getOperations()
        return op
    }
}

@PermissionsMarker
class AddOperation {

    private var operations: MutableSet<Operation> = mutableSetOf()

    fun getOperations():Set<Operation> = operations

    operator fun Any.unaryPlus() {
        operations += Operation(this)
    }
}


// ---------------------------------
// Example app usage: start
// ---------------------------------
object MY_APPOINTMENT : ResourceType()
class FakeCommand1
class FakeCommand2
class FakeCommand3

object MyAppRoles {
    val ADMIN = object: RoleHandle("Admin") {}
    val END_USER = object: RoleHandle("Admin") {}
}
// ---------------------------------
// Example app usage: end
// ---------------------------------

fun main() {

    val bridgeKeeper = BridgeKeeper(rolesPermissionRegistry {
        role(MyAppRoles.ADMIN) {
            resourceType(MY_APPOINTMENT) {
                visibility = Visibility.VISIBLE
                operations {
                    +FakeCommand1::class
                    +FakeCommand2::class
                    +FakeCommand3::class
                }
            }
        }
        })

    val user = User("userA", setOf(MyAppRoles.ADMIN))
    bridgeKeeper.assertCanPerform(user, MY_APPOINTMENT, FakeCommand1::class)
        .orElseThrow { NullPointerException() }

    bridgeKeeper.assertCanPerform(ANONYMOUS, MY_APPOINTMENT, FakeCommand1::class)
        .orElseThrow { NullPointerException() }
}



/***
 *    A_ROLE coming from securityContext,  RT coming from the establisher in the aggregate
 *    Roles PartyContext.authenticatedParty.getRoles()
  *
 *   *
 *    appPermission.permissionsFor(list<Role>, rt):
 *
 *
 */
