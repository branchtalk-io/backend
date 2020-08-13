package io.branchtalk

import cats.effect.{ IO, Resource }
import org.specs2.specification.BeforeAfterAll

trait ResourcefulTest extends BeforeAfterAll {

  protected def testResource: Resource[IO, Unit] = Resource.pure[IO, Unit](())

  private var release:      IO[Unit] = IO.unit
  override def beforeAll(): Unit     = release = testResource.allocated.unsafeRunSync()._2
  override def afterAll():  Unit     = release.unsafeRunSync()
}
