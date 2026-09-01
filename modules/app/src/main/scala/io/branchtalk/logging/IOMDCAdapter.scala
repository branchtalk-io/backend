package io.branchtalk.logging

import cats.effect.{ IO, IOLocal }

import java.util as ju
import org.slf4j.spi.MDCAdapter

import scala.jdk.CollectionConverters.*

// Bridges SLF4J's synchronous MDCAdapter API to a cats-effect IOLocal via IOLocal#unsafeThreadLocal (cats-effect 3.6+,
// enabled by -Dcats.effect.ioLocalPropagation=true), so a log statement running inside a fiber observes that fiber's
// context. Replaces the previous IOGlobal/IOLocalHack thread-local propagation hack (cats-effect now propagates the
// IOLocal to the running carrier thread itself).
@SuppressWarnings(Array("org.wartremover.warts.Null")) // talking to a Java interface that uses null as "absent"
final class IOMDCAdapter(threadLocal: ThreadLocal[MDC.Ctx]) extends MDCAdapter {

  private def getMDC:                          MDC.Ctx = Option(threadLocal.get).getOrElse(Map.empty[String, String])
  private def setMDC(mdc: MDC.Ctx):            Unit    = threadLocal.set(mdc)
  private def update(f:   MDC.Ctx => MDC.Ctx): Unit    = setMDC(f(getMDC))

  override def put(key:    String, `val`: String): Unit   = update(_.updated(key, `val`))
  override def get(key:    String):                String = getMDC.get(key).orNull
  override def remove(key: String):                Unit   = update(_.removed(key))
  override def clear():                            Unit   = setMDC(Map.empty)

  override def getCopyOfContextMap: ju.Map[String, String] = getMDC.asJava
  override def setContextMap(contextMap: ju.Map[String, String] @unchecked): Unit = setMDC(contextMap.asScala.toMap)

  // SLF4J 2.x per-key deque API - unused by branchtalk, but implemented faithfully (over a plain thread-local) so the
  // interface is fully satisfied without stubbing.
  private val deques: ThreadLocal[ju.Map[String, ju.Deque[String]]] =
    ThreadLocal.withInitial(() => new ju.HashMap[String, ju.Deque[String]]())

  override def pushByKey(key: String, value: String): Unit =
    deques.get.computeIfAbsent(key, _ => new ju.ArrayDeque[String]()).push(value)
  override def popByKey(key: String): String =
    Option(deques.get.get(key)).flatMap(deque => Option(deque.poll())).orNull
  override def getCopyOfDequeByKey(key: String): ju.Deque[String] =
    Option(deques.get.get(key)).map(deque => new ju.ArrayDeque[String](deque)).orNull
  override def clearDequeByKey(key: String): Unit =
    Option(deques.get.get(key)).foreach(_.clear())
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
          .set(null, new IOMDCAdapter(local.unsafeThreadLocal()))
      }
    } yield new IOMDC(local)
}
