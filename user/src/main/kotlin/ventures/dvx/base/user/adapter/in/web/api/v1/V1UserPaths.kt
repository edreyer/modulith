package ventures.dvx.base.user.adapter.`in`.web.api.v1

internal object V1UserPaths {
  const val API = "/api"
  const val V1 = "$API/v1"

  const val USERS = "$V1/users"
  const val USER_BY_ID = "$USERS/{userId}"
  const val USER_BY_EMAIL = "$USERS/email/{email}"
  const val USER_BY_MSISDN = "$USERS/msisdn/{msisdn}"
  const val ENABLE_USER = "$USER_BY_ID/enable"
  const val DISABLE_USER = "$USER_BY_ID/disable"
}

