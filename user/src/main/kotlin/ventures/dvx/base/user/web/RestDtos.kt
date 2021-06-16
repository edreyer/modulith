package ventures.dvx.base.user.web

sealed class UserLoginOutputDto
data class SuccessfulLoginDto(val accessToken: String) : UserLoginOutputDto()
data class LoginErrorDto(val err: String) : UserLoginOutputDto()
