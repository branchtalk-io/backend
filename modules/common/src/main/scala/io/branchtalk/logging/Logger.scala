package io.branchtalk.logging

import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

type Logger[F[_]] = SelfAwareStructuredLogger[F]
val Logger = Slf4jLogger

extension (l: Logger.type) {

  def apply[F[_]](using logger: Logger[F]): Logger[F] = logger
}
