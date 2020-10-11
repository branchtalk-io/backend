package io.branchtalk.shared.models

import cats.data.NonEmptyList
import io.branchtalk.ADT
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) sealed trait CommonError extends Exception with ADT {
  val codePosition: CodePosition
  override def toString: String = this.show
}
object CommonError {
  final case class InvalidCredentials(codePosition: CodePosition) extends CommonError
  final case class InsufficientPermissions(msg:     String, codePosition: CodePosition) extends CommonError
  final case class NotFound(entity:                 String, id: ID[_], codePosition: CodePosition) extends CommonError {
    override def getMessage: String = s"Entity $entity id=${id.show} not found at: ${codePosition.show}"
  }
  final case class ParentNotExist(entity: String, id: ID[_], codePosition: CodePosition) extends CommonError {
    override def getMessage: String = s"Entity's parent $entity id=${id.show} not exist at: ${codePosition.show}"
  }
  final case class ValidationFailed(errors: NonEmptyList[String], codePosition: CodePosition) extends CommonError {
    override def getMessage: String =
      s"Validation failed at: ${codePosition.show}:\n${errors.mkString_("- ", "\n", "")}"
  }
}
