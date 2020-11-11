package io.branchtalk

import io.branchtalk.shared.models.{ TupleAppender, TuplePrepender }
import sttp.tapir.Endpoint

package object auth {

  implicit class AuthOps[I, E, O, S](private val endpoint: Endpoint[I, E, O, S]) extends AnyVal {

    def authenticated[Inter, I2](implicit
      preAuth: TuplePrepender.Aux[Inter, api.Authentication, I],
      preUser: TuplePrepender.Aux[Inter, (users.model.User, Option[users.model.Session]), I2]
    ): AuthenticateLogic[I, I2, E, O, S] = new AuthenticateLogic[I, I2, E, O, S] {
      protected type Rest = Inter
      protected val mapped      = endpoint
      protected val extractAuth = preAuth.revert(_)
      protected val addUser     = preUser.prepend(_, _)
    }

    def optAuthenticated[Inter, I2](implicit
      preAuth: TuplePrepender.Aux[Inter, Option[api.Authentication], I],
      preUser: TuplePrepender.Aux[Inter, (Option[users.model.User], Option[users.model.Session]), I2]
    ): OptAuthenticateLogic[I, I2, E, O, S] = new OptAuthenticateLogic[I, I2, E, O, S] {
      protected type Rest = Inter
      protected val mapped      = endpoint
      protected val extractAuth = preAuth.revert(_)
      protected val addUser     = preUser.prepend(_, _)
    }

    def authorized[Inter1, Inter2, I2](implicit
      preAuth: TuplePrepender.Aux[Inter1, api.Authentication, I],
      appPerm: TupleAppender.Aux[Inter2, api.RequiredPermissions, Inter1],
      preUser: TuplePrepender.Aux[Inter2, (users.model.User, Option[users.model.Session]), I2]
    ): AuthorizeLogic[I, I2, E, O, S] = new AuthorizeLogic[I, I2, E, O, S] {
      protected type Rest1 = Inter1
      protected type Rest2 = Inter2
      protected val mapped      = endpoint
      protected val extractAuth = preAuth.revert(_)
      protected val extractPerm = appPerm.revert(_)
      protected val addUser     = preUser.prepend(_, _)
    }
  }
}
