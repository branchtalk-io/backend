import sbt._
import Settings._

lazy val root = project.root
  .setName("branchtalk")
  .setDescription("branchtalk build")
  .configureRoot
  .aggregate(common, commonInfrastructure, commonApi, discussions, discussionsApi, discussionsImpl, application)

addCommandAlias("fullTest", ";test")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate")

// commons

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

val commonInfrastructure = project
  .from("common-infrastructure")
  .setName("common-infrastructure")
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

val commonApi = project
  .from("common-api")
  .setName("common-api")
  .setDescription("Infrastructure-dependent implementations")
  .configureModule
  .configureTests()
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.tapir,
      Dependencies.tapirJsoniter
    )
  )
  .dependsOn(common)

// discussions

val discussions = project
  .from("discussions")
  .setName("discussions")
  .setDescription("Discussions' published language")
  .configureModule
  .configureTests()
  .dependsOn(common)

val discussionsApi = project
  .from("discussions-api")
  .setName("discussions-api")
  .setDescription("Discussions' HTTP API")
  .configureModule
  .configureTests()
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.jsoniterMacro
    )
  )
  .dependsOn(commonApi)

val discussionsImpl = project
  .from("discussions-impl")
  .setName("discussions-impl")
  .setDescription("Discussions' Reads, Writes and Services' implementations")
  .configureModule
  .configureTests()
  .dependsOn(commonInfrastructure, discussions)

// application

val application = project
  .from("app")
  .setName("app")
  .setDescription("Branchtalk backend application and business logic")
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
  .dependsOn(discussionsImpl, discussionsApi)
