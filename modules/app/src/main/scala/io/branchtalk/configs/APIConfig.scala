package io.branchtalk.configs

import enumeratum._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.string.{ MatchesRegex, Url }
import io.branchtalk.api.{ PaginationLimit, PaginationOffset }
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.infrastructure.PureconfigSupport._
import io.branchtalk.shared.model.{ ID, UUID }
import io.scalaland.catnip.Semi
import pureconfig.error.CannotConvert
import sttp.tapir.openapi.{ Contact, Info, License }

import scala.concurrent.duration.FiniteDuration

@Semi(ConfigReader) final case class APIContact(
  name:  String,
  email: String Refined MatchesRegex["(.+)@(.+)"],
  url:   String Refined Url
) {

  def toOpenAPI: Contact = Contact(
    name = name.some,
    email = email.value.some,
    url = url.value.some
  )
}

@Semi(ConfigReader) final case class APILicense(
  name: String,
  url:  String Refined Url
) {

  def toOpenAPI: License = License(
    name = name,
    url = url.value.some
  )
}

@Semi(ConfigReader) final case class APIInfo(
  title:          String Refined NonEmpty,
  version:        String Refined NonEmpty,
  description:    String Refined NonEmpty,
  termsOfService: String Refined Url, // URI
  contact:        APIContact,
  license:        APILicense
) {

  def toOpenAPI: Info = Info(
    title = title.value,
    version = version.value,
    description = description.value.some,
    termsOfService = termsOfService.value.some,
    contact = contact.toOpenAPI.some,
    license = license.toOpenAPI.some
  )
}

@Semi(ConfigReader) final case class APIHttp(
  logHeaders:           Boolean,
  logBody:              Boolean,
  http2Enabled:         Boolean,
  corsAnyOrigin:        Boolean,
  corsAllowCredentials: Boolean,
  corsMaxAge:           FiniteDuration,
  maxHeaderLineLength:  Int Refined Positive,
  maxRequestLineLength: Int Refined Positive
)

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
        .traverse { case (key, value) =>
          withNameInsensitiveEither(key)
            .map(_ -> value)
            .left
            .map(error => CannotConvert(key, "APIPart", error.getMessage()))
        }
        .map(_.toMap)
    }
}

@Semi(ConfigReader) final case class APIConfig(
  info:            APIInfo,
  http:            APIHttp,
  defaultChannels: List[UUID],
  pagination:      Map[APIPart, PaginationConfig]
) {

  val signedOutSubscriptions: Set[ID[Channel]] = defaultChannels.map(ID[Channel]).toSet

  val safePagination: Map[APIPart, PaginationConfig] =
    pagination.withDefaultValue(
      PaginationConfig(PaginationLimit(Defaults.defaultPaginationLimit), PaginationLimit(Defaults.maxPaginationLimit))
    )
}
