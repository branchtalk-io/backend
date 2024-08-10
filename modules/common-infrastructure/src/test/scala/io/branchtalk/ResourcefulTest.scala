package io.branchtalk

import cats.effect.std.Dispatcher
import cats.effect.{ IO, Resource }
import cats.effect.unsafe.implicits.global
import io.prometheus.client.CollectorRegistry
import org.specs2.specification.core.Execution

trait ResourcefulTest extends org.specs2.specification.Resource[Unit] {

  // populated by resources
  protected var registry:            CollectorRegistry = _
  implicit protected var dispatcher: Dispatcher[IO]    = _

  protected def testResource: Resource[IO, Unit] = {
    Resource.make(IO(new CollectorRegistry().tap(registry = _)))(cr => IO(cr.clear())) >>
      Dispatcher.parallel[IO].map(dispatcher = _)
  }.void

  private var release: IO[Unit] = IO.unit
  protected def acquire: scala.concurrent.Future[Unit] =
    testResource.allocated.map((_, releaseIO) => release = releaseIO).unsafeToFuture()
  protected def release(resource: Unit): Execution = release.unsafeRunSync()
}
