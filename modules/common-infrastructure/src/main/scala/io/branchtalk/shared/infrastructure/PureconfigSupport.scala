package io.branchtalk.shared.infrastructure

import java.net.URI
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.util.Try
import cats.data.NonEmptyList
import enumeratum.{ Enum, EnumEntry }
import hearth.kindlings.sconfigderivation.{ ConfigReader, SConfig }

// Backed by Kindlings' Sconfig derivation (over org.ekrich sconfig) - keeps the old `PureconfigSupport` name/imports.
object PureconfigSupport {

  export hearth.kindlings.sconfigderivation.{ ConfigCodec, ConfigReader, ConfigWriter }

  // HOCON keys are kebab-case (this matches the previous pureconfig default field mapping).
  given SConfig = SConfig.default.withKebabCaseMemberNames

  extension [A](reader: ConfigReader[A]) {
    def emapString[B](tpe: String)(f: A => Either[String, B]): ConfigReader[B] =
      reader.emap(a => f(a).left.map(err => s"$tpe: $err"))
  }

  // base readers Kindlings' sconfig does not ship out of the box

  given ConfigReader[URI] =
    summon[ConfigReader[String]].emap(s => Try(URI.create(s)).toEither.left.map(_.getMessage))

  given ConfigReader[java.util.UUID] =
    summon[ConfigReader[String]].emap(s => Try(java.util.UUID.fromString(s)).toEither.left.map(_.getMessage))

  given ConfigReader[FiniteDuration] =
    summon[ConfigReader[String]].emap { s =>
      Try(Duration(s)).toEither.left.map(_.getMessage).flatMap {
        case fd: FiniteDuration => Right(fd)
        case _ => Left(s"Expected a finite duration, got: $s")
      }
    }

  // enumeratum: read an enum entry by its (case-insensitive) name via its `Enum` instance
  def enumeratumReader[A <: EnumEntry](using e: Enum[A]): ConfigReader[A] =
    summon[ConfigReader[String]].emap(s => e.withNameInsensitiveEither(s).left.map(_.getMessage))

  // cats: HOCON lists into non-empty collections
  given [A](using ConfigReader[List[A]]): ConfigReader[NonEmptyList[A]] =
    summon[ConfigReader[List[A]]].emap(list => NonEmptyList.fromList(list).toRight("Expected a non-empty list"))
}
