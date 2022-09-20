package io.branchtalk.api

import java.util.Base64
import cats.effect.SyncIO
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.branchtalk.api.Authentication.{ Credentials, Session }
import io.branchtalk.shared.model.{ ParseRefined, UUIDGenerator, branchtalkCharset }
import io.branchtalk.shared.model.UUIDGenerator.FastUUIDGenerator
import sttp.tapir._

import scala.util.Try

// Authentication-related definitions for Tapir.
object AuthenticationSupport {

  private object base64 { // scalastyle:ignore object.name
    def apply(string:   String): String = Base64.getEncoder.encodeToString(string.getBytes(branchtalkCharset))
    def unapply(string: String): Option[String] =
      Try(new String(Base64.getDecoder.decode(string), branchtalkCharset)).toOption
  }

  private val basicR = raw"Basic (.+)".r
  private val upR    = raw"([^:]+):(.+)".r
  private object basic { // scalastyle:ignore object.name
    def apply(username: String, password: Array[Byte] Refined NonEmpty): String =
      s"""Basic ${base64(s"${username}:${new String(password.value, branchtalkCharset)}")}"""
    def unapply(string: String): Option[(String, Array[Byte] Refined NonEmpty)] = string match {
      case basicR(base64(upR(username, password))) =>
        ParseRefined[SyncIO]
          .parse[NonEmpty](password.getBytes(branchtalkCharset))
          .attempt
          .unsafeRunSync()
          .toOption
          .map(username -> _)
      case _ => None
    }
  }

  private val bearerR = raw"Bearer (.+)".r
  private object bearer { // scalastyle:ignore object.name
    def apply(sessionID: String): String = s"""Bearer ${sessionID}"""
    def unapply(string:  String): Option[String] = string match {
      case bearerR(sessionID) => Some(sessionID.trim)
      case _                  => None
    }
  }

  implicit private class ResultOps[A](private val io: SyncIO[A]) extends AnyVal {

    def asResult(original: String): DecodeResult[A] = io.attempt.unsafeRunSync() match {
      case Left(value)  => DecodeResult.Error(original, value)
      case Right(value) => DecodeResult.Value(value)
    }
  }

  val authHeaderMapping: Mapping[String, Authentication] = Mapping.fromDecode[String, Authentication] {
    case original @ basic(user, pass) =>
      (Username.parse[SyncIO](user), Password.parse[SyncIO](pass)).mapN(Credentials.apply).asResult(original)
    case original if original.startsWith("Basic") =>
      DecodeResult.Error(original, new Exception("Expected base64-encoded username:password"))
    case original @ bearer(sessionID) =>
      implicit val uuidGenerator: UUIDGenerator.FastUUIDGenerator.type = FastUUIDGenerator // passing it down it PITA
      SessionID.parse[SyncIO](sessionID).map(Session.apply).asResult(original)
    case original if original.startsWith("Bearer") =>
      DecodeResult.Error(original, new Exception("Expected session ID"))
    case original =>
      DecodeResult.Error(original, new Exception("Unknown authentication type"))
  } {
    case Session(sessionID)              => bearer(sessionID.uuid.show)
    case Credentials(username, password) => basic(username.nonEmptyString.value, password.nonEmptyBytes)
  }
  val authHeader: EndpointIO.Header[Authentication] = header[String]("Authentication")
    .map(authHeaderMapping)
    .description(
      """Accepts basic authentication (`"Basic " + base64("uname:pass")`) and bearer token (`"Bearer " + sessionID`)"""
    )

  val optAuthHeaderMapping: Mapping[Option[String], Option[Authentication]] =
    Mapping.fromDecode[Option[String], Option[Authentication]] {
      case Some(value) => authHeaderMapping.decode(value).map(Some.apply)
      case None        => DecodeResult.Value(None)
    }(_.map(authHeaderMapping.encode))
  val optAuthHeader: EndpointIO.Header[Option[Authentication]] = header[Option[String]]("Authentication")
    .map(optAuthHeaderMapping)
    .description(
      """Accepts basic authentication (`"Basic " + base64("uname:pass")`) and bearer token (`"Bearer " + sessionID`)"""
    )
}
