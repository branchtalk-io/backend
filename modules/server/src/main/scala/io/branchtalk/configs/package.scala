package io.branchtalk

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import io.branchtalk.api.Pagination.Limit
import io.branchtalk.shared.infrastructure.PureconfigSupport.*

package object configs {

  implicit val paginationLimitReader: ConfigReader[Pagination.Limit] =
    ConfigReader[Int Refined Positive].map(Pagination.Limit(_))
}
