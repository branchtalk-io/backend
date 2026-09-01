package io.branchtalk.users.model

import cats.Eq
import cats.data.{ NonEmptyList, NonEmptySet }
import io.branchtalk.shared.model.*

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
  given ShowPretty[NonEmptySet[Permission]] with {
    def showLines(t: NonEmptySet[Permission]): List[String] = {
      val perm  = summon[ShowPretty[Permission]]
      val elems = t.toNonEmptyList.toList
      val inner = elems.zipWithIndex.flatMap { case (permission, index) =>
        val lines       = perm.showLines(permission)
        val lastLineIdx = lines.size - 1
        val withComma =
          if (index < elems.size - 1)
            lines.zipWithIndex.map { case (line, lineIdx) => if (lineIdx === lastLineIdx) line + "," else line }
          else lines
        withComma.map("  " + _)
      }
      "NonEmptySet(" :: inner ::: List(")")
    }
  }
}
