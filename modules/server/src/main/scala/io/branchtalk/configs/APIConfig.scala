package io.branchtalk.configs

import cats.Show
import enumeratum.*
import io.branchtalk.api.Pagination
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.users.model.User
import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }
import io.branchtalk.shared.model.*
import pureconfig.error.CannotConvert
import sttp.apispec.openapi.*

import java.net.URI
import scala.concurrent.duration.FiniteDuration

given ConfigReader[User.Email]      = ConfigReader[String].emapString("User.Email")(User.Email.make)
given ConfigReader[Post.URL]        = ConfigReader[URI].emapString("Post.URL")(Post.URL.make)
given ConfigReader[Paginated.Limit] = ConfigReader[Int].emapString("Paginated.Limit")(Paginated.Limit.make)

final case class APIContact(
  name:  String,
  email: User.Email,
  url:   Post.URL
) derives ConfigReader,
      ShowPretty {

  def toOpenAPI: Contact = Contact(
    name = name.some,
    email = email.unwrap.some,
    url = url.unwrap.toString.some
  )
}

final case class APILicense(
  name: String,
  url:  Post.URL
) derives ConfigReader,
      ShowPretty {

  def toOpenAPI: License = License(
    name = name,
    url = url.unwrap.toString.some
  )
}

final case class APIInfo(
  title:          String, // TODO: refine
  version:        String, // TODO: refine
  description:    String, // TODO: refine
  termsOfService: Post.URL,
  contact:        APIContact,
  license:        APILicense
) derives ConfigReader,
      ShowPretty {

  def toOpenAPI: Info = Info(
    title = title,
    version = version,
    description = description.some,
    termsOfService = termsOfService.unwrap.toString.some,
    contact = contact.toOpenAPI.some,
    license = license.toOpenAPI.some
  )
}

final case class APIHttp(
  logHeaders:           Boolean,
  logBody:              Boolean,
  http2Enabled:         Boolean,
  corsAnyOrigin:        Boolean,
  corsAllowCredentials: Boolean,
  corsMaxAge:           FiniteDuration,
  maxHeaderLineLength:  Int, // TODO: refine
  maxRequestLineLength: Int // TODO: refine
) derives ConfigReader,
      ShowPretty

final case class PaginationConfig(
  defaultLimit: Paginated.Limit,
  maxLimit:     Paginated.Limit
) derives ConfigReader,
      ShowPretty {

  def resolveOffset(passedOffset: Option[Pagination.Offset]): Paginated.Offset =
    passedOffset.fold(Paginated.Offset(0L)) { value =>
      Paginated.Offset.unsafeMake(value.unwrap)
    }

  def resolveLimit(passedLimit: Option[Pagination.Limit]): Paginated.Limit =
    passedLimit.filter(_.unwrap <= maxLimit.unwrap).fold(defaultLimit) { value =>
      Paginated.Limit.unsafeMake(value.unwrap: Int)
    }
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
) derives ConfigReader,
      ShowPretty {

  val signedOutSubscriptions: Set[ID[Channel]] = defaultChannels.map(ID[Channel]).toSet

  val safePagination: Map[APIPart, PaginationConfig] =
    pagination.withDefaultValue(
      PaginationConfig(Paginated.Limit.unsafeMake(Defaults.defaultPaginationLimit),
                       Paginated.Limit.unsafeMake(Defaults.maxPaginationLimit)
      )
    )
}
