package io.branchtalk.shared.model

// Useful for generating the position in source code for debugging.
final case class CodePosition(
  file:    String,
  line:    Int,
  context: String
) derives FastEq,
      ShowPretty
object CodePosition {

  given providePosition(using
    file:      sourcecode.File,
    line:      sourcecode.Line,
    enclosing: sourcecode.Enclosing.Machine
  ): CodePosition =
    CodePosition(
      file = new java.io.File(file.value).getName,
      line = line.value,
      context = enclosing.value
    )
}
