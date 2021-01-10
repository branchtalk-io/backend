package io.branchtalk.shared.model

import cats.data.NonEmptyList
import io.branchtalk.ADT
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) sealed trait CommonError extends Exception with ADT {
  val codePosition: CodePosition
  override def toString: String = this.show
}
object CommonError {
  final case class InvalidCredentials(codePosition: CodePosition) extends CommonError
  final case class InsufficientPermissions(msg: String, codePosition: CodePosition) extends CommonError {
    override def getMessage: String = msg
  }
  final case class NotFound(entity: String, id: ID[_], codePosition: CodePosition) extends CommonError {
    override def getMessage: String = show"Entity $entity id=$id not found at: $codePosition"
  }
  final case class ParentNotExist(entity: String, id: ID[_], codePosition: CodePosition) extends CommonError {
    override def getMessage: String = show"Entity's parent $entity id=$id not exist at: $codePosition"
  }
  final case class ValidationFailed(errors: NonEmptyList[String], codePosition: CodePosition) extends CommonError {
    override def getMessage: String =
      show"Validation failed at: $codePosition:\n${errors.mkString_("- ", "\n", "")}"
  }
}
