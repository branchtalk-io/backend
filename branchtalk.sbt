import sbt._
import Settings._

lazy val root =
  project.root
    .setName("branchtalk")
    .setDescription("branchtalk build")
    .configureRoot
    .aggregate(common, infrastructure, discussions, discussionsApi, persistence, application)

val common = project
  .from("common")
  .setName("common")
  .setDescription("Common utilities")
  .configureModule
  .settings(libraryDependencies += Dependencies.catnip)
  .settings(
    Compile / resourceGenerators += task[Seq[File]] {
      val file = (Compile / resourceManaged).value / "branchtalk-version.conf"
      IO.write(file, s"version=${version.value}")
      Seq(file)
    }
  )

val infrastructure = project
  .from("infrastructure")
  .setName("infrastructure")
  .setDescription("Infrastructure-dependent implementations")
  .configureModule
  .configureTests()
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.doobie,
      Dependencies.doobieHikari,
      Dependencies.doobiePostgres,
      Dependencies.flyway,
      Dependencies.fs2,
      Dependencies.fs2IO,
      Dependencies.fs2Kafka,
      Dependencies.pureConfig,
      Dependencies.pureConfigCats,
      Dependencies.refinedPureConfig
    )
  )
  .dependsOn(common)

val discussions = project
  .from("discussions")
  .setName("discussions")
  .setDescription("Discussions' published language")
  .configureModule
  .configureTests()
  .dependsOn(common)

val discussionsApi = project
  .from("discussions-api")
  .setName("discussionsApi")
  .setDescription("Discussions' API")
  .configureModule
  .configureTests()
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.tapir,
      Dependencies.tapirJsoniter,
      Dependencies.jsoniterMacro
    )
  )
  .dependsOn(common)

val persistence = project
  .from("persistence")
  .setName("persistence")
  .setDescription("Writes projections and Reads queries")
  .configureModule
  .configureTests()
  .dependsOn(infrastructure, discussions)

val application = project
  .from("app")
  .setName("app")
  .setDescription("Backend application")
  .configureModule
  .configureTests()
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.decline,
      Dependencies.refinedDecline,
      Dependencies.monixExecution,
      Dependencies.monixEval,
      Dependencies.tapirHttp4s
    )
  )
  .dependsOn(persistence, discussionsApi)

addCommandAlias("fullTest", ";test")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate")
