package io.branchtalk

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.pureconfig._
import io.branchtalk.api.PaginationLimit
import pureconfig.ConfigReader

package object configs {

  implicit val paginationLimitReader: ConfigReader[PaginationLimit] =
    ConfigReader[Int Refined Positive].map(PaginationLimit(_))
}
