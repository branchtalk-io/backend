package io.branchtalk

import cats.effect.{ IO, Resource }
import io.prometheus.client.CollectorRegistry
import org.specs2.specification.BeforeAfterAll

trait ResourcefulTest extends BeforeAfterAll {

  // populated by resources
  protected var registry: CollectorRegistry = _
  protected def testResource: Resource[IO, Unit] =
    Resource.make(IO(new CollectorRegistry().tap(registry = _)))(cr => IO(cr.clear())).void

  private var release:      IO[Unit] = IO.unit
  override def beforeAll(): Unit     = release = testResource.allocated.unsafeRunSync()._2
  override def afterAll():  Unit     = release.unsafeRunSync()
}
