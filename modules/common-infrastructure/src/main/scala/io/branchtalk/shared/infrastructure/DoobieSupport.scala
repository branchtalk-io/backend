package io.branchtalk.shared.infrastructure

import cats.effect.{ IO, Sync }
import com.typesafe.scalalogging.Logger
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.shared.model.{
  CodePosition,
  CommonError,
  ID,
  OptionUpdatable,
  Paginated,
  ParseRefined,
  UUID,
  Updatable
}
import io.estatico.newtype.Coercible

import scala.reflect.runtime.universe.TypeTag

object DoobieSupport
    extends doobie.Aliases // basic functionalities
    with doobie.hi.Modules
    with doobie.syntax.AllSyntax
    with doobie.free.Modules
    with doobie.free.Types
    with doobie.free.Instances
    with doobie.postgres.Instances // postgres extensions (without postgis)
    with doobie.postgres.hi.Modules
    with doobie.postgres.free.Modules
    with doobie.postgres.free.Types
    with doobie.postgres.free.Instances
    with doobie.postgres.syntax.ToPostgresMonadErrorOps
    with doobie.postgres.syntax.ToFragmentOps
    with doobie.postgres.syntax.ToPostgresExplainOps
    with doobie.refined.Instances // refined types
    with doobie.util.meta.MetaConstructors // Java Time extensions
    with doobie.util.meta.TimeMetaInstances {

  // enumeratum automatic support

  implicit def enumeratumMeta[A <: enumeratum.EnumEntry](implicit
    enum:    enumeratum.Enum[A],
    typeTag: TypeTag[A]
  ): Meta[A] =
    Meta[String].timap(enum.withNameInsensitive)(_.entryName)

  // newtype automatic support

  implicit def coercibleMeta[R, N](implicit ev: Coercible[Meta[R], Meta[N]], R: Meta[R]): Meta[N] = ev(R)

  implicit def idArrayMeta[E](implicit
    to:   Coercible[Set[UUID], Set[ID[E]]],
    from: Coercible[Set[ID[E]], Set[UUID]]
  ): Meta[Set[ID[E]]] =
    unliftedUUIDArrayType.imap[Set[ID[E]]](arr => to(arr.toSet))(set => from(set).toArray)

  // handle updateable

  implicit class DoobieUpdatableOps[A](private val updatable: Updatable[A]) extends AnyVal {

    def toUpdateFragment(columnName: Fragment)(implicit meta: Put[A]): Option[Fragment] =
      updatable.fold(value => (columnName ++ fr" = ${value}").some, none[Fragment])
  }

  implicit class DoobieOptionUpdatableOps[A](private val updatable: OptionUpdatable[A]) extends AnyVal {

    def toUpdateFragment(columnName: Fragment)(implicit meta: Put[A]): Option[Fragment] =
      updatable.fold(value => (columnName ++ fr"= ${value}").some, (columnName ++ fr"= null").some, none[Fragment])
  }

  implicit class FragmentOps(private val fragment: Fragment) extends AnyVal {

    def exists: ConnectionIO[Boolean] = (fr"SELECT EXISTS(" ++ fragment ++ fr")").query[Boolean].unique

    def paginate[Entity: Read](offset: Long Refined NonNegative, limit: Int Refined Positive)(implicit
      logHandler: LogHandler
    ): ConnectionIO[Paginated[Entity]] = {
      val o = offset.value
      val l = limit.value
      // limit 1 entity more than returned to check if there is a next page in pagination
      (fragment ++ fr"LIMIT ${l + 1} OFFSET ${o}").query[Entity].to[List].map { entities =>
        val result = entities.take(l)
        val nextOffset =
          if (entities.sizeCompare(l) > 0) ParseRefined[IO].parse[NonNegative](o + l).attempt.unsafeRunSync().toOption
          else None
        Paginated(result, nextOffset)
      }
    }
  }

  // handle errors

  implicit class QueryOps[A](private val query: Query0[A]) extends AnyVal {

    def failNotFound(entity: String, id: ID[_])(implicit codePosition: CodePosition): ConnectionIO[A] =
      query.unique.handleErrorWith { _ =>
        Sync[ConnectionIO].raiseError(CommonError.NotFound(entity, id, codePosition))
      }
  }

  // log results

  def doobieLogger(clazz: Class[_]): LogHandler = {
    val logger = Logger(clazz)
    LogHandler {
      case doobie.util.log.Success(sql, _, exec, processing) =>
        logger.trace(
          s"""SQL succeeded:
             |${sql}
             |execution:  ${exec.toMillis.toString} ms
             |processing: ${processing.toMillis.toString} ms
             |total:      ${(exec + processing).toMillis.toString} ms""".stripMargin
        )
      case doobie.util.log.ExecFailure(sql, _, exec, failure) =>
        logger.error(
          s"""SQL failed at execution:
             |${sql}
             |failure cause:
             |${failure.getMessage} ms
             |execution:  ${exec.toMillis.toString} ms""".stripMargin,
          failure
        )
      case doobie.util.log.ProcessingFailure(sql, _, exec, processing, failure) =>
        logger.error(
          s"""SQL failed at processing:
             |${sql}
             |failure cause:
             |${failure.getMessage}
             |execution:  ${exec.toMillis.toString} ms
             |processing: ${processing.toMillis.toString} ms
             |total:      ${(exec + processing).toMillis.toString} ms""".stripMargin,
          failure
        )
    }
  }
}
