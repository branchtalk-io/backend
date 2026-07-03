import sbt.*
import com.typesafe.sbt.SbtNativePackager.Docker
import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings

Global / excludeLintKeys ++= Set(
  dockerExposedPorts,
  dockerUpdateLatest,
  fork,
  ideSkipProject,
  libraryDependencies,
  packageName,
  scalacOptions,
  trapExit
)

// Common settings:

val settings = Seq(
  organization := "io.branchtalk",
  scalaVersion := Dependencies.scalaVersion,
  scalacOptions ++= Seq(
    // standard settings
    // format: off
    "-encoding", "UTF-8",
    "-source", "3.3-migration",
    // format: on
    "-unchecked",
    "-deprecation",
    // "-explaintypes",
    "-feature",
    "-no-indent",
    // format: off
    "-Xmax-inlines", "64",
    // format: on
    "-Wconf:msg=The method `apply` is inserted:s",
    "-Wnonunit-statement",
    "-Wvalue-discard",
    // "-Xfatal-warnings",
    "-Ykind-projector:underscores"
  ),
  console / scalacOptions --= Seq(
    // warnings
    "-Ywarn-unused",
    // "-Wunused:imports", // import x.Underlying as X is marked as unused even though it is! probably one of https://github.com/scala/scala3/issues/: #18564, #19252, #19657, #19912
    "-Wunused:privates",
    "-Wunused:locals",
    "-Wunused:explicits",
    "-Wunused:implicits",
    "-Wunused:params",
    "-Wvalue-discard",
    "-Xfatal-warnings",
    "-Xcheck-macros",
    // advanced options
    "-Xfatal-warnings",
    // linting
    "-Xlint"
  ),
  Global / cancelable := true,
  Compile / trapExit := false,
  Compile / connectInput := true,
  Compile / outputStrategy := Some(StdoutOutput),
  libraryDependencies ++= Seq(
    Dependencies.cats,
    Dependencies.catsFree,
    Dependencies.catsEffect,
    Dependencies.alleycats,
    Dependencies.kindlingsCats,
    Dependencies.kindlingsCatsInterop,
    Dependencies.kindlingsShowPretty,
    Dependencies.chimney,
    Dependencies.enumeratum,
    Dependencies.fastuuid,
    Dependencies.uuidGenerator,
    Dependencies.log4cats,
    Dependencies.log4catsSlf4j,
    Dependencies.neotype,
    Dependencies.scalaLogging,
    Dependencies.quicklens,
    Dependencies.logback
  ),
  Compile / scalafmtOnCompile := true,
  Compile / compile / wartremoverWarnings ++= Warts.allBut(
    Wart.Any,
    Wart.DefaultArguments,
    Wart.ExplicitImplicitTypes,
    Wart.ImplicitConversion,
    Wart.ImplicitParameter,
    Wart.Overloading,
    Wart.PublicInference,
    Wart.NonUnitStatements,
    Wart.Nothing
  ),
  // used A LOT in Specs2
  Test / scalacOptions ++= Seq(
    "-Wconf:msg=unused value of type:s",
    "-language:implicitConversions"
  ),
  // don't publish
  publish / skip := true,
  publishArtifact := false
)

val tests = Seq(
  libraryDependencies ++= Seq(
    Dependencies.catsLaws % Test,
    Dependencies.spec2Core % Test,
    Dependencies.spec2Scalacheck % Test
  )
) ++ inConfig(Test)(
  Seq(
    scalafmtOnCompile := true,
    testFrameworks := Seq(TestFrameworks.Specs2),
    libraryDependencies ++= Seq(
      Dependencies.catsLaws % Test,
      Dependencies.spec2Core % Test,
      Dependencies.spec2Scalacheck % Test
    )
  )
) ++ inConfig(Test)(scalafmtConfigSettings)

val integrationTests = tests ++ Seq(
  Test / fork := true
)

def customPredef(imports: String*): Def.Setting[Task[Seq[String]]] =
  scalacOptions += s"-Yimports:${(Seq(
      "java.lang",
      "scala",
      "scala.Predef",
      // Kindlings cats derivation: brings `.derived` extensions onto cats type class companions
      // (Eq, Show, Order, Traverse, ...) so `derives Eq`/`ShowPretty`/... resolve everywhere.
      "hearth.kindlings.catsderivation.extensions"
    ) ++ imports).mkString(",")}"

// modules

lazy val root = project
  .in(file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "branchtalk",
    description := "branchtalk build"
  )
  .settings(settings *)
  .aggregate(common.projectRefs *)
  .aggregate(commonApi.projectRefs *)
  .aggregate(commonInfrastructure)
  .aggregate(discussions.projectRefs *)
  .aggregate(discussionsApi.projectRefs *)
  .aggregate(users.projectRefs *)
  .aggregate(usersApi.projectRefs *)
  .aggregate(usersImpl)
  .aggregate(server, application)

// commons

val common = projectMatrix
  .in(file("modules/common"))
  .jvmPlatform(List(Dependencies.scalaVersion))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "common",
    description := "Common utilities"
  )
  .settings(settings *)
  .settings(tests *)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.hearth,
      Dependencies.kindlingsAvro,
      Dependencies.jfairy % Test,
      Dependencies.guice % Test, // required by jfairy on JDK 15+
      Dependencies.guiceAssisted % Test // required by jfairy on JDK 15+
    ),
    customPredef("scala.util.chaining", "cats.implicits")
  )

val commonApi = projectMatrix
  .in(file("modules/common-api"))
  .jvmPlatform(List(Dependencies.scalaVersion))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "common-api",
    description := "Infrastructure-dependent implementations"
  )
  .settings(settings *)
  .settings(tests *)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.jsoniter,
      Dependencies.kindlingsJsoniter,
      Dependencies.neotypeTapir,
      Dependencies.neotypeJsoniter,
      Dependencies.tapir,
      Dependencies.tapirJsoniter
    ),
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .dependsOn(common)

val commonInfrastructure = project
  .in(file("modules/common-infrastructure"))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "common-infrastructure",
    description := "Infrastructure-dependent implementations"
  )
  .settings(settings *)
  .settings(integrationTests *)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.doobie,
      Dependencies.doobieHikari,
      Dependencies.doobiePostgres,
      Dependencies.enumeratumDoobie,
      Dependencies.flyway,
      Dependencies.flywayPostgres,
      Dependencies.fs2,
      Dependencies.fs2IO,
      Dependencies.fs2Kafka,
      Dependencies.neotypeDoobie,
      Dependencies.prometheus,
      Dependencies.kindlingsSconfig,
      Dependencies.sconfig,
      Dependencies.redis4cats
    ),
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .dependsOn(common.jvm(Dependencies.scalaVersion))

// discussions

val discussions = projectMatrix
  .in(file("modules/discussions"))
  .jvmPlatform(List(Dependencies.scalaVersion))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "discussions",
    description := "Discussions' published language"
  )
  .settings(settings)
  .settings(tests)
  .settings(
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .dependsOn(common)

val discussionsApi = projectMatrix
  .in(file("modules/discussions-api"))
  .jvmPlatform(List(Dependencies.scalaVersion))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "discussions-api",
    description := "Discussions' HTTP API"
  )
  .settings(settings)
  .settings(tests)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.kindlingsJsoniter
    ),
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .dependsOn(commonApi, discussions)

val discussionsImpl = project
  .in(file("modules/discussions-impl"))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "discussions-impl",
    description := "Discussions' Reads, Writes and Services' implementations"
  )
  .settings(settings)
  .settings(integrationTests)
  .settings(
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .dependsOn(
    common.jvm(Dependencies.scalaVersion) % s"$Compile->$Compile ; $Test->$Test",
    commonInfrastructure % s"$Compile->$Compile ; $Test->$Test",
    discussions.jvm(Dependencies.scalaVersion)
  )

// users

val users = projectMatrix
  .in(file("modules/users"))
  .jvmPlatform(List(Dependencies.scalaVersion))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "users",
    description := "Users' published language"
  )
  .settings(settings)
  .settings(tests)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.bcrypt
    ),
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .dependsOn(common)

val usersApi = projectMatrix
  .in(file("modules/users-api"))
  .jvmPlatform(List(Dependencies.scalaVersion))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "users-api",
    description := "Users' HTTP API"
  )
  .settings(settings)
  .settings(tests)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.kindlingsJsoniter
    ),
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .dependsOn(commonApi, users)

val usersImpl = project
  .in(file("modules/users-impl"))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "users-impl",
    description := "Users' Reads, Writes and Services' implementations"
  )
  .settings(settings)
  .settings(integrationTests)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.jsoniter,
      Dependencies.kindlingsJsoniter
    ),
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .dependsOn(
    common.jvm(Dependencies.scalaVersion) % s"$Compile->$Compile ; $Test->$Test",
    commonInfrastructure % s"$Compile->$Compile ; $Test->$Test",
    discussions.jvm(Dependencies.scalaVersion),
    users.jvm(Dependencies.scalaVersion)
  )

// application

val server = project
  .in(file("modules/server"))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "server",
    description := "Branchtalk backend business logic"
  )
  .settings(settings)
  .settings(integrationTests)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.decline,
      Dependencies.kindlingsJsoniter,
      Dependencies.kindlingsSconfig,
      Dependencies.kindlingsTapirOpenAPI,
      Dependencies.sttpCats % Test,
      Dependencies.http4sBlaze,
      Dependencies.http4sPrometheus,
      Dependencies.tapirHttp4s,
      Dependencies.tapirOpenAPI,
      Dependencies.tapirSwaggerUI,
      Dependencies.tapirSTTP % Test
    ),
    customPredef("scala.util.chaining", "cats.implicits", "neotype"),
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
  .dependsOn(
    commonInfrastructure,
    discussions.jvm(Dependencies.scalaVersion),
    discussionsApi.jvm(Dependencies.scalaVersion),
    discussionsImpl % s"$Test->$Test",
    users.jvm(Dependencies.scalaVersion),
    usersApi.jvm(Dependencies.scalaVersion),
    usersImpl % s"$Test->$Test"
  )

val application = project
  .in(file("modules/app"))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "app",
    description := "Branchtalk backend application"
  )
  .settings(settings)
  .settings(
    Compile / run / mainClass := Some("io.branchtalk.Main"),
    Compile / run / fork := true,
    Compile / runMain / fork := true,
    dockerUpdateLatest := true,
    Docker / packageName := "branchtalk-server",
    Docker / dockerExposedPorts := Seq(8080),
    libraryDependencies ++= Seq(
      Dependencies.logbackJackson,
      Dependencies.logbackJsonClassic
    ),
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .settings(
    inTask(assembly)(
      Seq(
        assemblyJarName := s"${name.value}.jar",
        assemblyMergeStrategy := {
          // required for OpenAPIServer to work
          case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
            MergeStrategy.singleOrError
          // conflicts on random crap
          case "module-info.class" => MergeStrategy.discard
          // otherwise
          case strategy => MergeStrategy.defaultMergeStrategy(strategy)
        },
        mainClass := Some("io.branchtalk.Main")
      )
    )
  )
  .dependsOn(server, discussionsImpl, usersImpl)

// aliases

addCommandAlias("fmt", "scalafmt ; Test/scalafmt")
addCommandAlias("fullTest", "test")
addCommandAlias("fullCoverageTest", "coverage ; test ; coverageReport ; coverageAggregate")
