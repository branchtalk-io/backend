package io.branchtalk.users.model

import cats.{ Eq, Eval, Show }

type Permissions = Permissions.Type
object Permissions extends Newtype[Set[Permission]] {
  def unapply(permissions: Permissions): Some[Set[Permission]] = Some(permissions.unwrap)

  def empty: Permissions = Permissions(Set.empty)

  @SuppressWarnings(Array("org.wartremover.warts.All")) // Eval should be stack-safe
  def validatePermissions(required: RequiredPermissions, existing: Permissions): Boolean = {
    def permitted(permission: Permission) = existing.unwrap.contains(permission)
    def evaluate(req: RequiredPermissions): Eval[Boolean] = Eval.later(req).flatMap {
      case RequiredPermissions.Empty      => Eval.True
      case RequiredPermissions.AllOf(set) => Eval.later(set.forall(permitted))
      case RequiredPermissions.AnyOf(set) => Eval.later(set.exists(permitted))
      case RequiredPermissions.And(x, y)  => (evaluate(x), evaluate(y)).mapN(_ && _)
      case RequiredPermissions.Or(x, y)   => (evaluate(x), evaluate(y)).mapN(_ || _)
      case RequiredPermissions.Not(x)     => evaluate(x).map(!_)
    }
    evaluate(required).value
  }

  extension (permissions: Permissions) {

    def append(permission: Permission): Permissions = unsafeMake(permissions.unwrap + permission)
    def remove(permission: Permission): Permissions = unsafeMake(permissions.unwrap - permission)

    def allow(required:    RequiredPermissions): Boolean     = validatePermissions(required, permissions)
    def intersect(another: Permissions):         Permissions = unsafeMake(permissions.unwrap intersect another.unwrap)
  }

  given Show[Permissions] = unsafeMakeF[Show](Show[Set[Permission]])
  given Eq[Permissions]   = unsafeMakeF[Eq](Eq[Set[Permission]])
}
