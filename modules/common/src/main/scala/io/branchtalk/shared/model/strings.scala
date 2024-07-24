package io.branchtalk.shared.model

import java.nio.charset.{ Charset, StandardCharsets }
import java.util.Locale
import java.util.regex.Pattern

val branchtalkCharset: Charset = StandardCharsets.UTF_8 // used in getBytes and new String
val branchtalkLocale:  Locale  = Locale.ROOT // used in toLowerCase(branchtalkLocale) in Meta definitions

private val basePattern: Pattern = Pattern.compile("([A-Z]+)([A-Z][a-z])")
private val swapPattern: Pattern = Pattern.compile("([a-z\\d])([A-Z])")
def discriminatorNameMapper(separator: String): String => String = in => {
  val simpleName = in.substring(in.lastIndexOf(separator) + separator.length)
  val partial    = basePattern.matcher(simpleName).replaceAll("$1-$2")
  swapPattern.matcher(partial).replaceAll("$1-$2").toLowerCase(branchtalkLocale)
}
// String => String has to be object rather than val, so that Jsoniter macro could find it
object adtDiscriminatorNameMapper extends (String => String) {
  private val impl = discriminatorNameMapper(".")

  override def apply(name: String): String = impl(name)
}
