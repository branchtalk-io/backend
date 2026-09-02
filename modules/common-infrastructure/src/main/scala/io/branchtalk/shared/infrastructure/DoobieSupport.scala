package io.branchtalk.shared.infrastructure

import cats.Show
import cats.effect.{ Sync, SyncIO }
import org.typelevel.doobie.util.log
import io.branchtalk.logging.Logger
import io.branchtalk.shared.model.*
import org.tpolecat.typename.TypeName

// Allows `import DoobieSupport.*` instead of... a lot of imports.
// Additionally, provides support for a few useful but missing features.
object DoobieSupport
// basic functionalities
    extends org.typelevel.doobie.Aliases,
      org.typelevel.doobie.hi.Modules,
      org.typelevel.doobie.syntax.AllSyntax,
      org.typelevel.doobie.free.Modules,
      org.typelevel.doobie.free.Types,
      org.typelevel.doobie.free.Instances,
      // postgres extensions (without postgis)
      org.typelevel.doobie.postgres.Instances,
      org.typelevel.doobie.postgres.hi.Modules,
      org.typelevel.doobie.postgres.free.Modules,
      org.typelevel.doobie.postgres.free.Types,
      org.typelevel.doobie.postgres.free.Instances,
      org.typelevel.doobie.postgres.syntax.ToPostgresMonadErrorOps,
      org.typelevel.doobie.postgres.syntax.ToFragmentOps,
      org.typelevel.doobie.postgres.syntax.ToPostgresExplainOps,
      // Java Time extensions
      org.typelevel.doobie.util.meta.MetaConstructors,
      org.typelevel.doobie.util.meta.TimeMetaInstances,
      // doobie RC12 no longer derives Read/Write for case classes by default - opt back in for DAO products
      org.typelevel.doobie.generic.AutoDerivation {

  // enumeratum automatic support

  export enumeratum.Doobie.meta as enumeraturmMeta

  // newtype automatic support (neotype 0.7 exposes these as named given Get/Put/array instances keyed on WrappedType).
  // A wildcard export off a package is disallowed, so export by name; arrayGet needs a Show[Array[_]] (provided below).
  export neotype.interop.doobie.{ arrayGet, arrayPut, get, put, read, write }

  given showArray[A](using show: Show[A]): Show[Array[A]] =
    Show.show(_.map(show.show).mkString("[", ", ", "]"))

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
    case org.typelevel.doobie.util.log.Success(sql, _, label, exec, processing) =>
      logger.trace(
        s"""SQL succeeded:
           |${label}
           |${sql}
           |execution:  ${exec.toMillis.toString} ms
           |processing: ${processing.toMillis.toString} ms
           |total:      ${(exec + processing).toMillis.toString} ms""".stripMargin
      )
    case org.typelevel.doobie.util.log.ExecFailure(sql, _, label, exec, failure) =>
      logger.error(failure)(
        s"""SQL failed at execution:
           |${label}
           |${sql}
           |failure cause:
           |${failure.getMessage} ms
           |execution:  ${exec.toMillis.toString} ms""".stripMargin
      )
    case org.typelevel.doobie.util.log.ProcessingFailure(sql, _, label, exec, processing, failure) =>
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
