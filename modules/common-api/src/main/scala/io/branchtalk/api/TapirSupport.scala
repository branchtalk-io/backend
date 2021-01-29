package io.branchtalk.api

import java.net.URI

import cats.Id
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptySet }
import io.branchtalk.shared.model.{ ID, OptionUpdatable, UUID, Updatable, discriminatorNameMapper }
import io.estatico.newtype.Coercible
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{ Codec, DecodeResult, Endpoint, Schema }
import sttp.tapir.codec.refined.TapirCodecRefined
import sttp.tapir.generic.Configuration
import sttp.tapir.json.jsoniter.TapirJsonJsoniter

import scala.annotation.nowarn

@nowarn("cat=unused")
object TapirSupport extends TapirCodecRefined with TapirJsonJsoniter {

  // shortcuts
  type Param[A] = Codec[String, A, TextPlain]

  type JsSchema[A] = Schema[A]
  object JsSchema

  def summonParam[T](implicit param:   Param[T]):  Param[T]  = param
  def summonSchema[T](implicit schema: Schema[T]): Schema[T] = schema

  sttp.tapir.Schema

  // utilities

  implicit class EndpointOps[I, E, O, R](private val endpoint: Endpoint[I, E, O, R]) extends AnyVal {

    def notRequiringPermissions: AuthedEndpoint[I, E, O, R] =
      AuthedEndpoint(endpoint, _ => RequiredPermissions.empty)

    def requiringPermissions(permissions: I => RequiredPermissions): AuthedEndpoint[I, E, O, R] =
      AuthedEndpoint(endpoint, permissions)
  }

  implicit class TapirResultOps[A](private val decodeResult: DecodeResult[A]) extends AnyVal {

    def toOption: Option[A] = decodeResult match {
      case DecodeResult.Value(v) => v.some
      case _                     => none[A]
    }
  }

  implicit class RefineSchema[T](private val schema: Schema[T]) extends AnyVal {

    def asNewtype[N: Coercible[T, *]]: Schema[N] = Coercible.unsafeWrapMM[Schema, Id, T, N].apply(schema)
  }

  implicit val uriSchema: Schema[URI] = Schema.schemaForString.asInstanceOf[Schema[URI]]

  // domain instances

  implicit def idParam[A]:  Param[ID[A]]  = summonParam[UUID].map[ID[A]](ID[A](_))(_.uuid)
  implicit def idSchema[A]: Schema[ID[A]] = summonSchema[UUID].asNewtype[ID[A]]

  implicit def updatableSchema[A: Schema]: Schema[Updatable[A]] = {
    implicit val customConfiguration: Configuration =
      Configuration.default.copy(toEncodedName = discriminatorNameMapper(".")).withDiscriminator("action")
    Schema.derived[Updatable[A]]
  }

  implicit def optionUpdatableSchema[A: Schema]: Schema[OptionUpdatable[A]] = {
    implicit val customConfiguration: Configuration =
      Configuration.default.copy(toEncodedName = discriminatorNameMapper(".")).withDiscriminator("action")
    Schema.derived[OptionUpdatable[A]]
  }

  /// Cats codecs

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def chainSchema[A: Schema]: Schema[Chain[A]] =
    summonSchema[List[A]].asInstanceOf[Schema[Chain[A]]] // scalastyle:ignore

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def necSchema[A: Schema]: Schema[NonEmptyChain[A]] =
    summonSchema[List[A]].asInstanceOf[Schema[NonEmptyChain[A]]] // scalastyle:ignore

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def nelSchema[A: Schema]: Schema[NonEmptyList[A]] =
    summonSchema[List[A]].asInstanceOf[Schema[NonEmptyList[A]]] // scalastyle:ignore

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def nesSchema[A: Schema]: Schema[NonEmptySet[A]] =
    summonSchema[List[A]].asInstanceOf[Schema[NonEmptySet[A]]] // scalastyle:ignore
}
