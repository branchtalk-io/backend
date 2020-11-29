import sbt._
import Settings._
import com.typesafe.sbt.SbtNativePackager.Docker

Global / excludeLintKeys ++= Set(scalacOptions, trapExit)

lazy val root = project.root
  .setName("branchtalk")
  .setDescription("branchtalk build")
  .configureRoot
  .aggregate(common,
             commonInfrastructure,
             commonApi,
             discussions,
             discussionsApi,
             discussionsImpl,
             users,
             usersApi,
             usersImpl,
             application)

addCommandAlias("fmt", ";scalafmt;test:scalafmt;it:scalafmt")
addCommandAlias("fullTest", ";test;it:test")
addCommandAlias("fullCoverageTest", ";coverage;test;it:test;coverageReport;coverageAggregate")

// commons

val common = project
  .from("common")
  .setName("common")
  .setDescription("Common utilities")
  .configureModule
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.avro4s,
      Dependencies.avro4sRefined,
      Dependencies.catnip,
      Dependencies.sourcecode,
      Dependencies.jfairy % Test
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .settings(
    Compile / resourceGenerators += task[Seq[File]] {
      val file = (Compile / resourceManaged).value / "branchtalk-version.conf"
      IO.write(file, s"""branchtalk-build {
                        |  version = "${version.value}"
                        |  commit  = "${git.gitHeadCommit.value.getOrElse("null")}"
                        |  date    = "${git.gitHeadCommitDate.value.getOrElse("null")}"
                        |}""".stripMargin)
      Seq(file)
    }
  )

val commonInfrastructure = project
  .from("common-infrastructure")
  .setName("common-infrastructure")
  .setDescription("Infrastructure-dependent implementations")
  .configureModule
  .configureIntegrationTests()
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.doobie,
      Dependencies.doobieHikari,
      Dependencies.doobiePostgres,
      Dependencies.doobieRefined,
      Dependencies.flyway,
      Dependencies.fs2,
      Dependencies.fs2IO,
      Dependencies.fs2Kafka,
      Dependencies.prometheus,
      Dependencies.pureConfig,
      Dependencies.pureConfigCats,
      Dependencies.pureConfigEnumeratum,
      Dependencies.refinedPureConfig
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
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
      Dependencies.jsoniter,
      Dependencies.jsoniterMacro,
      Dependencies.tapir,
      Dependencies.tapirJsoniter,
      Dependencies.tapirRefined
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .dependsOn(common)

// discussions

val discussions = project
  .from("discussions")
  .setName("discussions")
  .setDescription("Discussions' published language")
  .configureModule
  .configureTests()
  .settings(
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
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
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .dependsOn(commonApi, discussions)

val discussionsImpl = project
  .from("discussions-impl")
  .setName("discussions-impl")
  .setDescription("Discussions' Reads, Writes and Services' implementations")
  .configureModule
  .configureIntegrationTests(requiresFork = true)
  .settings(
    libraryDependencies += Dependencies.macwire,
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .compileAndTestDependsOn(commonInfrastructure)
  .dependsOn(discussions, common % "compile->compile;it->test")

// users

val users = project
  .from("users")
  .setName("users")
  .setDescription("Users' published language")
  .configureModule
  .configureTests()
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.bcrypt
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .dependsOn(common)

val usersApi = project
  .from("users-api")
  .setName("users-api")
  .setDescription("Users' HTTP API")
  .configureModule
  .configureTests()
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.jsoniterMacro
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .dependsOn(commonApi, users)

val usersImpl = project
  .from("users-impl")
  .setName("users-impl")
  .setDescription("Users' Reads, Writes and Services' implementations")
  .configureModule
  .configureIntegrationTests(requiresFork = true)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.jsoniter,
      Dependencies.jsoniterMacro,
      Dependencies.macwire
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .compileAndTestDependsOn(commonInfrastructure)
  .dependsOn(users, common % "compile->compile;it->test")

// application

val application = project
  .from("app")
  .setName("app")
  .setDescription("Branchtalk backend application and business logic")
  .configureModule
  .configureIntegrationTests(requiresFork = true)
  .configureRun("io.branchtalk.Main")
  .settings(
    Docker / packageName := "branchtalk-server",
    Docker / dockerAliases ++= Seq(
      DockerAlias(registryHost = None, username = None, name = "branchtalk", tag = Some("latest"))
    ),
    Docker / dockerExposedPorts := Seq(8080),
    libraryDependencies ++= Seq(
      Dependencies.decline,
      Dependencies.logbackJackson,
      Dependencies.logbackJsonClassic,
      Dependencies.jsoniter,
      Dependencies.jsoniterMacro,
      Dependencies.refinedDecline,
      Dependencies.monixExecution,
      Dependencies.monixEval,
      Dependencies.sttpCats % IntegrationTest,
      Dependencies.http4sPrometheus,
      Dependencies.tapirHttp4s,
      Dependencies.tapirOpenAPI,
      Dependencies.tapirSwaggerUI,
      Dependencies.tapirSTTP % IntegrationTest,
      Dependencies.macwire
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .compileAndTestDependsOn(discussionsImpl, usersImpl)
  .dependsOn(discussionsApi, usersApi)
