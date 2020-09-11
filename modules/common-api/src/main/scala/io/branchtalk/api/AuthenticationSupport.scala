package io.branchtalk.api

import java.util.Base64

import cats.effect.IO
import io.branchtalk.api.Authentication.{ Credentials, Session }
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.shared.models.UUIDGenerator.FastUUIDGenerator
import sttp.tapir.{ EndpointIO, _ }

import scala.util.Try

object AuthenticationSupport {

  private object base64 { // scalastyle:ignore object.name
    def apply(string:   String): String = Base64.getEncoder.encodeToString(string.getBytes)
    def unapply(string: String): Option[String] =
      Try(new String(Base64.getDecoder.decode(string))).toOption
  }

  private val basicR = raw"Basic (.+)".r
  private val upR    = raw"([^:]+):(.+)".r
  private object basic { // scalastyle:ignore object.name
    def apply(username: String, password: String): String = s"""Basic ${base64(s"$username:$password")}"""
    def unapply(string: String): Option[(String, String)] = string match {
      case basicR(base64(upR(username, password))) => Some(username.trim -> password.trim)
      case _                                       => None
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

  private implicit class ResultOps[A](private val io: IO[A]) extends AnyVal {

    def asResult(original: String): DecodeResult[A] = io.attempt.unsafeRunSync() match {
      case Left(value)  => DecodeResult.Error(original, value)
      case Right(value) => DecodeResult.Value(value)
    }
  }

  val authHeaderMapping: Mapping[String, Authentication] = Mapping.fromDecode[String, Authentication] {
    case original @ basic(user, pass) =>
      (Username.parse[IO](user), Password.parse[IO](pass)).mapN(Credentials.apply).asResult(original)
    case original if original.startsWith("Basic") =>
      DecodeResult.Error(original, new Exception("Expected base64-encoded username:password"))
    case original @ bearer(sessionID) =>
      implicit val uuidGenerator: UUIDGenerator.FastUUIDGenerator.type = FastUUIDGenerator // passing it down it PITA
      SessionID.parse[IO](sessionID).map(Session.apply).asResult(original)
    case original if original.startsWith("Bearer") =>
      DecodeResult.Error(original, new Exception("Expected session ID"))
    case original =>
      DecodeResult.Error(original, new Exception("Unknown authentication type"))
  } {
    case Session(sessionID)              => bearer(sessionID.value.show)
    case Credentials(username, password) => basic(username.value.value, password.value.value)
  }
  val authHeader: EndpointIO.Header[Authentication] = header[String]("Authentication").map(authHeaderMapping)

  val optAuthHeaderMapping: Mapping[Option[String], Option[Authentication]] =
    Mapping.fromDecode[Option[String], Option[Authentication]] {
      case Some(value) => authHeaderMapping.decode(value).map(Some.apply)
      case None        => DecodeResult.Value(None)
    } {
      _.map(authHeaderMapping.encode)
    }
  val optAuthHeader: EndpointIO.Header[Option[Authentication]] =
    header[Option[String]]("Authentication").map(optAuthHeaderMapping)
}
