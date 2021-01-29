package io.branchtalk

// Saves us writing everywhere `sealed trait X extends Product with Serializable`.
trait ADT extends Product with Serializable
