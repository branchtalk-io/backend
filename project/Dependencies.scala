import sbt._
import sbt.Keys.{ libraryDependencies, scalaBinaryVersion }
import Dependencies._
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSVersion

import Dependencies._

object Dependencies {

  // scala version
  val scalaOrganization  = "org.scala-lang"
  val scalaVersion       = "2.13.9"
  val crossScalaVersions = Seq("2.13.9")

  // libraries versions
  val avro4sVersion     = "4.1.0" // https://github.com/sksamuel/avro4s/releases
  val catsVersion       = "2.8.0" // https://github.com/typelevel/cats/releases
  val catsEffectVersion = "3.3.14" // https://github.com/typelevel/cats-effect/releases
  val declineVersion    = "2.3.0" // https://github.com/tpolecat/doobie/releases
  val doobieVersion     = "1.0.0-RC2" // https://github.com/tpolecat/doobie/releases
  val drosteVersion     = "0.9.0" // https://github.com/higherkindness/droste/releases
  val enumeratumVersion = "1.7.0" // https://github.com/lloydmeta/enumeratum/releases
  val fs2Version        = "3.2.14" // https://github.com/typelevel/fs2/releases
  val log4catsVersion   = "2.5.0" // https://github.com/ChristopherDavenport/log4cats/releases
  val http4sVersion     = "0.24.1" // https://github.com/http4s/http4s/releases
  val jsoniterVersion   = "2.17.4" // https://github.com/plokhotnyuk/jsoniter-scala/releases
  val monocleVersion    = "3.1.0" // https://github.com/optics-dev/Monocle/releases
  val pureConfigVersion = "0.17.1" // https://github.com/pureconfig/pureconfig/releases
  val refinedVersion    = "0.10.1" // https://github.com/fthomas/refined/releases
  val specs2Version     = "4.16.1" // https://github.com/etorreborre/specs2/releases
  val tapirVersion      = "1.1.0" // https://github.com/softwaremill/tapir/releases

  // resolvers
  val resolvers = Seq(
    Resolver.sonatypeOssRepos("public"),
    Resolver.sonatypeOssRepos("releases"),
    Seq(Resolver.typesafeRepo("releases"))
  ).flatten

  // compiler plugins
  val betterMonadicFor =
    "com.olegpy" %% "better-monadic-for" % "0.3.1" // https://github.com/oleg-py/better-monadic-for/releases
  val kindProjector =
    "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full // https://github.com/typelevel/kind-projector/releases
  // functional libraries
  val catnip =
    "io.scalaland" %% "catnip" % "1.1.2" exclude ("org.typelevel", "kittens_2.13") // https://github.com/scalalandio/catnip/releases
  val cats       = "org.typelevel" %% "cats-core" % catsVersion
  val catsFree   = "org.typelevel" %% "cats-free" % catsVersion
  val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  val alleycats  = "org.typelevel" %% "alleycats-core" % catsVersion
  val kittens    = "org.typelevel" %% "kittens" % "3.0.0" // https://github.com/typelevel/kittens/releases
  val catsLaws   = "org.typelevel" %% "cats-laws" % catsVersion
  val chimney    = "io.scalaland" %% "chimney" % "0.6.2" // https://github.com/scalalandio/chimney/releases
  val droste     = "io.higherkindness" %% "droste-core" % drosteVersion
  val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion
  val fastuuid   = "com.eatthepath" % "fast-uuid" % "0.2.0" // https://github.com/jchambers/fast-uuid/releases
  val uuidGenerator =
    "com.fasterxml.uuid" % "java-uuid-generator" % "4.0.1" // https://github.com/cowtowncoder/java-uuid-generator/releases
  val fs2   = "co.fs2" %% "fs2-core" % fs2Version
  val fs2IO = "co.fs2" %% "fs2-io" % fs2Version
  val magnolia =
    "com.softwaremill.magnolia1_2" %% "magnolia" % "1.1.2" // https://github.com/softwaremill/magnolia/releases
  val monocle           = "dev.optics" %% "monocle-core" % monocleVersion
  val monocleMacro      = "dev.optics" %% "monocle-macro" % monocleVersion
  val newtype           = "io.estatico" %% "newtype" % "0.4.4" // https://github.com/estatico/scala-newtype/releases
  val refined           = "eu.timepit" %% "refined" % refinedVersion
  val refinedCats       = "eu.timepit" %% "refined-cats" % refinedVersion
  val refinedDecline    = "com.monovore" %% "decline-refined" % declineVersion
  val refinedPureConfig = "eu.timepit" %% "refined-pureconfig" % refinedVersion
  // infrastructure
  val avro4s         = "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion
  val avro4sCats     = "com.sksamuel.avro4s" %% "avro4s-cats" % avro4sVersion
  val avro4sRefined  = "com.sksamuel.avro4s" %% "avro4s-refined" % avro4sVersion
  val doobie         = "org.tpolecat" %% "doobie-core" % doobieVersion
  val doobieHikari   = "org.tpolecat" %% "doobie-hikari" % doobieVersion
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion
  val doobieRefined  = "org.tpolecat" %% "doobie-refined" % doobieVersion
  val doobieSpecs2   = "org.tpolecat" %% "doobie-specs2" % doobieVersion
  val flyway         = "org.flywaydb" % "flyway-core" % "9.3.0" // https://github.com/flyway/flyway/releases
  val fs2Kafka       = "com.github.fd4s" %% "fs2-kafka" % "2.5.0" // https://github.com/fd4s/fs2-kafka/releases
  val macwire =
    "com.softwaremill.macwire" %% "macros" % "2.5.8" % "provided" // https://github.com/softwaremill/macwire/releases
  val redis4cats =
    "dev.profunktor" %% "redis4cats-effects" % "1.2.0" // https://github.com/profunktor/redis4cats/releases
  // API
  val sttpCats =
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.8.0" // https://github.com/softwaremill/sttp/releases
  // same as the one used by tapir
  val http4sBlaze      = "org.http4s" %% "http4s-blaze-server" % "0.23.12" // https://github.com/http4s/blaze/releases
  val http4sPrometheus = "org.http4s" %% "http4s-prometheus-metrics" % http4sVersion
  val tapir            = "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion
  val tapirHttp4s      = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion
  val tapirJsoniter    = "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % tapirVersion
  val tapirRefined     = "com.softwaremill.sttp.tapir" %% "tapir-refined" % tapirVersion
  val tapirOpenAPI     = "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion
  val tapirSwaggerUI   = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.19.0-M4" // tapirVersion
  val tapirSTTP        = "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion
  val jsoniter         = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoniterVersion
  val jsoniterMacro =
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoniterVersion % "compile-internal"
  // config
  val decline              = "com.monovore" %% "decline" % declineVersion
  val scalaConfig          = "com.typesafe" % "config" % "1.4.2" // https://github.com/lightbend/config/releases
  val pureConfig           = "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
  val pureConfigCats       = "com.github.pureconfig" %% "pureconfig-cats" % pureConfigVersion
  val pureConfigEnumeratum = "com.github.pureconfig" %% "pureconfig-enumeratum" % pureConfigVersion
  // security
  val bcrypt = "at.favre.lib" % "bcrypt" % "0.9.0"
  // logging
  val log4cats           = "org.typelevel" %% "log4cats-core" % log4catsVersion
  val log4catsSlf4j      = "org.typelevel" %% "log4cats-slf4j" % log4catsVersion
  val scalaLogging       = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5" // GH releases are out of date
  val logback            = "ch.qos.logback" % "logback-classic" % "1.4.1" // https://github.com/qos-ch/logback/releases
  val logbackJackson     = "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5" // see MVN
  val logbackJsonClassic = "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5" // see MVN
  val sourcecode         = "com.lihaoyi" %% "sourcecode" % "0.3.0" // https://github.com/lihaoyi/sourcecode/releases
  val prometheus         = "io.prometheus" % "simpleclient" % "0.16.0" // https://github.com/prometheus/client_java/releases
  // testing
  val jfairy = "com.devskiller" % "jfairy" % "0.6.4" // https://github.com/Devskiller/jfairy/releases
  val guice  = "com.google.inject" % "guice" % "5.0.1" // required by jfairy on JDK 15+
  val guiceAssisted =
    "com.google.inject.extensions" % "guice-assistedinject" % "5.0.1" // required by jfairy on JDK 15+
  val spec2Core       = "org.specs2" %% "specs2-core" % specs2Version
  val spec2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
}

trait Dependencies {

  val scalaOrganizationUsed  = scalaOrganization
  val scalaVersionUsed       = scalaVersion
  val crossScalaVersionsUsed = crossScalaVersions

  // resolvers
  val commonResolvers = resolvers

  val mainDeps = Seq(
    cats,
    catsFree,
    catsEffect,
    alleycats,
    kittens,
    chimney,
    enumeratum,
    fastuuid,
    uuidGenerator,
    log4cats,
    log4catsSlf4j,
    magnolia,
    monocle,
    monocleMacro,
    newtype,
    refined,
    refinedCats,
    scalaLogging,
    logback
  )

  val testDeps = Seq(catsLaws, spec2Core, spec2Scalacheck)

  implicit final class ProjectRoot(project: Project) {

    def root: Project = project in file(".")
  }

  implicit final class ProjectFrom(project: Project) {

    private val commonDir = "modules"

    def from(dir: String): Project = project in file(s"$commonDir/$dir")
  }

  implicit final class DependsOnProject(project: Project) {

    private val testConfigurations = Set("test", "fun", "it")
    private def findCompileAndTestConfigs(p: Project) =
      (p.configurations.map(_.name).toSet intersect testConfigurations) + "compile"

    private val thisProjectsConfigs = findCompileAndTestConfigs(project)
    private def generateDepsForProject(p: Project) =
      p % (thisProjectsConfigs intersect findCompileAndTestConfigs(p) map (c => s"$c->$c") mkString ";")

    def compileAndTestDependsOn(projects: Project*): Project =
      project dependsOn (projects.map(generateDepsForProject): _*)
  }

  implicit final class CrossProjectFrom(project: CrossProject) {

    private val commonDir = "modules"

    def from(dir: String): CrossProject = project in file(s"$commonDir/$dir")
  }

  implicit final class DependsOnCrossProject(project: CrossProject) {

    private val testConfigurations = Set("test", "fun", "it")
    private def findCompileAndTestConfigs(p: CrossProject) =
      (p.projects(JVMPlatform).configurations.map(_.name).toSet intersect testConfigurations) + "compile"

    private val thisProjectsConfigs = findCompileAndTestConfigs(project)
    private def generateDepsForProject(p: CrossProject) =
      p % (thisProjectsConfigs intersect findCompileAndTestConfigs(p) map (c => s"$c->$c") mkString ";")

    def compileAndTestDependsOn(projects: CrossProject*): CrossProject =
      project dependsOn (projects.map(generateDepsForProject): _*)
  }
}
