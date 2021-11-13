package ventures.dvx.common.workflow

interface Secured<R: Request> {

  suspend fun assertCanPerform(request: R)

}
