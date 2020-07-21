package io.branchtalk.shared.models

import cats.Show
import magnolia._

import scala.language.experimental.macros

trait ShowPretty[T] extends Show[T] {

  def show(t: T): String = showPretty(t).toString()

  def showPretty(
    t:           T,
    sb:          StringBuilder = new StringBuilder,
    indentWith:  String = "  ",
    indentLevel: Int = 0
  ): StringBuilder
}

object ShowPretty extends ShowPrettyLowLevel {
  type Typeclass[T] = ShowPretty[T]

  def combine[T](caseClass: ReadOnlyCaseClass[Typeclass, T]): Typeclass[T] =
    (t: T, sb: StringBuilder, indentWith: String, indentLevel: Int) => {
      val nextIndent = indentLevel + 1
      val lastIndex  = caseClass.parameters.size - 1
      sb.append(caseClass.typeName.full).append("(\n")
      caseClass.parameters.foreach { p =>
        sb.append(indentWith * nextIndent).append(p.label).append(" = ")
        p.typeclass.showPretty(p.dereference(t), sb, indentWith, nextIndent)
        if (p.index =!= lastIndex) {
          sb.append(",")
        }
        sb.append("\n")
      }
      sb.append(indentWith * indentLevel).append(")")
    }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] =
    (t: T, sb: StringBuilder, indentWith: String, indentLevel: Int) =>
      sealedTrait.dispatch(t)(sub => sub.typeclass.showPretty(sub.cast(t), sb, indentWith, indentLevel))

  def semi[T]: Typeclass[T] = macro Magnolia.gen[T]
}

trait ShowPrettyLowLevel { self: ShowPretty.type =>

  implicit def liftShow[T](implicit normalShow: Show[T]): ShowPretty[T] =
    (t: T, sb: StringBuilder, _: String, _: Int) => sb.append(normalShow.show(t))
}
