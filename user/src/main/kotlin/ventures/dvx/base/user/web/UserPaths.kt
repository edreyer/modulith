package ventures.dvx.base.user.web

internal object UserPaths {
  const val API = "/api"
  const val V1 = API + "/v1"

  const val USERS = V1 + "/users"
  const val USER_BY_ID = USERS + "/{userId}"
  const val USER_BY_USERNAME = USERS + "/username/{username}"
}
