package io.branchtalk.openapi

import cats.data.NonEmptyList
import cats.effect.Sync
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import io.branchtalk.api
import io.branchtalk.api.JsoniterSupport.*
import io.branchtalk.configs.APIInfo
import org.http4s.HttpRoutes
import sttp.apispec.{
  Discriminator,
  ExampleMultipleValue,
  ExampleSingleValue,
  ExampleValue,
  ExtensionValue,
  ExternalDocumentation,
  OAuthFlow,
  OAuthFlows,
  Pattern,
  Schema,
  SchemaLike,
  SchemaType,
  SecurityRequirement,
  SecurityScheme,
  Tag
}
import sttp.apispec.openapi.ReferenceOr
import sttp.apispec.openapi.*
import sttp.tapir.docs.openapi.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.annotation.nowarn
import scala.collection.immutable.ListMap

final class OpenAPIServer[F[_]: Sync](
  apiInfo:   APIInfo,
  endpoints: NonEmptyList[ServerEndpoint[Any, F]]
) {

  import OpenAPIServer.{ *, given }

  private val removedName = classOf[api.RequiredPermissions].getName
  private def fixPathItem(pathItem: PathItem) =
    pathItem.copy(parameters =
      pathItem.parameters.filterNot(_.fold(_.$ref.contains(removedName), _.name.contains(removedName)))
    )

  def openAPI: OpenAPI = OpenAPIDocsInterpreter(OpenAPIServer.openAPIDocsOptions)
    .toOpenAPI(endpoints.map(_.endpoint).toList, apiInfo.toOpenAPI)
    // TODO: quicklens
    .pipe(oa => oa.copy(paths = oa.paths.copy(pathItems = oa.paths.pathItems.view.mapValues(fixPathItem).to(ListMap))))

  val openAPIJson: String = writeToString(openAPI)

  val routes: HttpRoutes[F] = new SwaggerHttp4s(yaml = openAPIJson, yamlName = "swagger.json").routes
}
@SuppressWarnings(Array("org.wartremover.warts.All")) // macros
object OpenAPIServer {

  private given openAPIDocsOptions: OpenAPIDocsOptions = OpenAPIDocsOptions.default

  // technically, we only need encoder part so we can mock all the rest and call it a day
  trait JsEncoderOnly[T] extends JsCodec[T] {
    override def decodeValue(in: JsonReader, default: T):          T = ???
    override def nullValue:                                        T = null.asInstanceOf[T]
    def encodeValue(x:           T, out:              JsonWriter): Unit
  }
  object JsEncoderOnly {
    def apply[T](f: (T, JsonWriter) => Unit): JsEncoderOnly[T] = (value: T, out: JsonWriter) => f(value, out)
  }

  given encoderReference: JsCodec[Reference] = JsonCodecMaker.make
  // apparently Jsoniter cannot find this ...
  def encoderReferenceOr[T: JsCodec]: JsCodec[ReferenceOr[T]] =
    JsEncoderOnly[ReferenceOr[T]] { (x, out) =>
      x.fold(encoderReference.encodeValue(_, out), summonCodec[T].encodeValue(_, out))
    }
  // so I have to apply this manually
  @nowarn("msg=Implicit resolves to enclosing value") // here this is just because of recursion
  given encoderReferenceOrSchema:           JsCodec[ReferenceOr[Schema]]      = encoderReferenceOr
  given encoderReferenceOrParameterCodec:   JsCodec[ReferenceOr[Parameter]]   = encoderReferenceOr
  given encoderReferenceOrRequestBodyCodec: JsCodec[ReferenceOr[RequestBody]] = encoderReferenceOr
  given encoderReferenceOrResponseCodec:    JsCodec[ReferenceOr[Response]]    = encoderReferenceOr
  given encoderReferenceOrExampleCodec:     JsCodec[ReferenceOr[Example]]     = encoderReferenceOr
  given encoderReferenceOrHeaderCodec:      JsCodec[ReferenceOr[Header]]      = encoderReferenceOr

  // TODO: support extension at all
  given extensionValue: JsCodec[ExtensionValue] = JsEncoderOnly[ExtensionValue] { (x, out) =>
    JsonCodecMaker.make[String].encodeValue(x.toString, out)
  }
  given encoderOAuthFlow:      JsCodec[OAuthFlow]      = JsonCodecMaker.make
  given encoderOAuthFlows:     JsCodec[OAuthFlows]     = JsonCodecMaker.make
  given encoderSecurityScheme: JsCodec[SecurityScheme] = JsonCodecMaker.make
  given encoderExampleSingleValue: JsCodec[ExampleSingleValue] = JsEncoderOnly {
    // TODO: handle parse -> encode JSON
    case (ExampleSingleValue(value: String), out)     => JsonCodecMaker.make[String].encodeValue(value, out)
    case (ExampleSingleValue(value: Int), out)        => JsonCodecMaker.make[Int].encodeValue(value, out)
    case (ExampleSingleValue(value: Long), out)       => JsonCodecMaker.make[Long].encodeValue(value, out)
    case (ExampleSingleValue(value: Float), out)      => JsonCodecMaker.make[Float].encodeValue(value, out)
    case (ExampleSingleValue(value: Double), out)     => JsonCodecMaker.make[Double].encodeValue(value, out)
    case (ExampleSingleValue(value: Boolean), out)    => JsonCodecMaker.make[Boolean].encodeValue(value, out)
    case (ExampleSingleValue(value: BigDecimal), out) => JsonCodecMaker.make[BigDecimal].encodeValue(value, out)
    case (ExampleSingleValue(value: BigInt), out)     => JsonCodecMaker.make[BigInt].encodeValue(value, out)
    case (ExampleSingleValue(null), out)              => JsonCodecMaker.make[Option[String]].encodeValue(None, out)
    case (ExampleSingleValue(value), out)             => JsonCodecMaker.make[String].encodeValue(value.toString, out)
  }
  val encodeExampleMultipleValues: JsCodec[ExampleMultipleValue] =
    JsonCodecMaker.make[List[ExampleSingleValue]].map[ExampleMultipleValue](_ => ???) {
      case ExampleMultipleValue(values) => values.map(ExampleSingleValue)
    }
  given encodeExampleValue: JsCodec[ExampleValue] = JsEncoderOnly[ExampleValue] {
    case (e: ExampleSingleValue, out)   => encoderExampleSingleValue.encodeValue(e, out)
    case (e: ExampleMultipleValue, out) => encodeExampleMultipleValues.encodeValue(e, out)
  }
  given encoderPattern: JsonKeyCodec[Pattern] = new JsonKeyCodec[Pattern] {
    private val impl = JsonCodecMaker.make[String]
    override def decodeKey(in: JsonReader):               Pattern = Pattern(impl.decodeValue(in, ""))
    override def encodeKey(x:  Pattern, out: JsonWriter): Unit    = impl.encodeValue(x.value, out)
  }
  given encoderSchemaType: JsCodec[SchemaType] = JsonCodecMaker.make[String].map(_ => ???)(_.value)
  given encoderSchemaLike: JsCodec[SchemaLike] = JsonCodecMaker.make
  given encoderSchema:     JsCodec[Schema]     = JsonCodecMaker.makeWithoutDiscriminator
  given encoderHeader:     JsCodec[Header]     = JsonCodecMaker.make
  given encoderExample:    JsCodec[Example]    = JsonCodecMaker.make
  given encoderResponse:   JsCodec[Response]   = JsonCodecMaker.make
  given encoderLink:       JsCodec[Link]       = JsonCodecMaker.make
  given encoderCallback: JsCodec[Callback] =
    encodeListMap(encoderReferenceOr[PathItem]).map[Callback](_ => ???)(_.pathItems)
  given encoderEncoding:       JsCodec[Encoding]       = JsonCodecMaker.make
  given encoderMediaType:      JsCodec[MediaType]      = JsonCodecMaker.make
  given encoderRequestBody:    JsCodec[RequestBody]    = JsonCodecMaker.make
  given encoderParameterStyle: JsCodec[ParameterStyle] = JsonCodecMaker.make[String].map(_ => ???)(_.value)
  given encoderParameterIn:    JsCodec[ParameterIn]    = JsonCodecMaker.make
  given encoderParameter:      JsCodec[Parameter]      = JsonCodecMaker.make
  given encoderResponseMap: JsCodec[ListMap[ResponsesKey, ReferenceOr[Response]]] =
    JsonCodecMaker
      .make[Map[String, ReferenceOr[Response]]](CodecMakerConfig.withAllowRecursiveTypes(true))
      .map[ListMap[ResponsesKey, ReferenceOr[Response]]](_ => ???)(
        _.map {
          case (ResponsesDefaultKey, r)      => ("default", r)
          case (ResponsesCodeKey(code), r)   => (code.toString, r)
          case (ResponsesRangeKey(range), r) => (s"${range}XX", r)
        }
      )
  // TODO: handle extensions one day
  given encoderResponses: JsCodec[Responses] = encoderResponseMap.map[Responses](_ => ???) {
    case Responses(responses, _) => responses
  }
  // this is needed to override the encoding of `security: List[SecurityRequirement]`. An empty security requirement
  // should be represented as an empty object (`{}`), not `null`, which is the default encoding of `ListMap`s.
  given encodeSecurityRequirement: JsCodec[List[SecurityRequirement]] =
    JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true).withTransientEmpty(true))
  given operationCodec:  JsCodec[Operation] = JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true))
  given encoderPathItem: JsCodec[PathItem]  = JsonCodecMaker.make
  given encoderPaths: JsCodec[Paths] =
    JsonCodecMaker.make[ListMap[String, PathItem]].map(_ => ???) { case Paths(pathItems, _) => pathItems }
  given encoderComponents:            JsCodec[Components]            = JsonCodecMaker.make
  given encoderServerVariable:        JsCodec[ServerVariable]        = JsonCodecMaker.make
  given encoderServer:                JsCodec[Server]                = JsonCodecMaker.make
  given encoderExternalDocumentation: JsCodec[ExternalDocumentation] = JsonCodecMaker.make
  given encoderTag:                   JsCodec[Tag]                   = JsonCodecMaker.make
  given encoderInfo:                  JsCodec[Info]                  = JsonCodecMaker.make
  given encoderContact:               JsCodec[Contact]               = JsonCodecMaker.make
  given encoderLicense:               JsCodec[License]               = JsonCodecMaker.make
  given encoderOpenAPI: JsCodec[OpenAPI] =
    JsonCodecMaker.make(CodecMakerConfig.withTransientDefault(false).withTransientNone(true))
  given encoderDiscriminator: JsCodec[Discriminator] = JsonCodecMaker.make

  given encodeList[T: JsCodec]: JsCodec[List[T]] = JsEncoderOnly[List[T]] {
    case (Nil, out)  => JsonCodecMaker.make[Option[T]](CodecMakerConfig.withTransientNone(false)).encodeValue(None, out)
    case (list, out) => JsonCodecMaker.make[Vector[T]].encodeValue(list.toVector, out)
  }

  given encodeListMap[V: JsCodec]: JsCodec[ListMap[String, V]] =
    JsonCodecMaker.make(CodecMakerConfig.withTransientEmpty(false))
}
