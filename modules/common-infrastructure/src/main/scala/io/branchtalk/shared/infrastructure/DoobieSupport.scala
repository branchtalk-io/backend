package io.branchtalk.shared.infrastructure

import cats.effect.{ Sync, SyncIO }
import com.typesafe.scalalogging.Logger
import io.branchtalk.shared.model.*
import org.tpolecat.typename.TypeName
import neotype.*

// Allows `import DoobieSupport._` instead of... a lot of imports.
// Additionally provides support for a few useful but missing features.
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
    with doobie.util.meta.MetaConstructors // Java Time extensions
    with doobie.util.meta.TimeMetaInstances {

  // enumeratum automatic support

  export enumeratum.Doobie.meta as enumeraturmMeta

  // newtype automatic support

  export neotype.interop.doobie.{
    newtypeArrayGet,
    newtypeArrayPut,
    newtypeGet,
    newtypePut,
    subtypeArrayGet,
    subtypeArrayPut,
    subtypeGet,
    subtypePut
  }

  given [E]: Meta[Set[ID[E]]] =
    ID.unsafeMakeF[[A] =>> Meta[Set[A]], E](unliftedUUIDArrayType.imap[Set[UUID]](_.toSet)(_.toArray))

  // handle updateable

  extension [A](updatable: Updatable[A])
    def toUpdateFragment(columnName: Fragment)(using Put[A]): Option[Fragment] =
      updatable.fold(value => (columnName ++ fr" = ${value}").some, none[Fragment])

  extension [A](updatable: OptionUpdatable[A])
    def toUpdateFragment(columnName: Fragment)(using Put[A]): Option[Fragment] =
      updatable.fold(value => (columnName ++ fr"= ${value}").some, (columnName ++ fr"= null").some, none[Fragment])

  extension (fragment: Fragment)
    def exists: ConnectionIO[Boolean] =
      (fr"SELECT EXISTS(" ++ fragment ++ fr")").query[Boolean].unique

    def paginate[Entity: Read](offset: Paginated.Offset, limit: Paginated.Limit): ConnectionIO[Paginated[Entity]] = {
      val o: Long = offset.unwrap
      val l: Int  = limit.unwrap
      // limit 1 entity more than returned to check if there is a next page in pagination
      (fragment ++ fr"LIMIT ${l + 1} OFFSET ${o}").query[Entity].to[List].map { entities =>
        val result = entities.take(l)
        val nextOffset =
          if (entities.sizeIs <= l) None
          else ParseNewtype[SyncIO].parse[Paginated.Offset](o + l).attempt.unsafeRunSync().toOption
        Paginated(result, nextOffset)
      }
    }

  // handle errors

  extension [A](query: Query0[A])
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def failNotFound[Entity](entity: String, id: ID[Entity])(using CodePosition): ConnectionIO[A] =
      query.unique.orRaise(CommonError.notFound(entity, id.asInstanceOf[ID[Any]]))

  // log results

  /*
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
   */
}
