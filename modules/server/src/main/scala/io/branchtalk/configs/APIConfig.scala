package io.branchtalk.configs

import cats.Show
import enumeratum.*
import io.branchtalk.api.Pagination
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.infrastructure.PureconfigSupport.*
import io.branchtalk.shared.model.*
import pureconfig.error.CannotConvert
import sttp.apispec.openapi.*

import scala.concurrent.duration.FiniteDuration

final case class APIContact(
  name:  String,
  email: String Refined MatchesRegex["(.+)@(.+)"],
  url:   String Refined Url
) derives ConfigReader, ShowPretty {

  def toOpenAPI: Contact = Contact(
    name = name.some,
    email = email.value.some,
    url = url.value.some
  )
}
object APIContact {
  implicit private val showEmail: Show[String Refined MatchesRegex["(.+)@(.+)"]] = _.value
  implicit private val showUrl:   Show[String Refined Url]                       = _.value
}

final case class APILicense(
  name: String,
  url:  String Refined Url
) derives ConfigReader, ShowPretty {

  def toOpenAPI: License = License(
    name = name,
    url = url.value.some
  )
}
object APILicense {
  implicit private val showUrl: Show[String Refined Url] = _.value
}

final case class APIInfo(
  title:          String Refined NonEmpty,
  version:        String Refined NonEmpty,
  description:    String Refined NonEmpty,
  termsOfService: String Refined Url,
  contact:        APIContact,
  license:        APILicense
) derives ConfigReader, ShowPretty {

  def toOpenAPI: Info = Info(
    title = title.value,
    version = version.value,
    description = description.value.some,
    termsOfService = termsOfService.value.some,
    contact = contact.toOpenAPI.some,
    license = license.toOpenAPI.some
  )
}
object APIInfo {
  implicit private val showNES: Show[String Refined NonEmpty] = _.value
  implicit private val showUrl: Show[String Refined Url]      = _.value
}

final case class APIHttp(
  logHeaders:           Boolean,
  logBody:              Boolean,
  http2Enabled:         Boolean,
  corsAnyOrigin:        Boolean,
  corsAllowCredentials: Boolean,
  corsMaxAge:           FiniteDuration,
  maxHeaderLineLength:  Int Refined Positive,
  maxRequestLineLength: Int Refined Positive
)derives ConfigReader, ShowPretty
object APIHttp {
  implicit private val showPositive: Show[Int Refined Positive] = _.value.toString
}

final case class PaginationConfig(
  defaultLimit: Pagination.Limit,
  maxLimit:     Pagination.Limit
) derives ConfigReader, ShowPretty {

  def resolveOffset(passedOffset: Option[Pagination.Offset]): Pagination.Offset =
    passedOffset.getOrElse(Pagination.Offset(0L))

  def resolveLimit(passedLimit: Option[Pagination.Limit]): Pagination.Limit =
    passedLimit.filter(_.positiveInt.value <= maxLimit.positiveInt.value).getOrElse(defaultLimit)
}
object PaginationConfig {
  implicit private val showLimit: Show[Pagination.Limit] = _.positiveInt.value.toString
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
  implicit val show: Show[APIPart] = _.entryName
}

final case class APIConfig(
  info:            APIInfo,
  http:            APIHttp,
  defaultChannels: List[UUID],
  pagination:      Map[APIPart, PaginationConfig]
) derives ConfigReader, ShowPretty {

  val signedOutSubscriptions: Set[ID[Channel]] = defaultChannels.map(ID[Channel]).toSet

  val safePagination: Map[APIPart, PaginationConfig] =
    pagination.withDefaultValue(
      PaginationConfig(Pagination.Limit(Defaults.defaultPaginationLimit), Pagination.Limit(Defaults.maxPaginationLimit))
    )
}
