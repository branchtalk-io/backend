package io.branchtalk

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import io.branchtalk.api.PaginationLimit
import io.branchtalk.shared.infrastructure.PureconfigSupport._

package object configs {

  implicit val paginationLimitReader: ConfigReader[PaginationLimit] =
    ConfigReader[Int Refined Positive].map(PaginationLimit(_))
}
