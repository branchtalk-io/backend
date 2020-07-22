package io.branchtalk.shared.infrastructure

import java.text.Normalizer

object NormalizeForUrl {

  private val removeNonAscii    = "[^\\x00-\\x7F]".r
  private val removeWhitespaces = "\\s".r

  def apply(text: String): String =
    text.trim
      .pipe(Normalizer.normalize(_, Normalizer.Form.NFKD))
      .pipe(removeNonAscii.replaceAllIn(_, ""))
      .pipe(removeWhitespaces.replaceAllIn(_, "-"))
}
