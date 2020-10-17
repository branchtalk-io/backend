package io.branchtalk.api

import cats.effect.{ ContextShift, Sync }
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.api._
import io.branchtalk.configs.APIInfo
import io.branchtalk.discussions.api.PostAPIs
import io.branchtalk.users.api.UserAPIs
import org.http4s.HttpRoutes
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi._
import sttp.tapir.openapi.OpenAPI.ReferenceOr
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.collection.immutable.ListMap

final class OpenAPIServer[F[_]: Sync: ContextShift](apiInfo: APIInfo) {

  import OpenAPIServer._

  val openAPIJson: String = writeToString((UserAPIs.endpoints ++ PostAPIs.endpoints).toOpenAPI(apiInfo.toOpenAPI))

  val openAPIRoutes: HttpRoutes[F] = new SwaggerHttp4s(openAPIJson).routes
}
object OpenAPIServer {

  private implicit val openAPIDocsOptions: OpenAPIDocsOptions = OpenAPIDocsOptions.default

  // technically, we only need encoder part so we can mock all the rest and call it a day
  trait JsEncoderOnly[T] extends JsCodec[T] {
    override def decodeValue(in: JsonReader, default: T): T = ???
    override def nullValue: T = null.asInstanceOf[T] // scalastyle:ignore null
    def encodeValue(x: T, out: JsonWriter): Unit
  }

  final case class Ref($ref: String) // helper, not sure if needed because I have not test
  implicit val referenceCodec: JsCodec[Reference] =
    summonCodec[Ref](JsonCodecMaker.make).map[Reference](r => Reference(r.$ref))(r => Ref(r.$ref))

  implicit def referenceOrCodec[T: JsCodec]: JsEncoderOnly[ReferenceOr[T]] =
    (x: ReferenceOr[T], out: JsonWriter) =>
      x.fold(referenceCodec.encodeValue(_, out), summonCodec[T].encodeValue(_, out))

  implicit val responseMapCodec: JsCodec[ListMap[ResponsesKey, ReferenceOr[Response]]] =
    summonCodec[Map[String, ReferenceOr[Response]]](JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true)))
      .map[ListMap[ResponsesKey, ReferenceOr[Response]]](_ => ???)(_.map {
        case (ResponsesDefaultKey, r)    => ("default", r)
        case (ResponsesCodeKey(code), r) => (code.toString, r)
      })

  implicit val openAPICodec: JsCodec[OpenAPI] =
    JsonCodecMaker.make[OpenAPI](CodecMakerConfig.withAllowRecursiveTypes(true))
}
