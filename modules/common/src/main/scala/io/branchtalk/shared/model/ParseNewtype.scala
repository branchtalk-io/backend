package io.branchtalk.shared.model

import cats.data.NonEmptyList
import cats.effect.Sync
import neotype.*

object ParseNewtype {

  def apply[F[_]]: ApplyF[F] = new ApplyF[F]

  class ApplyF[F[_]] {
    def parse[P]: ApplyFP[F, P] = new ApplyFP[F, P]
  }

  class ApplyFP[F[_], P] {
    def apply[T](t: T)(using F: Sync[F], newtype: Newtype.WithType[T, P], codePosition: CodePosition): F[P] = F.defer {
      F.fromEither {
        newtype.make(t).leftMap(msg => CommonError.ValidationFailed(NonEmptyList.one(msg), codePosition))
      }
    }
  }
}
