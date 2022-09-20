import sbt._
import Settings._
import sbtcrossproject.CrossPlugin.autoImport.{ CrossType, crossProject }
import com.typesafe.sbt.SbtNativePackager.Docker

Global / excludeLintKeys ++= Set(scalacOptions, trapExit)

lazy val root = project.root
  .setName("branchtalk")
  .setDescription("branchtalk build")
  .configureRoot
  .aggregate(
    commonJVM,
    commonInfrastructure,
    commonApiJVM,
    discussionsJVM,
    discussionsApiJVM,
    discussionsImpl,
    usersJVM,
    usersApiJVM,
    usersImpl,
    server,
    application
  )

lazy val scalaJsArtifacts = project.in(file("scala-js"))
  .setName("scala-js-artifacts")
  .setDescription("aggregates all Scala.js modules to publish")
  .configureRoot
  .aggregate(
    commonMacrosJS,
    commonJS,
    commonApiMacrosJS,
    commonApiJS,
    discussionsJS,
    discussionsApiJS,
    usersJS,
    usersApiJS
  )

addCommandAlias("fmt", ";scalafmt;Test/scalafmt;It/scalafmt")
addCommandAlias("fullTest", ";test;It/test")
addCommandAlias("fullCoverageTest", ";coverage;test;It/test;coverageReport;coverageAggregate")

// commons

val commonMacros =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .build
    .from("common-macros")
    .setName("common-macros")
    .setDescription("Common macro definitions")
    .configureModule
val commonMacrosJVM = commonMacros.jvm
val commonMacrosJS  = commonMacros.js

val common = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .build
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
      Dependencies.jfairy % Test,
      Dependencies.guice % Test, // required by jfairy on JDK 15+
      Dependencies.guiceAssisted % Test // required by jfairy on JDK 15+
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .settings(
    Compile / resourceGenerators += task[Seq[File]] {
      val file = (Compile / resourceManaged).value / "branchtalk-version.conf"
      IO.write(
        file,
        s"""# Populated by the build tool, used by e.g. OpenAPI to display version.
           |branchtalk-build {
           |  version = "${version.value}"
           |  commit  = "${git.gitHeadCommit.value.getOrElse("null")}"
           |  date    = "${git.gitHeadCommitDate.value.getOrElse("null")}"
           |}""".stripMargin
      )
      Seq(file)
    }
  )
  .dependsOn(commonMacros)
val commonJVM = common.jvm
val commonJS  = common.js

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
      Dependencies.redis4cats,
      Dependencies.refinedPureConfig
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .dependsOn(commonJVM)

val commonApiMacros = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .build
  .from("common-api-macros")
  .setName("common-api-macros")
  .setDescription("Common API macro definitions")
  .configureModule
val commonApiMacrosJVM = commonApiMacros.jvm
val commonApiMacrosJS  = commonApiMacros.js

val commonApi = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .build
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
  .dependsOn(common, commonApiMacros)
val commonApiJVM = commonApi.jvm
val commonApiJS  = commonApi.js

// discussions

val discussions = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .build
  .from("discussions")
  .setName("discussions")
  .setDescription("Discussions' published language")
  .configureModule
  .configureTests()
  .settings(
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .dependsOn(common)
val discussionsJVM = discussions.jvm
val discussionsJS  = discussions.js

val discussionsApi = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .build
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
val discussionsApiJVM = discussionsApi.jvm
val discussionsApiJS  = discussionsApi.js

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
  .dependsOn(discussionsJVM, commonJVM % "compile->compile;it->test")

// users

val users = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .build
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
val usersJVM = users.jvm
val usersJS  = users.js

val usersApi = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .build
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
val usersApiJVM = usersApi.jvm
val usersApiJS  = usersApi.js

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
  .dependsOn(usersJVM, discussionsJVM, commonJVM % "compile->compile;it->test")

// application

val server = project
  .from("server")
  .setName("server")
  .setDescription("Branchtalk backend business logic")
  .configureModule
  .configureIntegrationTests(requiresFork = true)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.decline,
      Dependencies.refinedDecline,
      Dependencies.jsoniterMacro,
      Dependencies.sttpCats % IntegrationTest,
      Dependencies.http4sBlaze,
      Dependencies.http4sPrometheus,
      Dependencies.tapirHttp4s,
      Dependencies.tapirOpenAPI,
      Dependencies.tapirSwaggerUI,
      Dependencies.tapirSTTP % IntegrationTest,
      Dependencies.macwire
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .dependsOn(commonInfrastructure, discussionsJVM, usersJVM, discussionsApiJVM, usersApiJVM)
  .dependsOn(discussionsImpl % "it->it", usersImpl % "it->it")

val application = project
  .from("app")
  .setName("app")
  .setDescription("Branchtalk backend application")
  .configureModule
  .configureRun("io.branchtalk.Main")
  .settings(
    dockerUpdateLatest := true,
    Docker / packageName := "branchtalk-server",
    Docker / dockerExposedPorts := Seq(8080),
    libraryDependencies ++= Seq(
      Dependencies.logbackJackson,
      Dependencies.logbackJsonClassic,
    ),
    customPredef("scala.util.chaining", "cats.implicits", "eu.timepit.refined.auto")
  )
  .dependsOn(server, discussionsImpl, usersImpl)
