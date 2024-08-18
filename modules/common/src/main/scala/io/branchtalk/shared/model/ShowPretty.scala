package io.branchtalk.shared.model

import cats.Show
import magnolia1.*

// Custom implementation of ShowPretty which relies on Magnolia for derivation as opposed to Kittens' version.
@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
trait ShowPretty[T] extends Show[T] {

  final def show(t: T): String = showPretty(t).result()

  def showPretty(
    t:           T,
    sb:          StringBuilder = new StringBuilder,
    indentWith:  String = "  ",
    indentLevel: Int = 0
  ): StringBuilder
}

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
object ShowPretty extends Derivation[ShowPretty], ShowPrettyLowLevel {

  def join[T](caseClass: CaseClass[ShowPretty, T]): ShowPretty[T] = {
    (t: T, sb: StringBuilder, indentWith: String, indentLevel: Int) =>
      val nextIndent = indentLevel + 1
      val lastIndex  = caseClass.parameters.size - 1
      void(sb.append(caseClass.typeInfo.full).append("(\n"))
      caseClass.parameters.foreach { p =>
        void(sb.append(indentWith * nextIndent).append(p.label).append(" = "))
        void(p.typeclass.showPretty(p.deref(t), sb, indentWith, nextIndent))
        if (p.index =!= lastIndex) {
          sb.append(",")
        }
        sb.append("\n")
      }
      sb.append(indentWith * indentLevel).append(")")
  }

  def split[T](sealedTrait: SealedTrait[ShowPretty, T]): ShowPretty[T] =
    (t: T, sb: StringBuilder, indentWith: String, indentLevel: Int) =>
      sealedTrait.choose(t)(sub => sub.typeclass.showPretty(sub.cast(t), sb, indentWith, indentLevel))
}

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
trait ShowPrettyLowLevel { self: ShowPretty.type =>

  given liftShow[T](using normalShow: Show[T]): ShowPretty[T] =
    (t: T, sb: StringBuilder, _: String, _: Int) => sb.append(normalShow.show(t))
}
