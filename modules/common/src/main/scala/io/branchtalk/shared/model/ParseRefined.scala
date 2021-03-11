package io.branchtalk.shared.model

import cats.effect.Sync
import eu.timepit.refined._
import eu.timepit.refined.api.{ Refined, Validate }

object ParseRefined {

  def apply[F[_]]: ApplyF[F] = new ApplyF[F]

  class ApplyF[F[_]] {
    def parse[P]: ApplyFP[F, P] = new ApplyFP[F, P]
  }

  class ApplyFP[F[_], P] {
    def apply[T](t: T)(implicit F: Sync[F], validate: Validate[T, P]): F[T Refined P] =
      F.defer {
        F.fromEither {
          refineV[P](t).leftMap(msg => new Throwable(msg))
        }
      }
  }
}
