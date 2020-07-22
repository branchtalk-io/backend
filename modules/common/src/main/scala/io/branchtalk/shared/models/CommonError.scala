package io.branchtalk.shared.models

import io.branchtalk.ADT
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) sealed trait CommonError extends Exception with ADT { val codePosition: CodePosition }
object CommonError {
  final case class NotFound(entity: String, id: ID[_], codePosition: CodePosition) extends CommonError
}
