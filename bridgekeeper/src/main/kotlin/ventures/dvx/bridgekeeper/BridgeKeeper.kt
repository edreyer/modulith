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
value class Operation(val operation : Any) {
    companion object {
        val ANY = Operation("ANY")
    }
}

class Permission private constructor(val operations: Set<Operation>, val viewConstraint: Visibility) {

    class Builder(private val viewConstraint: Visibility) {

        private val operations: MutableSet<Operation> = mutableSetOf()

        fun addOperation(operation: Operation) = apply { operations += operation }

        fun build() = Permission(operations.toSet(), viewConstraint)
    }

    fun merge(other: Permission): Permission =
        Permission(operations + other.operations, maxOf(viewConstraint, other.viewConstraint))

    companion object {
        val EMPTY = Permission(setOf(),Visibility.HIDDEN)
    }
}

class ResourceTypePermission private constructor(val resourceType: ResourceType, val permission: Permission) {

    class Builder (private val resourceType: ResourceType, private val permission: Permission) {
        fun build() = ResourceTypePermission(resourceType, permission)
    }
}


open class RoleHandle(val name: String)

class Role private constructor(val roleHandle: RoleHandle, val resourceTypePermissions: List<ResourceTypePermission>) {

    fun permissionFor(rt: ResourceType) : Permission = resourceTypePermissions
          .filter { it.resourceType == rt }
          .map { it.permission }
          .firstOrNull() ?: Permission.EMPTY

    class Builder(private val roleHandle: RoleHandle) {

        private val resourceTypePermissions : MutableList<ResourceTypePermission> = mutableListOf()

        fun addResourceTypePermission(resourceTypePermission: ResourceTypePermission) = apply { resourceTypePermissions += resourceTypePermission }

        fun build() = Role(roleHandle, resourceTypePermissions.toList())
    }
}

class RolesRegistry private constructor(val roles: List<Role>) {


    fun permissionFor(party: Party, rt: ResourceType): Permission = with(party.roles) {
        permissionFor(this, rt)
            .merge(
                permissionFor(this, ResourceType.WILDCARD))
    }

    private fun permissionFor(roleHandles: Set<RoleHandle>, rt: ResourceType): Permission =
        roles
            .filter { roleHandles.contains(it.roleHandle) }
            .fold(Permission.EMPTY) { acc, cur -> acc.merge(cur.permissionFor(rt)) }

    class Builder() {
        private val roles: MutableList<Role> = mutableListOf()

        fun addRole(role: Role) = apply { roles += role }
        fun build() = RolesRegistry(roles.toList())
    }
}

sealed class Party {
    abstract val id: String
    abstract val roles: Set<RoleHandle>
}

object ANONYMOUS : Party() {
    override val roles: Set<RoleHandle> = setOf()
    override val id =  "anonymous"
}

interface PartyContext {
    val authenticatedParty : Party

    companion object { //anonymous is the party... the partycontext is anonymousPartyContext?
        val partyContext: ThreadLocal<PartyContext> = ThreadLocal.withInitial { PartyContext.ANONYMOUS_PARTY_CONTEXT }
        private val ANONYMOUS_PARTY_CONTEXT :PartyContext = object: PartyContext {
            override val authenticatedParty = ANONYMOUS
        }

        fun get(): PartyContext {
            return partyContext.get()
        }
    }

}

class BridgeKeeper(private val appPermissions: RolesRegistry) {

    sealed class AssertionResult {
        abstract fun orElse(block: () -> Unit)
        abstract fun orElseThrow(exSupplier: Supplier<Exception>)
    }
    object Failure : AssertionResult() {
        override fun orElse(block: () -> Unit) {
            block()
        }
        override fun orElseThrow(exSupplier: Supplier<Exception>) {
            throw exSupplier.get()
        }
    }
    object Success : AssertionResult() {
        override fun orElse(block: () -> Unit) {
            block()
        }

        override fun orElseThrow(exSupplier: Supplier<Exception>) {
        }
    }


    fun assertCanPerform(party: Party, rt: ResourceType, cmdCandidate: Any):AssertionResult {
        val permission = appPermissions.permissionFor(party, rt);
        if (!permission.operations.contains(Operation(cmdCandidate)) && !permission.operations.contains(Operation.ANY)) {
            return Failure
        }
        return Success
    }
}






data class Organization(override val id: String, override val roles: Set<RoleHandle>) : Party()
data class User(override val id: String, override val roles: Set<RoleHandle>) : Party()
data class System(override val id: String, override val roles: Set<RoleHandle>) : Party()

/****
*    role     resource type,   commandset, viewConstraint
*    EndUser, ADMIN, {}, hidden
 *
 *   EndUser, MY_APPOINTMENT, {Schedule, Cancel, Reschedule}, visible
 *   EndUser, NOT_MY_APPOINTMENT, {}, invisible
*
*/

/** example with rest service **/
/**
 *    resource = the url
 *    resource type = Service Name
 *    commands = RestVerbs
 *    Visibility = Visibility when GET
 *
 *    idea:  url dispatcher/servlet dispatcher  map url to service
 *    automatically create the service from the url using that
 *
 *    automatically check if the service can be invoked with the requested http method
 *    when a GET, apply the object
 *
 *
 */
enum class RestCommand {
    GET,
    PUT,
    POST,
    DELETE
}
