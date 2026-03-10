package io.liquidsoftware.common.application.error

/**
 * Shared application-contract error model for use-case and module API boundaries.
 *
 * This type exists to separate public application semantics from the legacy
 * `common.workflow.WorkflowError` hierarchy and from OSS workflow-engine errors.
 * It is intended to become the stable error language for `*-api` contracts.
 */
interface ApplicationError {
  /**
   * Stable machine-readable identifier for the error category or domain condition.
   */
  val code: String

  /**
   * Human-readable summary suitable for logs, diagnostics, and error responses.
   */
  val message: String

  /**
   * Optional structured details that callers may use for diagnostics or transport mapping.
   */
  val metadata: Map<String, String>

  /**
   * Indicates input or business-rule validation failure.
   */
  data class Validation(
    override val code: String = "VALIDATION_ERROR",
    override val message: String,
    override val metadata: Map<String, String> = emptyMap(),
  ) : ValidationApplicationError

  /**
   * Indicates a conflict with current application state, such as duplicate creation.
   */
  data class Conflict(
    override val code: String = "CONFLICT",
    override val message: String,
    override val metadata: Map<String, String> = emptyMap(),
  ) : ConflictApplicationError

  /**
   * Indicates the requested resource or aggregate was not found.
   */
  data class NotFound(
    override val code: String = "NOT_FOUND",
    override val message: String,
    override val metadata: Map<String, String> = emptyMap(),
  ) : NotFoundApplicationError

  /**
   * Indicates the caller is authenticated but not allowed to perform the operation.
   */
  data class Forbidden(
    override val code: String = "FORBIDDEN",
    override val message: String,
    override val metadata: Map<String, String> = emptyMap(),
  ) : AuthorizationApplicationError

  /**
   * Indicates the caller is not authenticated or cannot be resolved as a valid subject.
   */
  data class Unauthorized(
    override val code: String = "UNAUTHORIZED",
    override val message: String,
    override val metadata: Map<String, String> = emptyMap(),
  ) : AuthorizationApplicationError

  /**
   * Indicates an unexpected infrastructure or execution failure.
   */
  data class Unexpected(
    override val code: String = "UNEXPECTED_ERROR",
    override val message: String,
    override val metadata: Map<String, String> = emptyMap(),
  ) : InfrastructureApplicationError
}

/**
 * Marker for validation-oriented application errors.
 */
interface ValidationApplicationError : ApplicationError

/**
 * Marker for state-conflict application errors.
 */
interface ConflictApplicationError : ApplicationError

/**
 * Marker for missing-resource application errors.
 */
interface NotFoundApplicationError : ApplicationError

/**
 * Marker for authorization and authentication application errors.
 */
interface AuthorizationApplicationError : ApplicationError

/**
 * Marker for unexpected infrastructure or execution application errors.
 */
interface InfrastructureApplicationError : ApplicationError
