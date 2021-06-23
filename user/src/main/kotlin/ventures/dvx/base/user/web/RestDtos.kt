package ventures.dvx.base.user.web

import java.util.*

interface InputDto
interface OutputDto

data class OutputErrorDto(val errorMsg: String) : OutputDto

data class SuccessfulEmailLoginDto(val accessToken: String) : OutputDto
data class SuccessfulMsisdnLoginDto(val userId: UUID) : OutputDto
