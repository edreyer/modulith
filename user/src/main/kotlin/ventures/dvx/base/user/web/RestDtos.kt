package ventures.dvx.base.user.web

interface InputDto
interface OutputDto

data class OutputErrorDto(val errorMsg: String) : OutputDto

data class SuccessfulEmailLoginDto(val accessToken: String) : OutputDto
object SuccessfulMsisdnLoginDto : OutputDto
