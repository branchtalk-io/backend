import sbt.*
import commandmatrix.extra.*
import com.typesafe.sbt.SbtNativePackager.Docker
import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings
import sbt.TestFrameworks.Specs2

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

val only1VersionInIDE = Seq(
  MatrixAction.ForPlatform(VirtualAxis.jvm).Configure(_.settings(ideSkipProject := false, bspEnabled := true)),
  MatrixAction.ForPlatform(VirtualAxis.js).Configure(_.settings(ideSkipProject := true, bspEnabled := false))
)

val settings = Seq(
  organization := "io.branchtalk",
  scalaVersion := Dependencies.scalaVersion,
  scalacOptions ++= Seq(
    // standard settings
    // format: off
    "-encoding", "UTF-8",
    "-rewrite",
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
    Dependencies.kittens,
    Dependencies.chimney,
    Dependencies.enumeratum,
    Dependencies.fastuuid,
    Dependencies.uuidGenerator,
    Dependencies.log4cats,
    Dependencies.log4catsSlf4j,
    Dependencies.magnolia,
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
    testFrameworks := Seq(Specs2),
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
  scalacOptions += s"-Yimports:${(Seq("java.lang", "scala", "scala.Predef") ++ imports).mkString(",")}"

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

lazy val scalaJsArtifacts = project
  .in(file("scala-js"))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "scala-js-artifacts",
    description := "aggregates all Scala.js modules to publish"
  )
  .settings(settings *)
  .aggregate(
    common.js(Dependencies.scalaVersion),
    commonApi.js(Dependencies.scalaVersion),
    discussions.js(Dependencies.scalaVersion),
    discussionsApi.js(Dependencies.scalaVersion),
    users.js(Dependencies.scalaVersion),
    usersApi.js(Dependencies.scalaVersion)
  )

// commons

val common = projectMatrix
  .in(file("modules/common"))
  .someVariations(List(Dependencies.scalaVersion), List(VirtualAxis.jvm, VirtualAxis.js))(only1VersionInIDE *)
  .defaultAxes(VirtualAxis.scalaABIVersion(Dependencies.scalaVersion), VirtualAxis.jvm)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "common",
    description := "Common utilities"
  )
  .settings(settings *)
  .settings(tests *)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.avro4s,
      Dependencies.avro4sCats,
      Dependencies.sourcecode,
      Dependencies.jfairy % Test,
      Dependencies.guice % Test, // required by jfairy on JDK 15+
      Dependencies.guiceAssisted % Test // required by jfairy on JDK 15+
    ),
    customPredef("scala.util.chaining", "cats.implicits")
  )

val commonApi = projectMatrix
  .someVariations(List(Dependencies.scalaVersion), List(VirtualAxis.jvm, VirtualAxis.js))(only1VersionInIDE *)
  .defaultAxes(VirtualAxis.scalaABIVersion(Dependencies.scalaVersion), VirtualAxis.jvm)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .in(file("modules/common-api"))
  .settings(
    name := "common-api",
    description := "Infrastructure-dependent implementations"
  )
  .settings(settings *)
  .settings(tests *)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.jsoniter,
      Dependencies.jsoniterMacro,
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
      Dependencies.fs2,
      Dependencies.fs2IO,
      Dependencies.fs2Kafka,
      Dependencies.neotypeDoobie,
      Dependencies.prometheus,
      Dependencies.pureConfig,
      Dependencies.pureConfigCats,
      Dependencies.pureConfigEnumeratum,
      Dependencies.redis4cats
    ),
    customPredef("scala.util.chaining", "cats.implicits", "neotype")
  )
  .dependsOn(common.jvm(Dependencies.scalaVersion))

// discussions

val discussions = projectMatrix
  .someVariations(List(Dependencies.scalaVersion), List(VirtualAxis.jvm, VirtualAxis.js))(only1VersionInIDE *)
  .defaultAxes(VirtualAxis.scalaABIVersion(Dependencies.scalaVersion), VirtualAxis.jvm)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .in(file("modules/discussions"))
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
  .someVariations(List(Dependencies.scalaVersion), List(VirtualAxis.jvm, VirtualAxis.js))(only1VersionInIDE *)
  .defaultAxes(VirtualAxis.scalaABIVersion(Dependencies.scalaVersion), VirtualAxis.jvm)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .in(file("modules/discussions-api"))
  .settings(
    name := "discussions-api",
    description := "Discussions' HTTP API"
  )
  .settings(settings)
  .settings(tests)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.jsoniterMacro
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
  .someVariations(List(Dependencies.scalaVersion), List(VirtualAxis.jvm, VirtualAxis.js))(only1VersionInIDE *)
  .defaultAxes(VirtualAxis.scalaABIVersion(Dependencies.scalaVersion), VirtualAxis.jvm)
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
  .someVariations(List(Dependencies.scalaVersion), List(VirtualAxis.jvm, VirtualAxis.js))(only1VersionInIDE *)
  .defaultAxes(VirtualAxis.scalaABIVersion(Dependencies.scalaVersion), VirtualAxis.jvm)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "users-api",
    description := "Users' HTTP API"
  )
  .settings(settings)
  .settings(tests)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.jsoniterMacro
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
      Dependencies.jsoniterMacro
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
      Dependencies.jsoniterMacro,
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
