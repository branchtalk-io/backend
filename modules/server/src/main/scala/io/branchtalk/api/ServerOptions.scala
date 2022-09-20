package io.branchtalk.api

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.JsoniterSupport.JsCodec
import io.branchtalk.api.TapirSupport.jsonBody
import sttp.tapir.server.interceptor.decodefailure.{ DecodeFailureHandler, DefaultDecodeFailureHandler }
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.interceptor.DecodeFailureContext
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.{ DecodeResult, Schema, ValidationError }

object ServerOptions {

  final case class ErrorHandler[E](
    onMissing:         () => E,
    onMultiple:        () => E,
    onError:           (String, Throwable) => E,
    onMismatch:        (String, String) => E,
    onValidationError: List[ValidationError[_]] => E
  )

  def buildErrorHandler[E: JsCodec: Schema](errorHandler: ErrorHandler[E]): DecodeFailureHandler = {
    case handled if DefaultDecodeFailureHandler.default.respond(handled).isEmpty =>
      None
    case DecodeFailureContext(_, _, DecodeResult.Missing, _) =>
      Some(ValuedEndpointOutput(jsonBody[E], errorHandler.onMissing()))
    case DecodeFailureContext(_, _, DecodeResult.Multiple(_), _) =>
      Some(ValuedEndpointOutput(jsonBody[E], errorHandler.onMultiple()))
    case DecodeFailureContext(_, _, DecodeResult.Error(original, error), _) =>
      Some(ValuedEndpointOutput(jsonBody[E], errorHandler.onError(original, error)))
    case DecodeFailureContext(_, _, DecodeResult.Mismatch(expected, actual), _) =>
      Some(ValuedEndpointOutput(jsonBody[E], errorHandler.onMismatch(expected, actual)))
    case DecodeFailureContext(_, _, DecodeResult.InvalidValue(errors), _) =>
      Some(ValuedEndpointOutput(jsonBody[E], errorHandler.onValidationError(errors)))
  }

  def create[F[_]: Sync, E: JsCodec: Schema](
    logger:       Logger,
    errorHandler: ErrorHandler[E]
  ): Http4sServerOptions[F] =
    Http4sServerOptions
      .customiseInterceptors[F]
      .decodeFailureHandler(buildErrorHandler(errorHandler))
      .serverLog(
        DefaultServerLog[F](
          doLogWhenReceived = msg => Sync[F].delay(logger.debug(msg)),
          doLogWhenHandled = {
            case (msg, None)        => Sync[F].delay(logger.error(msg))
            case (msg, Some(error)) => Sync[F].delay(logger.error(msg, error))
          },
          doLogAllDecodeFailures = {
            case (msg, Some(ex)) => Sync[F].delay(logger.debug(msg, ex))
            case (msg, None)     => Sync[F].delay(logger.debug(msg))
          },
          doLogExceptions = (msg, ex) => Sync[F].delay(logger.error(msg, ex)),
          noLog = Sync[F].unit,
          logWhenHandled = true,
          logAllDecodeFailures = false,
          logLogicExceptions = true
        )
      )
      .options
}
