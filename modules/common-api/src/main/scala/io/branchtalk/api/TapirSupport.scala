package io.branchtalk.api

import java.net.URI

import cats.Id
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptySet }
import io.branchtalk.shared.model.{ ID, OptionUpdatable, UUID, Updatable, discriminatorNameMapper }
import io.estatico.newtype.Coercible
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.generic.Configuration

import scala.annotation.nowarn

// Allows `import TapirSupport._` instead of `import sttp.tapir._, sttp.tapir.codec.refined._, ...`.
@nowarn("cat=unused")
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

  def summonParam[T](implicit param:   Param[T]):    Param[T]    = param
  def summonSchema[T](implicit schema: JsSchema[T]): JsSchema[T] = schema

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

  implicit class RefineSchema[T](private val schema: JsSchema[T]) extends AnyVal {

    def asNewtype[N: Coercible[T, *]]: JsSchema[N] = Coercible.unsafeWrapMM[JsSchema, Id, T, N].apply(schema)
  }

  implicit val uriSchema: JsSchema[URI] = JsSchema.schemaForString.asInstanceOf[JsSchema[URI]]

  // domain instances

  implicit def idParam[A]:  Param[ID[A]]    = summonParam[UUID].map[ID[A]](ID[A](_))(_.uuid)
  implicit def idSchema[A]: JsSchema[ID[A]] = summonSchema[UUID].asNewtype[ID[A]]

  implicit def updatableSchema[A: JsSchema]: JsSchema[Updatable[A]] = {
    implicit val customConfiguration: Configuration =
      Configuration.default.copy(toEncodedName = discriminatorNameMapper(".")).withDiscriminator("action")
    JsSchema.derived[Updatable[A]]
  }

  implicit def optionUpdatableSchema[A: JsSchema]: JsSchema[OptionUpdatable[A]] = {
    implicit val customConfiguration: Configuration =
      Configuration.default.copy(toEncodedName = discriminatorNameMapper(".")).withDiscriminator("action")
    JsSchema.derived[OptionUpdatable[A]]
  }

  /// Cats codecs

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def chainSchema[A: JsSchema]: JsSchema[Chain[A]] =
    summonSchema[List[A]].asInstanceOf[JsSchema[Chain[A]]] // scalastyle:ignore

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def necSchema[A: JsSchema]: JsSchema[NonEmptyChain[A]] =
    summonSchema[List[A]].asInstanceOf[JsSchema[NonEmptyChain[A]]] // scalastyle:ignore

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def nelSchema[A: JsSchema]: JsSchema[NonEmptyList[A]] =
    summonSchema[List[A]].asInstanceOf[JsSchema[NonEmptyList[A]]] // scalastyle:ignore

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def nesSchema[A: JsSchema]: JsSchema[NonEmptySet[A]] =
    summonSchema[List[A]].asInstanceOf[JsSchema[NonEmptySet[A]]] // scalastyle:ignore
}
