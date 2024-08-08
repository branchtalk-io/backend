package io.branchtalk.api

import cats.Eq
import cats.data.{ NonEmptyList, NonEmptySet }
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.shared.model.{ FastEq, ShowPretty, void }

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

  given JsCodec[RequiredPermissions] = {
    transparent inline given CodecMakerConfig = CodecMakerConfig.withAllowRecursiveTypes(true)
    DefaultJsCodec.derived[RequiredPermissions]
  }

  given Eq[NonEmptySet[Permission]] = (x: NonEmptySet[Permission], y: NonEmptySet[Permission]) =>
    x.toSortedSet === y.toSortedSet

  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  given ShowPretty[NonEmptySet[Permission]] = {
    (t: NonEmptySet[Permission], sb: StringBuilder, indentWith: String, indentLevel: Int) =>
      val nextIndent = indentLevel + 1
      void(sb.append(indentWith * indentLevel).append("NonEmptySet(\n"))
      t.toNonEmptyList match {
        case NonEmptyList(head, tail) =>
          sb.append(indentWith * nextIndent)
          void(summon[ShowPretty[Permission]].showPretty(head, sb, indentWith, nextIndent))
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
