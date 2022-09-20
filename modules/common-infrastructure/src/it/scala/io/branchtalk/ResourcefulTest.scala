package io.branchtalk

import cats.effect.std.Dispatcher
import cats.effect.{ IO, Resource }
import cats.effect.unsafe.implicits.global
import io.prometheus.client.CollectorRegistry
import org.specs2.specification.BeforeAfterAll

trait ResourcefulTest extends BeforeAfterAll {

  // populated by resources
  protected var registry:            CollectorRegistry = _
  implicit protected var dispatcher: Dispatcher[IO]    = _

  protected def testResource: Resource[IO, Unit] = {
    Resource.make(IO(new CollectorRegistry().tap(registry = _)))(cr => IO(cr.clear())) >>
      Dispatcher[IO].map(dispatcher = _)
  }.void

  private var release:      IO[Unit] = IO.unit
  override def beforeAll(): Unit     = release = testResource.allocated.unsafeRunSync()._2
  override def afterAll():  Unit     = release.unsafeRunSync()
}
