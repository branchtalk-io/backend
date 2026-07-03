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

  // Cats instances

  given [A: JsCodec]: JsCodec[Chain[A]] = DefaultJsCodec.derived[List[A]].map(Chain.fromSeq)(_.toList)
  given [A: JsCodec]: JsCodec[NonEmptyChain[A]] = DefaultJsCodec
    .derived[List[A]]
    .mapDecode {
      case head :: tail => NonEmptyChain(head, tail: _*).asRight[String]
      case _            => "Expected non-empty list".asLeft[NonEmptyChain[A]]
    }(_.toList)
  given [A: JsCodec]: JsCodec[NonEmptyList[A]] = DefaultJsCodec
    .derived[List[A]]
    .mapDecode {
      case head :: tail => NonEmptyList(head, tail).asRight[String]
      case _            => "Expected non-empty list".asLeft[NonEmptyList[A]]
    }(_.toList)
  given [A: JsCodec: Order]: JsCodec[NonEmptySet[A]] = DefaultJsCodec
    .derived[List[A]]
    .mapDecode {
      case head :: tail => NonEmptySet.of(head, tail: _*).asRight[String]
      case _            => "Expected non-empty list".asLeft[NonEmptySet[A]]
    }(_.toList)

  // Neotype

  export neotype.interop.jsoniter.{ newtypeCodec, subtypeCodec }
}
private[api] trait JsoniterSupportImplicits { self: JsoniterSupport.type =>

  inline given JsoniterConfig = JsoniterConfig()
}
