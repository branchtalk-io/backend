package io.branchtalk.shared.infrastructure

trait TestResources extends TestPostgresResources with TestKafkaResources
object TestResources extends TestResources