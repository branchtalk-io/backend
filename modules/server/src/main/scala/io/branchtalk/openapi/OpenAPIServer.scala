package io.branchtalk.openapi

import cats.data.NonEmptyList
import cats.effect.Sync
import hearth.kindlings.tapiropenapijsoniter.TapirOpenApi
import io.branchtalk.api
import io.branchtalk.configs.APIInfo
import org.http4s.HttpRoutes
import sttp.apispec.openapi.*
import sttp.tapir.docs.openapi.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.collection.immutable.ListMap

final class OpenAPIServer[F[_]: Sync](
  apiInfo:   APIInfo,
  endpoints: NonEmptyList[ServerEndpoint[Any, F]]
) {

  private val removedName = classOf[api.RequiredPermissions].getName
  private def fixPathItem(pathItem: PathItem) =
    pathItem.copy(parameters =
      pathItem.parameters.filterNot(_.fold(_.$ref.contains(removedName), _.name.contains(removedName)))
    )

  def openAPI: OpenAPI = OpenAPIDocsInterpreter(OpenAPIServer.openAPIDocsOptions)
    .toOpenAPI(endpoints.map(_.endpoint).toList, apiInfo.toOpenAPI)
    // TODO: quicklens
    .pipe(oa => oa.copy(paths = oa.paths.copy(pathItems = oa.paths.pathItems.view.mapValues(fixPathItem).to(ListMap))))

  // Serialization provided by Kindlings' tapir-openapi-jsoniter (replaces the hand-written jsoniter codecs).
  val openAPIJson: String = TapirOpenApi.toJson(openAPI)

  val routes: HttpRoutes[F] = new SwaggerHttp4s(yaml = openAPIJson, yamlName = "swagger.json").routes
}
object OpenAPIServer {

  private given openAPIDocsOptions: OpenAPIDocsOptions = OpenAPIDocsOptions.default
}
