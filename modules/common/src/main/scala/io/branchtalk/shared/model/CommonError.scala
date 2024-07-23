package io.branchtalk.shared.model

import cats.data.NonEmptyList

// Defines errors as ADT but also Throwable, so that we can extract during handling it and pattern-match all cases.
enum CommonError extends Exception derives FastEq, ShowPretty {
  case InvalidCredentials(codePosition: CodePosition)
  case InsufficientPermissions(msg: String, codePosition: CodePosition)
  case NotFound(entity: String, id: ID[Any], codePosition: CodePosition)
  case ParentNotExist(entity: String, id: ID[Any], codePosition: CodePosition)
  case ValidationFailed(errors: NonEmptyList[String], codePosition: CodePosition)

  val codePosition: CodePosition

  override def getMessage: String = this match {
    case InvalidCredentials(codePosition)           => show"Invalid credentials at: $codePosition"
    case InsufficientPermissions(msg, codePosition) => show"Insufficient permissions at: $codePosition\n$msg"
    case NotFound(entity, id, codePosition)         => show"Entity $entity id=$id not found at: $codePosition"
    case ParentNotExist(entity, id, codePosition)   => show"Entity's parent $entity id=$id not exist at: $codePosition"
    case ValidationFailed(errors, codePosition) =>
      show"Validation failed at: $codePosition:\n${errors.mkString_("- ", "\n", "")}"
  }

  override def toString: String = this.show
}
