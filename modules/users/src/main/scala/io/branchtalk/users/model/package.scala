package io.branchtalk.users

import cats.{ Eq, Eval, Show }
import io.branchtalk.shared.model.ShowCompanionOps
import io.estatico.newtype.macros.newtype

package object model {

  @newtype final case class Permissions(set: Set[Permission]) {

    def append(permission: Permission): Permissions = Permissions(set + permission)
    def remove(permission: Permission): Permissions = Permissions(set - permission)

    def allow(permissions:     RequiredPermissions): Boolean     = Permissions.validatePermissions(permissions, this)
    def intersect(permissions: Permissions):         Permissions = Permissions(set intersect permissions.set)
  }
  object Permissions {
    def unapply(permissions: Permissions): Option[Set[Permission]] = permissions.set.some

    def empty: Permissions = Permissions(Set.empty)

    implicit val show: Show[Permissions] = Show.wrap(_.set.mkString(", "))
    implicit val eq:   Eq[Permissions]   = Eq.by(_.set)

    @SuppressWarnings(Array("org.wartremover.warts.All")) // Eval should be stack-safe
    def validatePermissions(required: RequiredPermissions, existing: Permissions): Boolean = {
      def permitted(permission: Permission) = existing.set.contains(permission)
      def evaluate(req:         RequiredPermissions): Eval[Boolean] = req match {
        case RequiredPermissions.Empty      => Eval.True
        case RequiredPermissions.AllOf(set) => Eval.later(set.forall(permitted))
        case RequiredPermissions.AnyOf(set) => Eval.later(set.exists(permitted))
        case RequiredPermissions.And(x, y)  => (evaluate(x), evaluate(y)).mapN(_ && _)
        case RequiredPermissions.Or(x, y)   => (evaluate(x), evaluate(y)).mapN(_ || _)
        case RequiredPermissions.Not(x)     => evaluate(x).map(!_)
      }
      evaluate(required).value
    }
  }
}
