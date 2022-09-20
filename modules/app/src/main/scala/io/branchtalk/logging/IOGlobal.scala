package io.branchtalk.logging

import cats.effect.{ Async, IO, IOLocal, IOLocalHack }
import cats.effect.kernel.{ Cont, Deferred, Fiber, Poll, Ref, Sync }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/** Hack allowing us to:
  *  - read whole content from `IO.Local` on every call that process user provided function through IOLocalHack
  *  - put that content into `IOGlobal.threadLocal` so that unsafe functions integrating with no-FP code could
  *    read it through `IOGlobal.getCurrent(ioLocal)`
  *
  *  Requires passing `IOGlobal.configuredStatePropagation` instead of `IO.asyncForIO` into tagless final code.
  */
object IOGlobal {

  private val threadLocal: ThreadLocal[scala.collection.immutable.Map[IOLocal[_], Any]] =
    ThreadLocal.withInitial(() => scala.collection.immutable.Map.empty[IOLocal[_], Any])

  private def propagateState[A](thunk: => IO[A]): IO[A] =
    IOLocalHack.get.flatMap { state => threadLocal.set(state); thunk }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf")) // we know that value under IOLocal[A] should be A
  def getCurrent[A](local: IOLocal[A]): Option[A] = threadLocal.get().get(local).asInstanceOf[Option[A]]

  def setTemporarily[A](local: IOLocal[A], value: A): Unit = threadLocal.set(threadLocal.get().updated(local, value))

  def configureStatePropagation(tc: Async[IO]): Async[IO] = new Async[IO] {

    override def evalOn[A](fa: IO[A], ec: ExecutionContext): IO[A] = tc.evalOn(fa, ec)

    override def executionContext: IO[ExecutionContext] = tc.executionContext

    override def cont[K, R](body: Cont[IO, K, R]): IO[R] = tc.cont(body)

    override def sleep(time: FiniteDuration): IO[Unit] = tc.sleep(time)

    override def ref[A](a: A): IO[Ref[IO, A]] = tc.ref(a)

    override def deferred[A]: IO[Deferred[IO, A]] = tc.deferred

    override def suspend[A](hint: Sync.Type)(thunk: => A): IO[A] =
      tc.suspend(hint)(propagateState(tc.pure(thunk))).flatten

    override def raiseError[A](e: Throwable): IO[A] = tc.raiseError(e)

    override def handleErrorWith[A](fa: IO[A])(f: Throwable => IO[A]): IO[A] =
      tc.handleErrorWith(fa)(e => propagateState(f(e)))

    override def monotonic: IO[FiniteDuration] = tc.monotonic

    override def realTime: IO[FiniteDuration] = tc.realTime

    override def start[A](fa: IO[A]): IO[Fiber[IO, Throwable, A]] = tc.start(fa)

    override def cede: IO[Unit] = tc.cede

    override def forceR[A, B](fa: IO[A])(fb: IO[B]): IO[B] = tc.forceR(fa)(fb)

    override def uncancelable[A](body: Poll[IO] => IO[A]): IO[A] = tc.uncancelable(body)

    override def canceled: IO[Unit] = tc.canceled

    override def onCancel[A](fa: IO[A], fin: IO[Unit]): IO[A] = tc.onCancel(fa, fin)

    override def pure[A](x: A): IO[A] = tc.pure(x)

    override def flatMap[A, B](fa: IO[A])(f: A => IO[B]): IO[B] = tc.flatMap(fa)(a => propagateState(f(a)))

    override def tailRecM[A, B](a: A)(f: A => IO[Either[A, B]]): IO[B] = tc.tailRecM(a)(b => propagateState(f(b)))
  }

  def configuredStatePropagation: Async[IO] = configureStatePropagation(IO.asyncForIO)
}
