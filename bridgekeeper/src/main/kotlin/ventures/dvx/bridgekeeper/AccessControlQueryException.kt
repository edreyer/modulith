package ventures.dvx.bridgekeeper

class AccessControlQueryException(
  val securedName: String,
  val partyId: String,
) : RuntimeException()

