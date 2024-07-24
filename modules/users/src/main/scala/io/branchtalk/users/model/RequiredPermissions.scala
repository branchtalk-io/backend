package io.branchtalk.users.model

import cats.Eq
import cats.data.{ NonEmptyList, NonEmptySet }
import io.branchtalk.shared.model.{ FastEq, ShowPretty }

import scala.annotation.targetName

enum RequiredPermissions derives FastEq, ShowPretty {
  case Empty

  case AllOf(toSet: NonEmptySet[Permission])
  case AnyOf(toSet: NonEmptySet[Permission])

  case And(x: RequiredPermissions, y: RequiredPermissions)
  case Or(x: RequiredPermissions, y: RequiredPermissions)
  case Not(x: RequiredPermissions)

  @targetName("and") def &&(other: RequiredPermissions): RequiredPermissions = And(this, other)
  @targetName("or") def ||(other:  RequiredPermissions): RequiredPermissions = Or(this, other)
  @targetName("not") def unary_!                       : RequiredPermissions = Not(this)
}
object RequiredPermissions {

  def empty:                                          RequiredPermissions = Empty
  def one(permission: Permission):                    RequiredPermissions = AllOf(NonEmptySet.one(permission))
  def allOf(head:     Permission, tail: Permission*): RequiredPermissions = AllOf(NonEmptySet.of(head, tail: _*))
  def anyOf(head:     Permission, tail: Permission*): RequiredPermissions = AnyOf(NonEmptySet.of(head, tail: _*))

  given Eq[NonEmptySet[Permission]] = (x: NonEmptySet[Permission], y: NonEmptySet[Permission]) =>
    x.toSortedSet === y.toSortedSet
  given ShowPretty[NonEmptySet[Permission]] =
    (t: NonEmptySet[Permission], sb: StringBuilder, indentWith: String, indentLevel: Int) => {
      val nextIndent = indentLevel + 1
      sb.append(indentWith * indentLevel).append("NonEmptySet(\n")
      t.toNonEmptyList match {
        case NonEmptyList(head, tail) =>
          sb.append(indentWith * nextIndent)
          summon[ShowPretty[Permission]].showPretty(head, sb, indentWith, nextIndent)
          tail.foreach { elem =>
            sb.append(",\n")
            sb.append(indentWith * nextIndent)
            summon[ShowPretty[Permission]].showPretty(elem, sb, indentWith, nextIndent)
          }
          sb.append("\n)")
      }
      sb
    }
}
