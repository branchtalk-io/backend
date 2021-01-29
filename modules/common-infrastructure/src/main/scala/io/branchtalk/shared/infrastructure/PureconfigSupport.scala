package io.branchtalk.shared.infrastructure

import cats.{ Alternative, Foldable }
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptyMap, NonEmptySet, NonEmptyVector }
import cats.kernel.Order
import enumeratum._
import enumeratum.values._
import eu.timepit.refined.api._

import scala.collection.immutable.SortedSet
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.WeakTypeTag

// Allows `import PureconfigSupport._` instead of `import pureconfig._, pureconfig.module.cats._, ...`.
// `pureconfig` modules are objects, there are no traits so if we want to have everything in one place we have to...
object PureconfigSupport extends LowPriorityPureconfigImplicit {

  type ConfigReader[A] = pureconfig.ConfigReader[A]
  val ConfigReader = pureconfig.ConfigReader

  type ConfigWriter[A] = pureconfig.ConfigWriter[A]
  val ConfigWriter = pureconfig.ConfigWriter

  type ConfigSource = pureconfig.ConfigSource
  val ConfigSource = pureconfig.ConfigSource

  // Cats

  implicit def nonEmptyListReader[A](implicit reader: ConfigReader[List[A]]): ConfigReader[NonEmptyList[A]] =
    pureconfig.module.cats.nonEmptyListReader[A]
  implicit def nonEmptyListWriter[A](implicit writer: ConfigWriter[List[A]]): ConfigWriter[NonEmptyList[A]] =
    pureconfig.module.cats.nonEmptyListWriter[A]

  implicit def nonEmptyVectorReader[A](implicit reader: ConfigReader[Vector[A]]): ConfigReader[NonEmptyVector[A]] =
    pureconfig.module.cats.nonEmptyVectorReader[A]
  implicit def nonEmptyVectorWriter[A](implicit writer: ConfigWriter[Vector[A]]): ConfigWriter[NonEmptyVector[A]] =
    pureconfig.module.cats.nonEmptyVectorWriter[A]

  implicit def nonEmptySetReader[A](implicit reader: ConfigReader[SortedSet[A]]): ConfigReader[NonEmptySet[A]] =
    pureconfig.module.cats.nonEmptySetReader[A]
  implicit def nonEmptySetWriter[A](implicit writer: ConfigWriter[SortedSet[A]]): ConfigWriter[NonEmptySet[A]] =
    pureconfig.module.cats.nonEmptySetWriter[A]

  implicit def nonEmptyMapReader[A, B](implicit
    reader: ConfigReader[Map[A, B]],
    ord:    Order[A]
  ): ConfigReader[NonEmptyMap[A, B]] =
    pureconfig.module.cats.nonEmptyMapReader[A, B]
  implicit def nonEmptyMapWriter[A, B](implicit writer: ConfigWriter[Map[A, B]]): ConfigWriter[NonEmptyMap[A, B]] =
    pureconfig.module.cats.nonEmptyMapWriter[A, B]

  // For emptiable foldables not covered by TraversableOnce reader/writer, e.g. Chain.
  implicit def lowPriorityNonReducibleReader[A, F[_]: Foldable: Alternative](implicit
    reader: ConfigReader[List[A]]
  ): pureconfig.Exported[ConfigReader[F[A]]] =
    pureconfig.module.cats.lowPriorityNonReducibleReader[A, F]
  implicit def lowPriorityNonReducibleWriter[A, F[_]: Foldable: Alternative](implicit
    writer: ConfigWriter[List[A]]
  ): pureconfig.Exported[ConfigWriter[F[A]]] =
    pureconfig.module.cats.lowPriorityNonReducibleWriter[A, F]

  implicit def nonEmptyChainReader[A](implicit reader: ConfigReader[Chain[A]]): ConfigReader[NonEmptyChain[A]] =
    pureconfig.module.cats.nonEmptyChainReader[A]
  implicit def nonEmptyChainWriter[A](implicit writer: ConfigWriter[Chain[A]]): ConfigWriter[NonEmptyChain[A]] =
    pureconfig.module.cats.nonEmptyChainWriter[A]

  // enumeratum

  implicit def enumeratumConfigConvert[A <: EnumEntry](implicit
    enum: Enum[A],
    ct:   ClassTag[A]
  ): pureconfig.ConfigConvert[A] =
    pureconfig.module.enumeratum.enumeratumConfigConvert[A]

  implicit def enumeratumIntConfigConvert[A <: IntEnumEntry](implicit
    enum: IntEnum[A],
    ct:   ClassTag[A]
  ): pureconfig.ConfigConvert[A] =
    pureconfig.module.enumeratum.enumeratumIntConfigConvert[A]

  implicit def enumeratumLongConfigConvert[A <: LongEnumEntry](implicit
    enum: LongEnum[A],
    ct:   ClassTag[A]
  ): pureconfig.ConfigConvert[A] =
    pureconfig.module.enumeratum.enumeratumLongConfigConvert[A]

  implicit def enumeratumShortConfigConvert[A <: ShortEnumEntry](implicit
    enum: ShortEnum[A],
    ct:   ClassTag[A]
  ): pureconfig.ConfigConvert[A] =
    pureconfig.module.enumeratum.enumeratumShortConfigConvert[A]

  implicit def enumeratumStringConfigConvert[A <: StringEnumEntry](implicit
    enum: StringEnum[A],
    ct:   ClassTag[A]
  ): pureconfig.ConfigConvert[A] =
    pureconfig.module.enumeratum.enumeratumStringConfigConvert[A]

  implicit def enumeratumByteConfigConvert[A <: ByteEnumEntry](implicit
    enum: ByteEnum[A],
    ct:   ClassTag[A]
  ): pureconfig.ConfigConvert[A] =
    pureconfig.module.enumeratum.enumeratumByteConfigConvert[A]

  implicit def enumeratumCharConfigConvert[A <: CharEnumEntry](implicit
    enum: CharEnum[A],
    ct:   ClassTag[A]
  ): pureconfig.ConfigConvert[A] =
    pureconfig.module.enumeratum.enumeratumCharConfigConvert[A]

  // refined

  implicit def refTypeConfigConvert[F[_, _], T, P](implicit
    configConvert: pureconfig.ConfigConvert[T],
    refType:       RefType[F],
    validate:      Validate[T, P],
    typeTag:       WeakTypeTag[F[T, P]]
  ): pureconfig.ConfigConvert[F[T, P]] = eu.timepit.refined.pureconfig.refTypeConfigConvert[F, T, P]
}

// for some reason original traversableReader is not seen, maybe because author forgot to annotate it?
trait LowPriorityPureconfigImplicit extends pureconfig.CollectionReaders
