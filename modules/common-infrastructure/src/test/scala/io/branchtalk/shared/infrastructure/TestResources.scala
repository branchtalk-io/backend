package io.branchtalk.shared.infrastructure

trait TestResources extends TestPostgresResources, TestKafkaResources
object TestResources extends TestResources
