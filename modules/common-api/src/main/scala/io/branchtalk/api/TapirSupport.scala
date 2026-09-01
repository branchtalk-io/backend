package io.branchtalk.api

import java.net.URI
import cats.data.{ Chain, NonEmptyChain, NonEmptyList }
import hearth.kindlings.jsoniterderivation.JsoniterConfig as JsCodecConfig
import io.branchtalk.shared.model.*
import sttp.tapir.CodecFormat.TextPlain

// Allows `import TapirSupport._` instead of `import sttp.tapir._, sttp.tapir.json.jsoniter._, ...`.
object TapirSupport extends sttp.tapir.Tapir, sttp.tapir.TapirAliases, sttp.tapir.json.jsoniter.TapirJsonJsoniter {

  // shortcuts
  type Param[A] = sttp.tapir.Codec[String, A, TextPlain]

  // alias to avoid confusion with Avro4s Schema
  type JsSchema[A] = sttp.tapir.Schema[A]
  // `derives JsSchema` routes to Kindlings' tapir-schema derivation instead of Tapir's built-in one, so the derived
  // `Schema` lands in the type's companion (available everywhere, like before) but reads the in-scope Kindlings
  // JsoniterConfig - discriminator, field/leaf-name mappers - so the OpenAPI always matches the JSON payloads with no
  // separate Tapir Configuration. neotype newtypes are handled via the neotype-kindlings IsValueType macro-extension.
  object JsSchema {
    inline def derived[A]: sttp.tapir.Schema[A] =
      scala.compiletime.summonInline[hearth.kindlings.tapirschemaderivation.KindlingsSchema[A]].schema
    export sttp.tapir.Schema.schemaForString
  }
  // Modules that also have kindlings-avro-derivation on the classpath (e.g. anything depending on the domain layer)
  // expose more than one JSON-schema config, which makes Kindlings' schema derivation ambiguous. Prefer the jsoniter
  // config - that's the one that drives the HTTP JSON codecs the OpenAPI must describe.
  given hearth.kindlings.tapirschemaderivation.PreferSchemaConfig[JsCodecConfig] =
    hearth.kindlings.tapirschemaderivation.PreferSchemaConfig[JsCodecConfig]

  inline def summonParam[T](using param:   Param[T]):    Param[T]    = param
  inline def summonSchema[T](using schema: JsSchema[T]): JsSchema[T] = schema

  // utilities

  extension [A, I, E, O, R](endpoint: Endpoint[A, I, E, O, R]) {
    def notRequiringPermissions: AuthedEndpoint[A, I, E, O, R] =
      AuthedEndpoint(endpoint, _ => RequiredPermissions.empty)
    def requiringPermissions(permissions: I => RequiredPermissions): AuthedEndpoint[A, I, E, O, R] =
      AuthedEndpoint(endpoint, permissions)
  }

  extension [A](decodeResult: DecodeResult[A]) {
    def toOption: Option[A] = decodeResult match {
      case DecodeResult.Value(v) => v.some
      case _                     => none[A]
    }
  }

  // Utility for the few newtypes with a *custom* JSON representation (e.g. Password.Raw as a String, Post.URL as a URI):
  // their schema is the custom underlying schema re-typed to the newtype. Plain newtypes need nothing here - Kindlings'
  // schema derivation treats them as value types (via the neotype-kindlings IsValueType macro-extension) automatically.
  extension [A](schema: JsSchema[A]) {
    def asNewtypeSchema[B](using newtype: Newtype.WithType[A, B]): JsSchema[B] =
      newtype.unsafeMakeF[JsSchema](schema)
  }

  // A neotype newtype's schema is its underlying type's schema, re-typed. This is constrained to newtypes
  // (Newtype.WithType), so it never shadows Tapir's primitive/collection schemas; it lets Kindlings resolve newtype
  // fields (ID, User.Email, ...) while deriving the enclosing model's schema.
  given [A, B](using A: Newtype.WithType[B, A], B: JsSchema[B]): JsSchema[A] = B.asNewtypeSchema[A]

  given JsSchema[URI] = JsSchema.schemaForString.as[URI]

  // domain instances

  given [A]: Param[ID[A]] = summonParam[UUID].map[ID[A]](ID[A].apply)(_.unwrap)

  given [A: JsSchema]: JsSchema[Updatable[A]] = {
    inline given JsCodecConfig =
      JsCodecConfig().withAdtLeafClassNameMapper(adtDiscriminatorNameMapper).withDiscriminator("action")
    summon[hearth.kindlings.tapirschemaderivation.KindlingsSchema[Updatable[A]]].schema
  }
  given [A: JsSchema]: JsSchema[OptionUpdatable[A]] = {
    inline given JsCodecConfig =
      JsCodecConfig().withAdtLeafClassNameMapper(adtDiscriminatorNameMapper).withDiscriminator("action")
    summon[hearth.kindlings.tapirschemaderivation.KindlingsSchema[OptionUpdatable[A]]].schema
  }

  // Cats collections: the schema is derived by Kindlings, which treats them as collections via the
  // kindlings-cats-integration macro-extension. These givens exist only so Kindlings can resolve a cats-collection
  // *field* while deriving an enclosing model's schema (field schemas are summoned as `Schema[_]`, not re-derived).
  given [A: JsSchema]: JsSchema[Chain[A]] =
    summon[hearth.kindlings.tapirschemaderivation.KindlingsSchema[Chain[A]]].schema
  given [A: JsSchema]: JsSchema[NonEmptyChain[A]] =
    summon[hearth.kindlings.tapirschemaderivation.KindlingsSchema[NonEmptyChain[A]]].schema
  given [A: JsSchema]: JsSchema[NonEmptyList[A]] =
    summon[hearth.kindlings.tapirschemaderivation.KindlingsSchema[NonEmptyList[A]]].schema
  // NonEmptySet is only used by the internal RequiredPermissions ADT, which is never serialized, so it needs no schema.
}
