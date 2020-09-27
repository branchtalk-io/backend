package io.branchtalk.configs

import eu.timepit.refined.auto._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

object Defaults {

  val host: String = "localhost"
  val port: Int    = 8080

  val runAPI:                    Boolean = false
  val runDiscussionsProjections: Boolean = false

  val defaultPaginationLimit: Int Refined Positive = 50
  val maxPaginationLimit:     Int Refined Positive = 100
}
