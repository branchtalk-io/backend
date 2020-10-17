package io.branchtalk.configs

import enumeratum._
import io.branchtalk.api.{ PaginationLimit, PaginationOffset }
import io.scalaland.catnip.Semi
import io.scalaland.chimney.dsl._
import pureconfig._
import pureconfig.error.CannotConvert
import pureconfig.module.enumeratum._
import sttp.tapir.openapi.{ Contact, Info, License }
@Semi(ConfigReader) final case class APIContact(name: String, email: String, url: String) {

  def toOpenAPI: Contact = this.transformInto[Contact]
}
@Semi(ConfigReader) final case class APILicense(name: String, url: String) {

  def toOpenAPI: License = this.transformInto[License]
}

@Semi(ConfigReader) final case class APIInfo(
  title:          String,
  version:        String,
  description:    String,
  termsOfService: String,
  contact:        APIContact,
  license:        APILicense
) {

  def toOpenAPI: Info =
    this
      .into[Info]
      .withFieldConst(_.contact, contact.toOpenAPI.some)
      .withFieldConst(_.license, license.toOpenAPI.some)
      .transform
}

@Semi(ConfigReader) final case class PaginationConfig(
  defaultLimit: PaginationLimit,
  maxLimit:     PaginationLimit
) {

  def resolveOffset(passedOffset: Option[PaginationOffset]): PaginationOffset =
    passedOffset.getOrElse(PaginationOffset(0L))

  def resolveLimit(passedLimit: Option[PaginationLimit]): PaginationLimit =
    passedLimit.filter(_.positiveInt.value <= maxLimit.positiveInt.value).getOrElse(defaultLimit)
}

sealed trait APIPart extends EnumEntry
object APIPart extends Enum[APIPart] {
  case object Users extends APIPart
  case object Channels extends APIPart
  case object Posts extends APIPart
  case object Comments extends APIPart

  val values: IndexedSeq[APIPart] = findValues

  // NOTE: there is no derivation for Map[A, B] ConfigReader, only Map[String, A]
  implicit def asMapKey[A](implicit mapReader: ConfigReader[Map[String, A]]): ConfigReader[Map[APIPart, A]] =
    mapReader.emap { map =>
      map.toList
        .traverse {
          case (key, value) =>
            withNameEither(key).map(_ -> value).left.map(error => CannotConvert(key, "APIPart", error.getMessage()))
        }
        .map(_.toMap)
    }
}

@Semi(ConfigReader) final case class APIConfig(
  info:       APIInfo,
  pagination: Map[APIPart, PaginationConfig]
) {

  val safePagination: Map[APIPart, PaginationConfig] =
    pagination.withDefaultValue(
      PaginationConfig(PaginationLimit(Defaults.defaultPaginationLimit), PaginationLimit(Defaults.maxPaginationLimit))
    )
}
