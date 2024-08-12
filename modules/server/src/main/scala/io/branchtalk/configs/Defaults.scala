package io.branchtalk.configs

object Defaults {

  val host: String = "localhost"
  val port: Int    = 8080

  val runAPI:                    Boolean = false
  val runUsersProjections:       Boolean = false
  val runDiscussionsProjections: Boolean = false

  val defaultPaginationLimit: Int = 50
  val maxPaginationLimit:     Int = 100
}
