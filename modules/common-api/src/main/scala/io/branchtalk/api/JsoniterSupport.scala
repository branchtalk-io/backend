package io.branchtalk.api

import cats.{ Id, Order }
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptySet }
import com.github.plokhotnyuk.jsoniter_scala.core.*
import hearth.kindlings.jsoniterderivation.{ JsoniterConfig, KindlingsJsonValueCodec }
import io.branchtalk.shared.model.*

// Provides (missing :/) support for .map, .mapDecode,.asNewtype for Jsoniter Scala codecs.
object JsoniterSupport extends JsoniterSupportImplicits {

  // for shortening

  type JsCodec[A] = JsonValueCodec[A]

  // Kindlings' Hearth-based derivation replaces jsoniter-scala's JsonCodecMaker.
  type DefaultJsCodec[A] = KindlingsJsonValueCodec[A]
  val DefaultJsCodec = KindlingsJsonValueCodec
  export hearth.kindlings.jsoniterderivation.JsoniterConfig as JsCodecConfig

  // utilities

  inline def summonCodec[T](using codec: JsCodec[T]): JsCodec[T] = codec

  extension [A](codec: JsCodec[A]) {
    @SuppressWarnings(Array("org.wartremover.warts.All"))
    def mapDecode[B](f: A => Either[String, B])(g: B => A): JsCodec[B] = new JsCodec[B] {
      override def decodeValue(in: JsonReader, default: B): B =
        codec.decodeValue(in, if (default != null) g(default) else null.asInstanceOf[A]) match {
          case null => null.asInstanceOf[B]
          case t =>
            f(t) match {
              case null         => null.asInstanceOf[B]
              case Left(error)  => in.decodeError(error)
              case Right(value) => value
            }
        }

      override def encodeValue(x: B, out: JsonWriter): Unit = codec.encodeValue(g(x), out)

      override def nullValue: B = codec.nullValue match {
        case null => null.asInstanceOf[B]
        case u    => f(u).getOrElse(null.asInstanceOf[B])
      }
    }

    @SuppressWarnings(Array("org.wartremover.warts.All"))
    def map[B](f: A => B)(g: B => A): JsCodec[B] = new JsCodec[B] {
      override def decodeValue(in: JsonReader, default: B): B =
        codec.decodeValue(in, if (default != null) g(default) else null.asInstanceOf[A]) match {
          case null => null.asInstanceOf[B]
          case t    => f(t)
        }

      override def encodeValue(x: B, out: JsonWriter): Unit = codec.encodeValue(g(x), out)

      override def nullValue: B = codec.nullValue match {
        case null => null.asInstanceOf[B]
        case u    => f(u)
      }
    }

    def asNewtypeCodec[B](using newtype: Newtype.WithType[A, B]): JsCodec[B] =
      newtype.unsafeMakeF[JsCodec](codec)
  }

  // NOTE: we deliberately do NOT `export neotype.interop.jsoniter.{ newtypeCodec, subtypeCodec }`.
  // That inline given wraps jsoniter-scala's JsonCodecMaker, which decodes an empty JSON array `[]` to the passed
  // `default` value; Kindlings hands `null` as the default for fields, so collection-backed newtypes (e.g. Permissions
  // over Set) decoded empty collections to `null`. Kindlings' own derivation handles `[]` correctly, so we let the
  // neotype-kindlings IsValueType macro-extension treat all neotype newtypes as value types uniformly across every
  // Kindlings macro (jsoniter, avro, tapir-schema). Use `.asNewtypeCodec` only when a custom representation is needed.

  // domain instances

  given [A]: JsCodec[ID[A]] = DefaultJsCodec.derived[UUID].asNewtypeCodec[ID[A]]

  given [A](using JsCodec[A]): JsCodec[Updatable[A]] = {
    inline given JsoniterConfig =
      JsCodecConfig()
        .withAdtLeafClassNameMapper(adtDiscriminatorNameMapper)
        .copy(discriminatorFieldName = Some("action"))
    DefaultJsCodec.derived[Updatable[A]]
  }

  given [A](using JsCodec[A]): JsCodec[OptionUpdatable[A]] = {
    inline given JsoniterConfig =
      JsCodecConfig()
        .withAdtLeafClassNameMapper(adtDiscriminatorNameMapper)
        .copy(discriminatorFieldName = Some("action"))
    DefaultJsCodec.derived[OptionUpdatable[A]]
  }

  // Cats collections (Chain, NonEmptyList, NonEmptyChain, NonEmptySet) are handled uniformly by the
  // kindlings-cats-integration macro-extension (on the classpath via the shared settings), so no manual codecs here.
}
private[api] trait JsoniterSupportImplicits { self: JsoniterSupport.type =>

  // Match the pre-Kindlings API contract: ADTs are encoded with a "type" discriminator field
  // (e.g. {"type":"Administrate"}), not Kindlings' default wrapper form ({"Administrate":{}}).
  // Updatable/OptionUpdatable override this with the "action" discriminator (see above).
  inline given JsoniterConfig = JsoniterConfig().withDiscriminator("type")
}
