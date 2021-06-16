package ventures.dvx.base.user.web

sealed class EmailLoginOutputDto
data class SuccessfulEmailLoginDto(val accessToken: String) : EmailLoginOutputDto()
data class EmailLoginErrorDto(val err: String) : EmailLoginOutputDto()

sealed class MsisdnLoginStartedOutputDto
class SuccessfulMsisdnLoginDto : MsisdnLoginStartedOutputDto()
data class MsisdnLoginErrorDto(val err: String) : MsisdnLoginStartedOutputDto()
