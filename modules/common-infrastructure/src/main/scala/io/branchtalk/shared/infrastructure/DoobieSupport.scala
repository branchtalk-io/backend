package io.branchtalk.shared.infrastructure

import com.typesafe.scalalogging.Logger
import io.branchtalk.shared.models.{ OptionUpdatable, Updatable }
import io.estatico.newtype.Coercible

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

  // newtype automatic support
  implicit def coercibleMeta[R, N](implicit ev: Coercible[Meta[R], Meta[N]], R: Meta[R]): Meta[N] = ev(R)

  implicit class DoobieUpdatableOps[A](private val updatable: Updatable[A]) extends AnyVal {

    def toUpdateFragment(columnName: Fragment)(implicit meta: Put[A]): Option[Fragment] =
      updatable.fold(value => (columnName ++ fr" = ${value}").some, none[Fragment])
  }

  implicit class DoobieOptionUpdatableOps[A](private val updatable: OptionUpdatable[A]) extends AnyVal {

    def toUpdateFragment(columnName: Fragment)(implicit meta: Put[A]): Option[Fragment] =
      updatable.fold(value => (columnName ++ fr"= ${value}").some, (columnName ++ fr"= null").some, none[Fragment])
  }

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
