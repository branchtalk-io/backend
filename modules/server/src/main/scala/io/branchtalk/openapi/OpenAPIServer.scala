package io.branchtalk.openapi

import cats.data.NonEmptyList
import cats.effect.Sync
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.api
import io.branchtalk.api.JsoniterSupport._
import io.branchtalk.configs.APIInfo
import org.http4s.HttpRoutes
import monocle.macros.syntax.lens._
import sttp.apispec.{
  Discriminator,
  ExampleMultipleValue,
  ExampleSingleValue,
  ExampleValue,
  ExtensionValue,
  ExternalDocumentation,
  OAuthFlow,
  OAuthFlows,
  Reference,
  ReferenceOr,
  Schema,
  SchemaType,
  SecurityRequirement,
  SecurityScheme,
  Tag
}
import sttp.apispec.openapi._
import sttp.tapir.docs.openapi._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.annotation.nowarn
import scala.collection.immutable.ListMap

final class OpenAPIServer[F[_]: Sync](
  apiInfo:   APIInfo,
  endpoints: NonEmptyList[ServerEndpoint[Any, F]]
) {

  import OpenAPIServer._

  private val removedName = classOf[api.RequiredPermissions].getName
  private def fixPathItem(pathItem: PathItem) =
    pathItem.focus(_.parameters).modify(_.filterNot(_.fold(_.$ref.contains(removedName), _.name.contains(removedName))))

  def openAPI: OpenAPI = OpenAPIDocsInterpreter(OpenAPIServer.openAPIDocsOptions)
    .toOpenAPI(endpoints.map(_.endpoint).toList, apiInfo.toOpenAPI)
    .focus(_.paths.pathItems)
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

  implicit val encoderReference: JsCodec[Reference] = JsonCodecMaker.make
  // apparently Jsoniter cannot find this ...
  def encoderReferenceOr[T: JsCodec]: JsCodec[ReferenceOr[T]] =
    JsEncoderOnly[ReferenceOr[T]] { (x, out) =>
      x.fold(encoderReference.encodeValue(_, out), summonCodec[T].encodeValue(_, out))
    }
  // so I have to apply this manually
  @nowarn("msg=Implicit resolves to enclosing value") // here this is just because of recursion
  implicit lazy val encoderReferenceOrSchema:           JsCodec[ReferenceOr[Schema]]      = encoderReferenceOr
  implicit lazy val encoderReferenceOrParameterCodec:   JsCodec[ReferenceOr[Parameter]]   = encoderReferenceOr
  implicit lazy val encoderReferenceOrRequestBodyCodec: JsCodec[ReferenceOr[RequestBody]] = encoderReferenceOr
  implicit lazy val encoderReferenceOrResponseCodec:    JsCodec[ReferenceOr[Response]]    = encoderReferenceOr
  implicit lazy val encoderReferenceOrExampleCodec:     JsCodec[ReferenceOr[Example]]     = encoderReferenceOr
  implicit lazy val encoderReferenceOrHeaderCodec:      JsCodec[ReferenceOr[Header]]      = encoderReferenceOr

  // TODO: support extension at all
  implicit val extensionValue: JsCodec[ExtensionValue] = JsEncoderOnly[ExtensionValue] { (x, out) =>
    JsonCodecMaker.make[String].encodeValue(x.toString, out)
  }
  implicit val encoderOAuthFlow:      JsCodec[OAuthFlow]      = JsonCodecMaker.make
  implicit val encoderOAuthFlows:     JsCodec[OAuthFlows]     = JsonCodecMaker.make
  implicit val encoderSecurityScheme: JsCodec[SecurityScheme] = JsonCodecMaker.make
  implicit val encoderExampleSingleValue: JsCodec[ExampleSingleValue] = JsEncoderOnly {
    // TODO: handle parse -> encode JSON
    case (ExampleSingleValue(value: String), out) => JsonCodecMaker.make[String].encodeValue(value, out)
    case (ExampleSingleValue(value: Int), out) => JsonCodecMaker.make[Int].encodeValue(value, out)
    case (ExampleSingleValue(value: Long), out) => JsonCodecMaker.make[Long].encodeValue(value, out)
    case (ExampleSingleValue(value: Float), out) => JsonCodecMaker.make[Float].encodeValue(value, out)
    case (ExampleSingleValue(value: Double), out) => JsonCodecMaker.make[Double].encodeValue(value, out)
    case (ExampleSingleValue(value: Boolean), out) => JsonCodecMaker.make[Boolean].encodeValue(value, out)
    case (ExampleSingleValue(value: BigDecimal), out) => JsonCodecMaker.make[BigDecimal].encodeValue(value, out)
    case (ExampleSingleValue(value: BigInt), out) => JsonCodecMaker.make[BigInt].encodeValue(value, out)
    case (ExampleSingleValue(null), out) => // scalastyle:ignore null
      JsonCodecMaker.make[Option[String]].encodeValue(None, out)
    case (ExampleSingleValue(value), out) => JsonCodecMaker.make[String].encodeValue(value.toString, out)
  }
  val encodeExampleMultipleValues: JsCodec[ExampleMultipleValue] =
    summonCodec[List[ExampleSingleValue]](JsonCodecMaker.make).map[ExampleMultipleValue](_ => ???) {
      case ExampleMultipleValue(values) => values.map(ExampleSingleValue)
    }
  implicit val encodeExampleValue: JsCodec[ExampleValue] = JsEncoderOnly[ExampleValue] {
    case (e: ExampleSingleValue, out) => encoderExampleSingleValue.encodeValue(e, out)
    case (e: ExampleMultipleValue, out) => encodeExampleMultipleValues.encodeValue(e, out)
  }
  implicit val encoderSchemaType: JsCodec[SchemaType] = summonCodec[String](JsonCodecMaker.make).map(_ => ???)(_.value)
  implicit val encoderSchema:     JsCodec[Schema]     = JsonCodecMaker.make
  implicit val encoderHeader:     JsCodec[Header]     = JsonCodecMaker.make
  implicit val encoderExample:    JsCodec[Example]    = JsonCodecMaker.make
  implicit val encoderResponse:   JsCodec[Response]   = JsonCodecMaker.make
  implicit val encoderLink:       JsCodec[Link]       = JsonCodecMaker.make
  implicit lazy val encoderCallback: JsCodec[Callback] =
    encodeListMap(encoderReferenceOr[PathItem]).map[Callback](_ => ???)(_.pathItems)
  implicit val encoderEncoding:    JsCodec[Encoding]    = JsonCodecMaker.make
  implicit val encoderMediaType:   JsCodec[MediaType]   = JsonCodecMaker.make
  implicit val encoderRequestBody: JsCodec[RequestBody] = JsonCodecMaker.make
  implicit val encoderParameterStyle: JsCodec[ParameterStyle] =
    summonCodec[String](JsonCodecMaker.make).map(_ => ???)(_.value)
  implicit val encoderParameterIn: JsCodec[ParameterIn] = JsonCodecMaker.make
  implicit val encoderParameter:   JsCodec[Parameter]   = JsonCodecMaker.make
  implicit val encoderResponseMap: JsCodec[ListMap[ResponsesKey, ReferenceOr[Response]]] =
    summonCodec[Map[String, ReferenceOr[Response]]](
      JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true))
    ).map[ListMap[ResponsesKey, ReferenceOr[Response]]](_ => ???)(
      _.map {
        case (ResponsesDefaultKey, r)      => ("default", r)
        case (ResponsesCodeKey(code), r)   => (code.toString, r)
        case (ResponsesRangeKey(range), r) => (s"${range}XX", r)
      }
    )
  // TODO: handle extensions one day
  implicit val encoderResponses: JsCodec[Responses] = encoderResponseMap.map[Responses](_ => ???) {
    case Responses(responses, _) => responses
  }
  // this is needed to override the encoding of `security: List[SecurityRequirement]`. An empty security requirement
  // should be represented as an empty object (`{}`), not `null`, which is the default encoding of `ListMap`s.
  implicit def encodeSecurityRequirement: JsCodec[List[SecurityRequirement]] =
    JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true).withTransientEmpty(true))
  implicit val operationCodec:  JsCodec[Operation] = JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true))
  implicit val encoderPathItem: JsCodec[PathItem]  = JsonCodecMaker.make
  implicit val encoderPaths: JsCodec[Paths] =
    summonCodec[ListMap[String, PathItem]](JsonCodecMaker.make).map(_ => ???) { case Paths(pathItems, _) =>
      pathItems
    }
  implicit val encoderComponents:            JsCodec[Components]            = JsonCodecMaker.make
  implicit val encoderServerVariable:        JsCodec[ServerVariable]        = JsonCodecMaker.make
  implicit val encoderServer:                JsCodec[Server]                = JsonCodecMaker.make
  implicit val encoderExternalDocumentation: JsCodec[ExternalDocumentation] = JsonCodecMaker.make
  implicit val encoderTag:                   JsCodec[Tag]                   = JsonCodecMaker.make
  implicit val encoderInfo:                  JsCodec[Info]                  = JsonCodecMaker.make
  implicit val encoderContact:               JsCodec[Contact]               = JsonCodecMaker.make
  implicit val encoderLicense:               JsCodec[License]               = JsonCodecMaker.make
  implicit val encoderOpenAPI: JsCodec[OpenAPI] =
    JsonCodecMaker.make(CodecMakerConfig.withTransientDefault(false).withTransientNone(true))
  implicit val encoderDiscriminator: JsCodec[Discriminator] = JsonCodecMaker.make

  implicit def encodeList[T: JsCodec]: JsCodec[List[T]] = JsEncoderOnly[List[T]] {
    case (Nil, out) =>
      summonCodec[Option[T]](JsonCodecMaker.make(CodecMakerConfig.withTransientNone(false))).encodeValue(None, out)
    case (list, out) =>
      summonCodec[Vector[T]](JsonCodecMaker.make).encodeValue(list.toVector, out)
  }

  implicit def encodeListMap[V: JsCodec]: JsCodec[ListMap[String, V]] =
    JsonCodecMaker.make(CodecMakerConfig.withTransientEmpty(false))
}
