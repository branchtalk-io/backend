package io.branchtalk.shared.model

import hearth.source.{ FileName, Line, MethodName }

// Useful for generating the position in source code for debugging.
final case class CodePosition(
  file:    String,
  line:    Int,
  context: String
) derives FastEq,
      ShowPretty
object CodePosition {

  // Hearth's source utilities (replacing lihaoyi sourcecode): FileName is the bare file name,
  // Line the line number, MethodName the enclosing method/definition.
  given providePosition(using
    fileName: FileName,
    lineNo:   Line,
    method:   MethodName
  ): CodePosition =
    CodePosition(
      file = fileName,
      line = lineNo,
      context = method
    )
}
