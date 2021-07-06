package ventures.dvx.bridgekeeper

class AccessControlCommandException(
  val securedId: String,
  val partyId: String,
  val securedName: String
  ) : RuntimeException()

