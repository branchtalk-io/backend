import sbt._

import Dependencies._

object Dependencies {

  // scala version
  val scalaOrganization  = "org.scala-lang"
  val scalaVersion       = "2.13.4"
  val crossScalaVersions = Seq("2.13.4")

  // libraries versions
  val avro4sVersion     = "4.0.0" // https://github.com/sksamuel/avro4s/releases
  val catsVersion       = "2.3.1" // https://github.com/typelevel/cats/releases
  val catsEffectVersion = "2.3.1" // https://github.com/typelevel/cats-effect/releases
  val declineVersion    = "1.3.0" // https://github.com/tpolecat/doobie/releases
  val doobieVersion     = "0.10.0" // https://github.com/tpolecat/doobie/releases
  val drosteVersion     = "0.8.0" // https://github.com/higherkindness/droste/releases
  val enumeratumVersion = "1.6.1" // https://github.com/lloydmeta/enumeratum/releases
  val fs2Version        = "2.4.5" // https://github.com/typelevel/fs2/releases
  val log4catsVersion   = "1.1.1" // https://github.com/ChristopherDavenport/log4cats/releases
  val http4sVersion     = "0.21.16" // https://github.com/http4s/http4s/releases
  val jsoniterVersion   = "2.6.2" // https://github.com/plokhotnyuk/jsoniter-scala/releases
  val monixVersion      = "3.3.0" // https://github.com/monix/monix/releases
  val monocleVersion    = "2.1.0" // https://github.com/optics-dev/Monocle/releases
  val pureConfigVersion = "0.14.0" // https://github.com/pureconfig/pureconfig/releases
  val refinedVersion    = "0.9.20" // https://github.com/fthomas/refined/releases
  val specs2Version     = "4.10.6" // https://github.com/etorreborre/specs2/releases
  val tapirVersion      = "0.17.7" // https://github.com/softwaremill/tapir/releases

  // resolvers
  val resolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver.sonatypeRepo("releases"),
    Resolver typesafeRepo "releases"
  )

  // compiler plugins
  val betterMonadicFor =
    "com.olegpy" %% "better-monadic-for" % "0.3.1" // https://github.com/oleg-py/better-monadic-for/releases
  val kindProjector =
    "org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full // https://github.com/typelevel/kind-projector/releases
  // functional libraries
  val catnip     = "io.scalaland" %% "catnip" % "1.1.2" // https://github.com/scalalandio/catnip/releases
  val cats       = "org.typelevel" %% "cats-core" % catsVersion
  val catsFree   = "org.typelevel" %% "cats-free" % catsVersion
  val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  val alleycats  = "org.typelevel" %% "alleycats-core" % catsVersion
  val kittens    = "org.typelevel" %% "kittens" % "2.2.1" // https://github.com/typelevel/kittens/releases
  val catsLaws   = "org.typelevel" %% "cats-laws" % catsVersion
  val chimney    = "io.scalaland" %% "chimney" % "0.6.1" // https://github.com/scalalandio/chimney/releases
  val droste     = "io.higherkindness" %% "droste-core" % drosteVersion
  val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion
  val fastuuid   = "com.eatthepath" % "fast-uuid" % "0.1" // https://github.com/jchambers/fast-uuid/releases
  val uuidGenerator =
    "com.fasterxml.uuid" % "java-uuid-generator" % "4.0.1" // https://github.com/cowtowncoder/java-uuid-generator/releases
  val fs2               = "co.fs2" %% "fs2-core" % fs2Version
  val fs2IO             = "co.fs2" %% "fs2-io" % fs2Version
  val magnolia          = "com.propensive" %% "magnolia" % "0.17.0" // https://github.com/propensive/magnolia/releases
  val monocle           = "com.github.julien-truffaut" %% "monocle-core" % monocleVersion
  val monocleMacro      = "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion
  val newtype           = "io.estatico" %% "newtype" % "0.4.4" // https://github.com/estatico/scala-newtype/releases
  val refined           = "eu.timepit" %% "refined" % refinedVersion
  val refinedCats       = "eu.timepit" %% "refined-cats" % refinedVersion
  val refinedDecline    = "com.monovore" %% "decline-refined" % declineVersion
  val refinedPureConfig = "eu.timepit" %% "refined-pureconfig" % refinedVersion
  // async
  val monixExecution = "io.monix" %% "monix-execution" % monixVersion
  val monixEval      = "io.monix" %% "monix-eval" % monixVersion
  val monixBio       = "io.monix" %% "monix-bio" % "1.0.0" // https://github.com/monix/monix-bio/releases
  // infrastructure
  val avro4s         = "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion
  val avro4sCats     = "com.sksamuel.avro4s" %% "avro4s-cats" % avro4sVersion
  val avro4sRefined  = "com.sksamuel.avro4s" %% "avro4s-refined" % avro4sVersion
  val doobie         = "org.tpolecat" %% "doobie-core" % doobieVersion
  val doobieHikari   = "org.tpolecat" %% "doobie-hikari" % doobieVersion
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion
  val doobieRefined  = "org.tpolecat" %% "doobie-refined" % doobieVersion
  val doobieSpecs2   = "org.tpolecat" %% "doobie-specs2" % doobieVersion
  val flyway         = "org.flywaydb" % "flyway-core" % "7.2.0" // https://github.com/flyway/flyway/releases
  val fs2Kafka       = "com.github.fd4s" %% "fs2-kafka" % "1.1.0" // https://github.com/fd4s/fs2-kafka/releases
  val macwire        = "com.softwaremill.macwire" %% "macros" % "2.3.7" % "provided" // GH releases are out of date
  val redis4cats =
    "dev.profunktor" %% "redis4cats-effects" % "0.10.3" // https://github.com/profunktor/redis4cats/releases
  // API
  val sttpCats =
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.0.0" // https://github.com/softwaremill/sttp/releases
  // same as the one used by tapir
  val http4sPrometheus = "org.http4s" %% "http4s-prometheus-metrics" % http4sVersion
  val tapir            = "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion
  val tapirHttp4s      = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion
  val tapirJsoniter    = "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % tapirVersion
  val tapirRefined     = "com.softwaremill.sttp.tapir" %% "tapir-refined" % tapirVersion
  val tapirOpenAPI     = "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion
  val tapirSwaggerUI   = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % tapirVersion
  val tapirSTTP        = "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion
  val jsoniter         = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoniterVersion
  val jsoniterMacro =
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoniterVersion % "compile-internal"
  // config
  val decline              = "com.monovore" %% "decline" % declineVersion
  val scalaConfig          = "com.typesafe" % "config" % "1.4.1" // https://github.com/lightbend/config/releases
  val pureConfig           = "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
  val pureConfigCats       = "com.github.pureconfig" %% "pureconfig-cats" % pureConfigVersion
  val pureConfigEnumeratum = "com.github.pureconfig" %% "pureconfig-enumeratum" % pureConfigVersion
  // security
  val bcrypt = "at.favre.lib" % "bcrypt" % "0.9.0"
  // logging
  val log4cats           = "io.chrisdavenport" %% "log4cats-core" % log4catsVersion
  val log4catsSlf4j      = "io.chrisdavenport" %% "log4cats-slf4j" % log4catsVersion
  val scalaLogging       = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2" // GH releases are out of date
  val logback            = "ch.qos.logback" % "logback-classic" % "1.2.3" // https://github.com/qos-ch/logback/releases
  val logbackJackson     = "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5" // see MVN
  val logbackJsonClassic = "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5" // see MVN
  val sourcecode         = "com.lihaoyi" %% "sourcecode" % "0.2.1" // https://github.com/lihaoyi/sourcecode/releases
  val prometheus         = "io.prometheus" % "simpleclient" % "0.9.0" // https://github.com/prometheus/client_java/releases
  // testing
  val jfairy          = "com.devskiller" % "jfairy" % "0.6.4" // https://github.com/Devskiller/jfairy/releases
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
}
