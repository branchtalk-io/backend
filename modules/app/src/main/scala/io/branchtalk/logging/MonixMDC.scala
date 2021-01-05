package io.branchtalk.logging

import monix.eval.Task

object MonixMDC extends MDC[Task] {

  override def enable[A](fa: Task[A]): Task[A] = fa.executeWithOptions(_.enableLocalContextPropagation)

  override def get(key: String): Task[Option[String]] = Task(org.log4s.MDC.get(key)).pipe(enable)

  override def set(key: String, value: String): Task[Unit] = Task(org.log4s.MDC(key) = value).pipe(enable)
}
