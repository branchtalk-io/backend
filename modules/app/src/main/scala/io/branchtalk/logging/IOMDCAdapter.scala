package io.branchtalk.logging

import cats.effect.{ IO, IOLocal }

import java.{ util => ju }
import ch.qos.logback.classic.util.LogbackMDCAdapter

import scala.jdk.CollectionConverters._

// Based on solution described by OlegPy in https://olegpy.com/better-logging-monix-1/
// Using experimental hack: https://gist.github.com/MateuszKubuszok/d506706ee3c9b4c2291d47279f619523
final class IOMDCAdapter(local: IOLocal[MDC.Ctx]) extends LogbackMDCAdapter {

  private def getMDC: MDC.Ctx = IOGlobal.getCurrent(local).getOrElse(Map.empty[String, String])
  private def setMDC(mdc: MDC.Ctx):            Unit = IOGlobal.setTemporarily(local, mdc)
  private def update(f:   MDC.Ctx => MDC.Ctx): Unit = setMDC(f(getMDC))

  @SuppressWarnings(Array("org.wartremover.warts.Null")) // talking to Java interface
  override def get(key:    String): String = getMDC.get(key).orNull
  override def put(key:    String, `val`: String): Unit = update(_.updated(key, `val`))
  override def remove(key: String): Unit = update(_.removed(key))

  override def clear():             Unit                   = setMDC(Map.empty)
  override def getCopyOfContextMap: ju.Map[String, String] = getMDC.asJava
  override def setContextMap(contextMap: ju.Map[String, String]): Unit = setMDC(contextMap.asScala.toMap)

  override def getPropertyMap: ju.Map[String, String] = getMDC.asJava
  override def getKeys:        ju.Set[String]         = getMDC.asJava.keySet()
}
object IOMDCAdapter {

  // Initialize MDC.mdcAdapter (with default scope) to our implementation.
  @SuppressWarnings(Array("org.wartremover.warts.Null")) // null used to call static method
  def configure: IO[MDC[IO]] =
    for {
      local <- IOLocal(Map.empty[String, String])
      _ <- IO {
        classOf[org.slf4j.MDC]
          .getDeclaredField("mdcAdapter")
          .tap(_.setAccessible(true))
          .set(null, new IOMDCAdapter(local)) // scalastyle:ignore null
      }
    } yield new IOMDC(local)
}
