package io.branchtalk.api

import java.net.URI
import cats.Id
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptySet }
import io.branchtalk.shared.model.{ ID, OptionUpdatable, UUID, Updatable, adtDiscriminatorNameMapper }
import neotype.*
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.generic.Configuration

import scala.annotation.nowarn

// Allows `import TapirSupport._` instead of `import sttp.tapir._, sttp.tapir.codec.refined._, ...`.
object TapirSupport
    extends sttp.tapir.Tapir
    with sttp.tapir.TapirAliases
    with sttp.tapir.codec.refined.TapirCodecRefined
    with sttp.tapir.json.jsoniter.TapirJsonJsoniter {

  // shortcuts
  type Param[A] = sttp.tapir.Codec[String, A, TextPlain]

  // alias to avoid confusion with Avro4s Schema
  type JsSchema[A] = sttp.tapir.Schema[A]
  val JsSchema = sttp.tapir.Schema

  inline def summonParam[T](using param:   Param[T]):    Param[T]    = param
  inline def summonSchema[T](using schema: JsSchema[T]): JsSchema[T] = schema

  // utilities

  extension [A, I, E, O, R](endpoint: Endpoint[A, I, E, O, R])
    def notRequiringPermissions: AuthedEndpoint[A, I, E, O, R] =
      AuthedEndpoint(endpoint, _ => RequiredPermissions.empty)
    def requiringPermissions(permissions: I => RequiredPermissions): AuthedEndpoint[A, I, E, O, R] =
      AuthedEndpoint(endpoint, permissions)

  extension [A](decodeResult: DecodeResult[A])
    def toOption: Option[A] = decodeResult match {
      case DecodeResult.Value(v) => v.some
      case _                     => none[A]
    }

  extension [T](schema: JsSchema[T])
    def asNewtypeSchema[N](using newtype: Newtype.WithType[T, N]): JsSchema[N] =
      newtype.unsafeMakeF[JsSchema](schema)

  given JsSchema[URI] = JsSchema.schemaForString.as[URI]

  // domain instances

  given [A]: Param[ID[A]]    = summonParam[UUID].map[ID[A]](ID[A].apply)(_.unwrap)
  given [A]: JsSchema[ID[A]] = summonSchema[UUID].asNewtypeSchema[ID[A]]

  given [A: JsSchema]: JsSchema[Updatable[A]] = {
    given Configuration =
      Configuration.default.copy(toEncodedName = adtDiscriminatorNameMapper).withDiscriminator("action")
    JsSchema.derived[Updatable[A]]
  }
  given [A: JsSchema]: JsSchema[OptionUpdatable[A]] = {
    given Configuration =
      Configuration.default.copy(toEncodedName = adtDiscriminatorNameMapper).withDiscriminator("action")
    JsSchema.derived[OptionUpdatable[A]]
  }

  /// Cats codecs

  given [A: JsSchema]: JsSchema[Chain[A]]         = summonSchema[List[A]].as[Chain[A]]
  given [A: JsSchema]: JsSchema[NonEmptyChain[A]] = summonSchema[List[A]].as[NonEmptyChain[A]]
  given [A: JsSchema]: JsSchema[NonEmptyList[A]]  = summonSchema[List[A]].as[NonEmptyList[A]]
  given [A: JsSchema]: JsSchema[NonEmptySet[A]]   = summonSchema[List[A]].as[NonEmptySet[A]]
}
