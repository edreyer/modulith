package io.liquidsoftware.bridgekeeper

class AccessControlQueryException(
  val securedName: String,
  val partyId: String,
) : RuntimeException()

