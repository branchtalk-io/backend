package io.branchtalk.api

import cats.{ Id, Order }
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptySet }
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined.api.{ Refined, Validate }
import eu.timepit.refined.refineV
import io.branchtalk.shared.models.{ ID, OptionUpdatable, UUID, Updatable, discriminatorNameMapper }
import io.estatico.newtype.Coercible

// TODO: consider moving to some external library
@SuppressWarnings(Array("org.wartremover.warts.All")) // handling valid null values
object JsoniterSupport {

  // shortcuts

  type JsCodec[A] = JsonValueCodec[A]
  object JsCodec

  def summonCodec[T](implicit codec: JsCodec[T]): JsCodec[T] = codec

  // utilities

  implicit class RefineCodec[T](private val codec: JsCodec[T]) extends AnyVal {

    def mapDecode[U](f: T => Either[String, U])(g: U => T): JsCodec[U] = new JsCodec[U] {
      override def decodeValue(in: JsonReader, default: U): U =
        codec.decodeValue(in, if (default != null) g(default) else null.asInstanceOf[T]) match { // scalastyle:ignore
          case null => null.asInstanceOf[U] // scalastyle:ignore
          case t =>
            f(t) match {
              case null         => null.asInstanceOf[U] // scalastyle:ignore
              case Left(error)  => in.decodeError(error)
              case Right(value) => value
            }
        }

      override def encodeValue(x: U, out: JsonWriter): Unit = codec.encodeValue(g(x), out)

      override def nullValue: U = codec.nullValue match {
        case null => null.asInstanceOf[U] // scalastyle:ignore
        case u    => f(u).getOrElse(null.asInstanceOf[U]) // scalastyle:ignore
      }
    }

    def map[U](f: T => U)(g: U => T): JsCodec[U] = new JsCodec[U] {
      override def decodeValue(in: JsonReader, default: U): U =
        codec.decodeValue(in, if (default != null) g(default) else null.asInstanceOf[T]) match { // scalastyle:ignore
          case null => null.asInstanceOf[U] // scalastyle:ignore
          case t    => f(t)
        }

      override def encodeValue(x: U, out: JsonWriter): Unit = codec.encodeValue(g(x), out)

      override def nullValue: U = codec.nullValue match {
        case null => null.asInstanceOf[U] // scalastyle:ignore
        case u    => f(u)
      }
    }

    def refine[P: Validate[T, *]]: JsCodec[T Refined P] = mapDecode(refineV[P](_: T))(_.value)

    def asNewtype[N: Coercible[T, *]]: JsCodec[N] = Coercible.unsafeWrapMM[JsCodec, Id, T, N].apply(codec)
  }

  // domain instances

  implicit def idCodec[A]: JsCodec[ID[A]] = summonCodec[UUID](JsonCodecMaker.make).asNewtype[ID[A]]

  implicit def updatableCodec[A: JsCodec]: JsCodec[Updatable[A]] = summonCodec[Updatable[A]](
    JsonCodecMaker.make(
      CodecMakerConfig
        .withAdtLeafClassNameMapper(discriminatorNameMapper("."))
        .withDiscriminatorFieldName(Some("action"))
    )
  )

  implicit def optionUpdatableCodec[A: JsCodec]: JsCodec[OptionUpdatable[A]] = summonCodec[OptionUpdatable[A]](
    JsonCodecMaker.make(
      CodecMakerConfig
        .withAdtLeafClassNameMapper(discriminatorNameMapper("."))
        .withDiscriminatorFieldName(Some("action"))
    )
  )

  // Cats instances

  implicit def chainCodec[A: JsCodec]: JsCodec[Chain[A]] =
    summonCodec[List[A]](JsonCodecMaker.make).map(Chain.fromSeq)(_.toList)

  @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
  implicit def necCodec[A: JsCodec]: JsCodec[NonEmptyChain[A]] = summonCodec[List[A]](JsonCodecMaker.make).mapDecode {
    case head :: tail => NonEmptyChain(head, tail: _*).asRight[String]
    case _            => "Expected non-empty list".asLeft[NonEmptyChain[A]]
  }(_.toList)

  implicit def nelCodec[A: JsCodec]: JsCodec[NonEmptyList[A]] = summonCodec[List[A]](JsonCodecMaker.make).mapDecode {
    case head :: tail => NonEmptyList(head, tail).asRight[String]
    case _            => "Expected non-empty list".asLeft[NonEmptyList[A]]
  }(_.toList)

  implicit def nesCodec[A: JsCodec: Order]: JsCodec[NonEmptySet[A]] =
    summonCodec[List[A]](JsonCodecMaker.make).mapDecode {
      case head :: tail => NonEmptySet.of(head, tail: _*).asRight[String]
      case _            => "Expected non-empty list".asLeft[NonEmptySet[A]]
    }(_.toList)
}
