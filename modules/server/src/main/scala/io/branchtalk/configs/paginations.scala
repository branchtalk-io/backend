package io.branchtalk.configs

import io.branchtalk.api.Pagination
import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }

given paginationLimitReader: ConfigReader[Pagination.Limit] =
  summon[ConfigReader[Int]].emapString("Pagination.Limit")(Pagination.Limit.make)
