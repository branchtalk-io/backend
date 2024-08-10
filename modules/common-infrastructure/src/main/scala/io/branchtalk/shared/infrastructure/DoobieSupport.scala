package io.branchtalk.shared.infrastructure

import cats.effect.{ Sync, SyncIO }
import doobie.util.log
import io.branchtalk.logging.Logger
import io.branchtalk.shared.model.*
import org.tpolecat.typename.TypeName

// Allows `import DoobieSupport.*` instead of... a lot of imports.
// Additionally, provides support for a few useful but missing features.
object DoobieSupport
// basic functionalities
    extends doobie.Aliases,
      doobie.hi.Modules,
      doobie.syntax.AllSyntax,
      doobie.free.Modules,
      doobie.free.Types,
      doobie.free.Instances,
      // postgres extensions (without postgis)
      doobie.postgres.Instances,
      doobie.postgres.hi.Modules,
      doobie.postgres.free.Modules,
      doobie.postgres.free.Types,
      doobie.postgres.free.Instances,
      doobie.postgres.syntax.ToPostgresMonadErrorOps,
      doobie.postgres.syntax.ToFragmentOps,
      doobie.postgres.syntax.ToPostgresExplainOps,
      // Java Time extensions
      doobie.util.meta.MetaConstructors,
      doobie.util.meta.TimeMetaInstances {

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

  given [E]: Meta[ID[E]] =
    ID.unsafeMakeF[Meta, E](Meta[UUID])

  given [E]: Meta[Set[ID[E]]] =
    ID.unsafeMakeF[[A] =>> Meta[Set[A]], E](unliftedUUIDArrayType.imap[Set[UUID]](_.toSet)(_.toArray))

  given Meta[CreationTime] = CreationTime.unsafeMakeF[Meta](JavaOffsetDateTimeMeta)

  given Meta[ModificationTime] = ModificationTime.unsafeMakeF[Meta](JavaOffsetDateTimeMeta)

  // handle updateable

  extension [A](updatable: Updatable[A]) {
    def toUpdateFragment(columnName: Fragment)(using Put[A]): Option[Fragment] =
      updatable.fold(value => (columnName ++ fr" = ${value}").some, none[Fragment])
  }

  extension [A](updatable: OptionUpdatable[A]) {
    def toUpdateFragment(columnName: Fragment)(using Put[A]): Option[Fragment] =
      updatable.fold(value => (columnName ++ fr"= ${value}").some, (columnName ++ fr"= null").some, none[Fragment])
  }

  extension (fragment: Fragment) {
    def exists(label: String): ConnectionIO[Boolean] =
      (fr"SELECT EXISTS(" ++ fragment ++ fr")").queryWithLabel[Boolean](label).unique

    def paginate[Entity: Read](
      offset: Paginated.Offset,
      limit:  Paginated.Limit,
      label:  String
    ): ConnectionIO[Paginated[Entity]] = {
      val o: Long = offset.unwrap
      val l: Int  = limit.unwrap
      // limit 1 entity more than returned to check if there is a next page in pagination
      (fragment ++ fr"LIMIT ${l + 1} OFFSET ${o}").queryWithLabel[Entity](label).to[List].map { entities =>
        val result     = entities.take(l)
        val nextOffset = if (entities.sizeIs <= l) None else Paginated.Offset.make(o + l).toOption
        Paginated(result, nextOffset)
      }
    }
  }

  // handle errors

  extension [A](query: Query0[A]) {
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def failNotFound[Entity](entity: String, id: ID[Entity])(using CodePosition): ConnectionIO[A] =
      query.unique.orRaise(CommonError.notFound(entity, id.asInstanceOf[ID[Any]]))
  }

  // log results

  def doobieLogger[F[_]](logger: Logger[F]): LogHandler[F] = {
    case doobie.util.log.Success(sql, _, label, exec, processing) =>
      logger.trace(
        s"""SQL succeeded:
           |${label}
           |${sql}
           |execution:  ${exec.toMillis.toString} ms
           |processing: ${processing.toMillis.toString} ms
           |total:      ${(exec + processing).toMillis.toString} ms""".stripMargin
      )
    case doobie.util.log.ExecFailure(sql, _, label, exec, failure) =>
      logger.error(failure)(
        s"""SQL failed at execution:
           |${label}
           |${sql}
           |failure cause:
           |${failure.getMessage} ms
           |execution:  ${exec.toMillis.toString} ms""".stripMargin
      )
    case doobie.util.log.ProcessingFailure(sql, _, label, exec, processing, failure) =>
      logger.error(failure)(
        s"""SQL failed at processing:
           |${label}
           |${sql}
           |failure cause:
           |${failure.getMessage}
           |execution:  ${exec.toMillis.toString} ms
           |processing: ${processing.toMillis.toString} ms
           |total:      ${(exec + processing).toMillis.toString} ms""".stripMargin
      )
  }
}
