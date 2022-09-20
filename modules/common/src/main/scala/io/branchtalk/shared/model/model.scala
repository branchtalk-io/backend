package io.branchtalk.shared

import java.nio.charset.{ Charset, StandardCharsets }
import java.time.{ OffsetDateTime, ZoneId }
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import java.util.{ Locale, UUID => jUUID }
import cats.effect.{ Clock, Sync }
import cats.{ Eq, Functor, Order, Show }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.annotation.unused
import scala.reflect.runtime.universe.{ WeakTypeTag, weakTypeOf }

package object model {

  implicit class ShowCompanionOps(@unused private val s: Show.type) extends AnyVal {

    // displays "wrapper.package.WrapperName(content)"
    def wrap[Wrapper: WeakTypeTag, Content: Show](f: Wrapper => Content): Show[Wrapper] = {
      @SuppressWarnings(Array("org.wartremover.warts.ToString")) val name = weakTypeOf[Wrapper].toString
      s => show"$name(${f(s)})"
    }
  }

  type Logger[F[_]] = SelfAwareStructuredLogger[F]
  val Logger = Slf4jLogger

  type UUID = jUUID
  object UUID {

    def apply(string: String Refined Uuid)(implicit uuidGenerator: UUIDGenerator): UUID =
      uuidGenerator(string)
    def create[F[_]: Sync](implicit uuidGenerator: UUIDGenerator): F[UUID] =
      uuidGenerator.create[F]
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[UUID] =
      uuidGenerator.parse[F](string)
  }

  @newtype final case class ID[+Entity](uuid: UUID)
  object ID {
    def unapply[Entity](entity: ID[Entity]): Some[UUID] = Some(entity.uuid)
    def create[F[_]: Sync, Entity](implicit uuidGenerator: UUIDGenerator): F[ID[Entity]] =
      UUID.create[F].map(ID[Entity])
    def parse[F[_]: Sync, Entity](string: String)(implicit uuidGenerator: UUIDGenerator): F[ID[Entity]] =
      UUID.parse[F](string).map(ID[Entity])

    implicit def show[Entity]:  Show[ID[Entity]]  = Show.wrap(_.uuid)
    implicit def order[Entity]: Order[ID[Entity]] = Order[UUID].coerce[Order[ID[Entity]]]
  }

  @newtype final case class CreationTime(offsetDateTime: OffsetDateTime)
  object CreationTime {
    def unapply(creationTime: CreationTime): Some[OffsetDateTime] = Some(creationTime.offsetDateTime)
    def now[F[_]: Functor: Clock]: F[CreationTime] =
      Clock[F].realTimeInstant.map(OffsetDateTime.ofInstant(_, ZoneId.systemDefault())).map(CreationTime(_))

    implicit val show: Show[CreationTime] = Show.wrap(_.offsetDateTime.pipe(DateTimeFormatter.ISO_INSTANT.format))
    implicit val order: Order[CreationTime] =
      Order.by[CreationTime, OffsetDateTime](_.offsetDateTime)(Order.fromComparable)
  }
  @newtype final case class ModificationTime(offsetDateTime: OffsetDateTime)
  object ModificationTime {
    def unapply(modificationTime: ModificationTime): Some[OffsetDateTime] = Some(modificationTime.offsetDateTime)
    def now[F[_]: Functor: Clock]: F[ModificationTime] =
      Clock[F].realTimeInstant.map(OffsetDateTime.ofInstant(_, ZoneId.systemDefault())).map(ModificationTime(_))

    implicit val show: Show[ModificationTime] = Show.wrap(_.offsetDateTime.pipe(DateTimeFormatter.ISO_INSTANT.format))
    implicit val order: Order[ModificationTime] =
      Order.by[ModificationTime, OffsetDateTime](_.offsetDateTime)(Order.fromComparable)
  }

  @newtype final case class CreationScheduled[Entity](id: ID[Entity])
  object CreationScheduled {
    def unapply[Entity](creationScheduled: CreationScheduled[Entity]): Some[ID[Entity]] = Some(creationScheduled.id)

    implicit def show[Entity]: Show[CreationScheduled[Entity]] = Show.wrap(_.id)
    implicit def eq[Entity]:   Eq[CreationScheduled[Entity]]   = Eq[ID[Entity]].coerce
  }
  @newtype final case class UpdateScheduled[Entity](id: ID[Entity])
  object UpdateScheduled {
    def unapply[Entity](updateScheduled: UpdateScheduled[Entity]): Some[ID[Entity]] = Some(updateScheduled.id)

    implicit def show[Entity]: Show[UpdateScheduled[Entity]] = Show.wrap(_.id)
    implicit def eq[Entity]:   Eq[UpdateScheduled[Entity]]   = Eq[ID[Entity]].coerce
  }
  @newtype final case class DeletionScheduled[Entity](id: ID[Entity])
  object DeletionScheduled {
    def unapply[Entity](deletionScheduled: DeletionScheduled[Entity]): Some[ID[Entity]] = Some(deletionScheduled.id)

    implicit def show[Entity]: Show[DeletionScheduled[Entity]] = Show.wrap(_.id)
    implicit def eq[Entity]:   Eq[DeletionScheduled[Entity]]   = Eq[ID[Entity]].coerce
  }
  @newtype final case class RestoreScheduled[Entity](id: ID[Entity])
  object RestoreScheduled {
    def unapply[Entity](restoreScheduled: RestoreScheduled[Entity]): Some[ID[Entity]] = Some(restoreScheduled.id)

    implicit def show[Entity]: Show[RestoreScheduled[Entity]] = Show.wrap(_.id)
    implicit def eq[Entity]:   Eq[RestoreScheduled[Entity]]   = Eq[ID[Entity]].coerce
  }

  implicit class Untupled2[I1, I2, Out](private val f: ((I1, I2)) => Out) extends AnyVal {
    def untupled(i1: I1, i2: I2): Out = f.apply((i1, i2))
  }
  implicit class Untupled2A[I1, I2, Out](private val f: (I1, I2) => Out) extends AnyVal {
    def untupled(i1: I1, i2: I2): Out = f.apply(i1, i2)
  }
  implicit class Untupled3[I1, I2, I3, Out](private val f: ((I1, I2, I3)) => Out) extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3): Out = f.apply((i1, i2, i3))
  }
  implicit class Untupled3A[I1, I2, I3, Out](private val f: (I1, (I2, I3)) => Out) extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3): Out = f.apply(i1, (i2, i3))
  }
  implicit class Untupled4[I1, I2, I3, I4, Out](private val f: ((I1, I2, I3, I4)) => Out) extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3, i4: I4): Out = f.apply((i1, i2, i3, i4))
  }
  implicit class Untupled4A[I1, I2, I3, I4, Out](private val f: (I1, (I2, I3, I4)) => Out) extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3, i4: I4): Out = f.apply(i1, (i2, i3, i4))
  }
  implicit class Untupled5[I1, I2, I3, I4, I5, Out](private val f: ((I1, I2, I3, I4, I5)) => Out) extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5): Out = f.apply((i1, i2, i3, i4, i5))
  }
  implicit class Untupled5A[I1, I2, I3, I4, I5, Out](private val f: (I1, (I2, I3, I4, I5)) => Out) extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5): Out = f.apply(i1, (i2, i3, i4, i5))
  }
  implicit class Untupled6[I1, I2, I3, I4, I5, I6, Out](private val f: ((I1, I2, I3, I4, I5, I6)) => Out)
      extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, i6: I6): Out = f.apply((i1, i2, i3, i4, i5, i6))
  }
  implicit class Untupled6A[I1, I2, I3, I4, I5, I6, Out](private val f: (I1, (I2, I3, I4, I5, I6)) => Out)
      extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, i6: I6): Out = f.apply(i1, (i2, i3, i4, i5, i6))
  }
  implicit class Untupled7[I1, I2, I3, I4, I5, I6, I7, Out](private val f: ((I1, I2, I3, I4, I5, I6, I7)) => Out)
      extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, i6: I6, i7: I7): Out = f.apply((i1, i2, i3, i4, i5, i6, i7))
  }
  implicit class Untupled7A[I1, I2, I3, I4, I5, I6, I7, Out](private val f: (I1, (I2, I3, I4, I5, I6, I7)) => Out)
      extends AnyVal {
    def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, i6: I6, i7: I7): Out = f.apply(i1, (i2, i3, i4, i5, i6, i7))
  }

  val branchtalkCharset: Charset = StandardCharsets.UTF_8 // used in getBytes and new String
  val branchtalkLocale:  Locale  = Locale.ROOT // used in toLowerCase(branchtalkLocale) in Meta definitions

  private val basePattern: Pattern = Pattern.compile("([A-Z]+)([A-Z][a-z])")
  private val swapPattern: Pattern = Pattern.compile("([a-z\\d])([A-Z])")
  def discriminatorNameMapper(separator: String): String => String = in => {
    val simpleName = in.substring(in.lastIndexOf(separator) + separator.length)
    val partial    = basePattern.matcher(simpleName).replaceAll("$1-$2")
    swapPattern.matcher(partial).replaceAll("$1-$2").toLowerCase(branchtalkLocale)
  }
}
