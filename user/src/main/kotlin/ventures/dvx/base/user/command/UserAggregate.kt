package ventures.dvx.base.user.command

enum class UserRole {
  USER,
  ADMIN;

  override fun toString(): String {
    return "ROLE_${this.name}"
  }
}

interface UserAggregate {
  var email :String
  var firstName :String
  var lastName :String
  var roles : List<UserRole>
}
