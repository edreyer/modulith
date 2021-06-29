package ventures.dvx.base.user.command

import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.NonEmptyString

enum class UserRole {
  USER,
  ADMIN;

  override fun toString(): String {
    return "ROLE_${this.name}"
  }
}

interface UserAggregate {
  var email: EmailAddress
  var firstName: NonEmptyString
  var lastName: NonEmptyString
  var roles : List<UserRole>
}
