package io.branchtalk.configs

import cats.Show
import enumeratum.*
import io.branchtalk.api.Pagination
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.users.model.User
import io.branchtalk.shared.infrastructure.Server
import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }
import io.branchtalk.shared.model.*
import sttp.apispec.openapi.*

import java.net.URI
import scala.concurrent.duration.FiniteDuration

given ConfigReader[User.Email]      = summon[ConfigReader[String]].emapString("User.Email")(User.Email.make)
given ConfigReader[Post.URL]        = summon[ConfigReader[URI]].emapString("Post.URL")(Post.URL.make)
given ConfigReader[Paginated.Limit] = summon[ConfigReader[Int]].emapString("Paginated.Limit")(Paginated.Limit.make)

final case class APIContact(
  name:  String,
  email: User.Email,
  url:   Post.URL
) derives ConfigReader,
      ShowPretty {

  def toOpenAPI: Contact = Contact(
    name = name.some,
    email = email.unwrap.some,
    url = url.show.some
  )
}

final case class APILicense(
  name: String,
  url:  Post.URL
) derives ConfigReader,
      ShowPretty {

  def toOpenAPI: License = License(
    name = name,
    url = url.show.some
  )
}

final case class APIInfo(
  title:          String,
  version:        String,
  description:    String,
  termsOfService: Post.URL,
  contact:        APIContact,
  license:        APILicense
) derives ConfigReader,
      ShowPretty {

  def toOpenAPI: Info = Info(
    title = title,
    version = version,
    description = description.some,
    termsOfService = termsOfService.show.some,
    contact = contact.toOpenAPI.some,
    license = license.toOpenAPI.some
  )
}

final case class APIHttp(
  logHeaders:           Boolean,
  logBody:              Boolean,
  corsAnyOrigin:        Boolean,
  corsAllowedOrigins:   List[String] = List.empty,
  corsAllowCredentials: Boolean,
  corsMaxAge:           FiniteDuration,
  maxHeaderLineLength:  Int,
  maxRequestLineLength: Int
) derives ConfigReader,
      ShowPretty

// Controls the HTTP-layer idempotency middleware that caches responses for repeated state-modifying requests
// (POST/PUT/PATCH/DELETE) keyed by the X-Request-ID header.
final case class APIIdempotency(
  enabled: Boolean,
  ttl:     FiniteDuration,
  redis:   Server
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
  case object Notifications extends APIPart

  val values: IndexedSeq[APIPart] = findValues

  // NOTE: there is no derivation for Map[A, B] ConfigReader, only Map[String, A].
  // A concrete given (not a generic one) is required so Kindlings' same-run derivation macro can resolve it.
  def asMapKey[A](using mapReader: ConfigReader[Map[String, A]]): ConfigReader[Map[APIPart, A]] =
    mapReader.emap { map =>
      map.toList
        .traverse { case (key, value) =>
          withNameInsensitiveEither(key)
            .map(_ -> value)
            .left
            .map(error => s"Cannot convert '$key' to APIPart: ${error.getMessage()}")
        }
        .map(_.toMap)
    }
  given ConfigReader[Map[APIPart, PaginationConfig]] = asMapKey
  given Show[APIPart]                                = _.entryName
}

final case class APIConfig(
  info:            APIInfo,
  http:            APIHttp,
  idempotency:     APIIdempotency,
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
