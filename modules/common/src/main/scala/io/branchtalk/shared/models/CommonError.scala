package io.branchtalk.shared.models

import io.branchtalk.ADT
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) sealed trait CommonError extends Exception with ADT {
  val codePosition: CodePosition
  override def toString: String = this.show
}
object CommonError {
  final case class NotFound(entity: String, id: ID[_], codePosition: CodePosition) extends CommonError {
    override def getMessage: String = s"Entity $entity id=${id.show} not found at: ${codePosition.show}"
  }
  final case class ParentNotExist(entity: String, id: ID[_], codePosition: CodePosition) extends CommonError {
    override def getMessage: String = s"Entity's parent $entity id=${id.show} not exist at: ${codePosition.show}"
  }
}
