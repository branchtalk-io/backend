package io.branchtalk.openapi

import cats.data.NonEmptyList
import cats.effect.{ ContextShift, Sync }
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.api
import io.branchtalk.api.JsoniterSupport._
import io.branchtalk.configs.APIInfo
import org.http4s.HttpRoutes
import monocle.macros.syntax.lens._
import sttp.tapir.apispec._
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.annotation.nowarn
import scala.collection.immutable.ListMap

final class OpenAPIServer[F[_]: Sync: ContextShift](
  apiInfo:   APIInfo,
  endpoints: NonEmptyList[ServerEndpoint[_, _, _, Nothing, F]]
) {

  import OpenAPIServer._

  private val removedName = classOf[api.RequiredPermissions].getName
  private def fixPathItem(pathItem: PathItem) =
    pathItem.lens(_.parameters).modify(_.filterNot(_.fold(_.$ref.contains(removedName), _.name.contains(removedName))))

  def openAPI: OpenAPI = OpenAPIDocsInterpreter
    .toOpenAPI(endpoints.map(_.endpoint).toList, apiInfo.toOpenAPI)
    .lens(_.paths)
    .modify(_.view.mapValues(fixPathItem).to(ListMap))

  val openAPIJson: String = writeToString(openAPI)

  val routes: HttpRoutes[F] = new SwaggerHttp4s(yaml = openAPIJson, yamlName = "swagger.json").routes
}
@SuppressWarnings(Array("org.wartremover.warts.All")) // macros
object OpenAPIServer {

  implicit private val openAPIDocsOptions: OpenAPIDocsOptions = OpenAPIDocsOptions.default

  // technically, we only need encoder part so we can mock all the rest and call it a day
  trait JsEncoderOnly[T] extends JsCodec[T] {
    override def decodeValue(in: JsonReader, default: T): T = ???
    override def nullValue: T = null.asInstanceOf[T] // scalastyle:ignore null
    def encodeValue(x: T, out: JsonWriter): Unit
  }
  object JsEncoderOnly {
    def apply[T](f: (T, JsonWriter) => Unit): JsEncoderOnly[T] = (value: T, out: JsonWriter) => f(value, out)
  }

  final case class Ref($ref: String) // helper, not sure if needed because I have not test
  implicit val referenceCodec: JsCodec[Reference] =
    summonCodec[Ref](JsonCodecMaker.make).map[Reference](r => Reference(r.$ref))(r => Ref(r.$ref))

  // apparently Jsonite cannot find this if private...
  def referenceOrCodec[T: JsCodec]: JsCodec[ReferenceOr[T]] = JsEncoderOnly[ReferenceOr[T]] { (x, out) =>
    x.fold(referenceCodec.encodeValue(_, out), summonCodec[T].encodeValue(_, out))
  }
  // so I have to apply this manually
  @nowarn("msg=Implicit resolves to enclosing value") // here this is just because of recursion
  implicit val referenceOrSchemaCodec: JsCodec[ReferenceOr[Schema]] =
    referenceOrCodec(summonCodec[Schema](JsonCodecMaker.make))
  implicit val referenceOrParameterCodec: JsCodec[ReferenceOr[Parameter]] =
    referenceOrCodec(summonCodec[Parameter](JsonCodecMaker.make))
  implicit val referenceOrRequestBodyCodec: JsCodec[ReferenceOr[RequestBody]] =
    referenceOrCodec(summonCodec[RequestBody](JsonCodecMaker.make))
  implicit val referenceOrResponseCodec: JsCodec[ReferenceOr[Response]] =
    referenceOrCodec(summonCodec[Response](JsonCodecMaker.make))
  implicit val referenceOrExampleCodec: JsCodec[ReferenceOr[Example]] =
    referenceOrCodec(summonCodec[Example](JsonCodecMaker.make))
  implicit val referenceOrHeaderCodec: JsCodec[ReferenceOr[Header]] =
    referenceOrCodec(summonCodec[Header](JsonCodecMaker.make))

  implicit val exampleValueCodec: JsCodec[ExampleValue] = JsEncoderOnly[ExampleValue] {
    case (ExampleSingleValue(value), out)    => summonCodec[String](JsonCodecMaker.make).encodeValue(value, out)
    case (ExampleMultipleValue(values), out) => summonCodec[List[String]](JsonCodecMaker.make).encodeValue(values, out)
  }

  implicit val responseMapCodec: JsCodec[ListMap[ResponsesKey, ReferenceOr[Response]]] =
    summonCodec[Map[String, ReferenceOr[Response]]](
      JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true))
    ).map[ListMap[ResponsesKey, ReferenceOr[Response]]](_ => ???)(_.map {
      case (ResponsesDefaultKey, r)    => ("default", r)
      case (ResponsesCodeKey(code), r) => (code.toString, r)
    })

  // makes sure that parameters field of Operation will be empty as [], not as null (overrides listCodec from below)
  implicit val parametersCodec: JsCodec[List[ReferenceOr[Parameter]]] = summonCodec[List[ReferenceOr[Parameter]]](
    JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true).withTransientEmpty(false))
  )
  // this is needed to override the encoding of `security: List[SecurityRequirement]`. An empty security requirement
  // should be represented as an empty object (`{}`), not `null`, which is the default encoding of `ListMap`s.
  implicit val operationCodec: JsCodec[Operation] = summonCodec[Operation](
    JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true).withTransientEmpty(false))
  )

  implicit def listCodec[T: JsCodec]: JsCodec[List[T]] = JsEncoderOnly[List[T]] {
    case (Nil, out) =>
      summonCodec[Option[T]](JsonCodecMaker.make(CodecMakerConfig.withTransientNone(false))).encodeValue(None, out)
    case (list, out) =>
      summonCodec[Vector[T]](JsonCodecMaker.make).encodeValue(list.toVector, out)
  }

  implicit def listMapCodec[T: JsCodec]: JsCodec[ListMap[String, T]] =
    summonCodec[ListMap[String, T]](JsonCodecMaker.make)

  implicit val openAPICodec: JsCodec[OpenAPI] = summonCodec[OpenAPI](
    JsonCodecMaker.make(
      CodecMakerConfig
        .withAllowRecursiveTypes(true)
        .withTransientDefault(false)
        .withTransientEmpty(true)
        .withTransientNone(true)
    )
  )
}
