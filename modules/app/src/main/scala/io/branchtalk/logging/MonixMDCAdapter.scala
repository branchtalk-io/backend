package io.branchtalk.logging

import java.{ util => ju }

import monix.execution.misc.Local
import ch.qos.logback.classic.util.LogbackMDCAdapter

// Solution described by OlegPy in https://olegpy.com/better-logging-monix-1/
final class MonixMDCAdapter extends LogbackMDCAdapter {
  private[this] val map = Local[ju.Map[String, String]](ju.Collections.emptyMap())

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  override def put(key: String, `val`: String): Unit = {
    if (map() eq ju.Collections.EMPTY_MAP) {
      map := new ju.HashMap()
    }
    map().put(key, `val`)
    ()
  }

  override def get(key: String): String = map().get(key)
  override def remove(key: String): Unit = {
    map().remove(key)
    ()
  }

  // Note: we're resetting the Local to default, not clearing the actual hashmap
  override def clear():             Unit                   = map.clear()
  override def getCopyOfContextMap: ju.Map[String, String] = new ju.HashMap(map())
  override def setContextMap(contextMap: ju.Map[String, String]): Unit =
    map := new ju.HashMap(contextMap)

  override def getPropertyMap: ju.Map[String, String] = map()
  override def getKeys:        ju.Set[String]         = map().keySet()
}
object MonixMDCAdapter {

  // Initialize MDC.mdcAdapter (with default scope) to our implementation.
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def configure(): Unit = {
    val field = classOf[org.slf4j.MDC].getDeclaredField("mdcAdapter")
    field.setAccessible(true)
    field.set(null, new MonixMDCAdapter) // scalastyle:ignore null
  }
}
