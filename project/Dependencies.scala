import sbt.*

object Dependencies {

  // scala version
  val scalaVersion = "3.9.0"

  // libraries versions
  val catsVersion       = "2.13.0" // https://github.com/typelevel/cats/releases
  val catsEffectVersion = "3.7.1" // https://github.com/typelevel/cats-effect/releases
  val declineVersion    = "2.6.2" // https://github.com/bkirwi/decline/releases
  val doobieVersion     = "1.0.0-RC13" // https://github.com/tpolecat/doobie/releases
  val drosteVersion     = "0.10.0" // https://github.com/higherkindness/droste/releases
  val enumeratumVersion = "1.9.8" // https://github.com/lloydmeta/enumeratum/releases
  val fs2Version        = "3.13.0" // https://github.com/typelevel/fs2/releases
  val hearthVersion     = "0.4.2" // https://github.com/kubuszok/hearth/releases
  val kindlingsVersion  = "0.3.2" // https://github.com/kubuszok/kindlings/releases
  val log4catsVersion   = "2.8.0" // https://github.com/ChristopherDavenport/log4cats/releases
  val http4sVersion     = "0.24.7" // https://github.com/http4s/http4s/releases
  val jsoniterVersion   = "2.40.1" // https://github.com/plokhotnyuk/jsoniter-scala/releases
  val neotypeVersion    = "0.7.1" // https://github.com/kitlangton/neotype/releases
  val specs2Version     = "5.9.1" // https://github.com/etorreborre/specs2/releases
  val tapirVersion      = "1.13.31" // https://github.com/softwaremill/tapir/releases

  // Kindlings / Hearth (github.com/kubuszok) - type class derivation built on Hearth macros
  val hearth = "com.kubuszok" %% "hearth" % hearthVersion
  // Compiler plugin enabling Hearth's Expr.quote/Expr.splice DSL - needed only to author macro extensions
  // (the neotype-kindlings IsValueType provider), not to use derivation.
  val hearthCrossQuotes = "com.kubuszok" %% "hearth-cross-quotes" % hearthVersion
  val kindlingsCats     = "com.kubuszok" %% "kindlings-cats-derivation" % kindlingsVersion
  // Hearth StandardExtension (service-loader): teaches ALL Kindlings derivations to treat cats collections
  // (NonEmptyList, ...) as collections rather than structurally as products.
  val kindlingsCatsInterop  = "com.kubuszok" %% "kindlings-cats-integration" % kindlingsVersion
  val kindlingsShowPretty   = "com.kubuszok" %% "kindlings-fast-show-pretty" % kindlingsVersion
  val kindlingsJsoniter     = "com.kubuszok" %% "kindlings-jsoniter-derivation" % kindlingsVersion
  val kindlingsAvro         = "com.kubuszok" %% "kindlings-avro-derivation" % kindlingsVersion
  val kindlingsSconfig      = "com.kubuszok" %% "kindlings-sconfig-derivation" % kindlingsVersion
  val kindlingsTapirSchema  = "com.kubuszok" %% "kindlings-tapir-schema-derivation" % kindlingsVersion
  val kindlingsTapirOpenAPI = "com.kubuszok" %% "kindlings-tapir-openapi-jsoniter" % kindlingsVersion

  // functional libraries
  val cats             = "org.typelevel" %% "cats-core" % catsVersion
  val catsFree         = "org.typelevel" %% "cats-free" % catsVersion
  val catsEffect       = "org.typelevel" %% "cats-effect" % catsEffectVersion
  val alleycats        = "org.typelevel" %% "alleycats-core" % catsVersion
  val catsLaws         = "org.typelevel" %% "cats-laws" % catsVersion
  val chimney          = "io.scalaland" %% "chimney" % "2.0.0-RC1" // https://github.com/scalalandio/chimney/releases
  val droste           = "io.higherkindness" %% "droste-core" % drosteVersion
  val enumeratum       = "com.beachape" %% "enumeratum" % enumeratumVersion
  val enumeratumDoobie = "com.beachape" %% "enumeratum-doobie" % "1.9.8"
  val fastuuid         = "com.eatthepath" % "fast-uuid" % "0.2.0" // https://github.com/jchambers/fast-uuid/releases
  val uuidGenerator =
    "com.fasterxml.uuid" % "java-uuid-generator" % "5.2.0" // https://github.com/cowtowncoder/java-uuid-generator/releases
  val fs2             = "co.fs2" %% "fs2-core" % fs2Version
  val fs2IO           = "co.fs2" %% "fs2-io" % fs2Version
  val neotype         = "io.github.kitlangton" %% "neotype" % neotypeVersion
  // neotype-chimney intentionally dropped: Chimney 2.0 is Hearth-based, so the neotype-kindlings Hearth integration
  // (IsValueTypeProviderForNeotype via StandardMacroExtension) already covers neotype <-> Chimney derivation.
  val neotypeDoobie   = "io.github.kitlangton" %% "neotype-doobie" % neotypeVersion
  val neotypeJsoniter = "io.github.kitlangton" %% "neotype-jsoniter" % neotypeVersion
  val neotypeTapir    = "io.github.kitlangton" %% "neotype-tapir" % neotypeVersion
  val quicklens =
    "com.softwaremill.quicklens" %% "quicklens" % "1.9.15" // https://github.com/softwaremill/quicklens/releases
  // infrastructure
  val doobie         = "org.typelevel" %% "doobie-core" % doobieVersion
  val doobieHikari   = "org.typelevel" %% "doobie-hikari" % doobieVersion
  val doobiePostgres = "org.typelevel" %% "doobie-postgres" % doobieVersion
  val doobieSpecs2   = "org.typelevel" %% "doobie-specs2" % doobieVersion
  val flyway         = "org.flywaydb" % "flyway-core" % "13.4.0" // https://github.com/flyway/flyway/releases
  val flywayPostgres =
    "org.flywaydb" % "flyway-database-postgresql" % "13.4.0" // https://github.com/flyway/flyway/releases
  val fs2Kafka = "com.github.fd4s" %% "fs2-kafka" % "3.9.1" // https://github.com/fd4s/fs2-kafka/releasesreleases
  val redis4cats =
    "dev.profunktor" %% "redis4cats-effects" % "2.0.6" // https://github.com/profunktor/redis4cats/releases
  // API
  val sttpCats =
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.11.0" // https://github.com/softwaremill/sttp/releases
  // same as the one used by tapir
  val http4sBlaze      = "org.http4s" %% "http4s-blaze-server" % "0.23.18" // https://github.com/http4s/blaze/releases
  val http4sPrometheus = "org.http4s" %% "http4s-prometheus-metrics" % http4sVersion
  val tapir            = "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion
  val tapirHttp4s      = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion
  val tapirJsoniter    = "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % tapirVersion
  val tapirOpenAPI     = "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion
  val tapirSwaggerUI   = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.19.0-M4" // tapirVersion
  val tapirSTTP        = "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion
  val jsoniter         = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoniterVersion
  // config
  val decline = "com.monovore" %% "decline" % declineVersion
  val sconfig = "org.ekrich" %% "sconfig" % "2.0.0" // https://github.com/ekrich/sconfig/releases
  // security
  val bcrypt = "at.favre.lib" % "bcrypt" % "0.10.2"
  // logging
  val log4cats           = "org.typelevel" %% "log4cats-core" % log4catsVersion
  val log4catsSlf4j      = "org.typelevel" %% "log4cats-slf4j" % log4catsVersion
  val scalaLogging       = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6" // GH releases are out of date
  val logback            = "ch.qos.logback" % "logback-classic" % "1.6.3" // https://github.com/qos-ch/logback/releases
  val logbackJackson     = "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5" // see MVN
  val logbackJsonClassic = "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5" // see MVN
  val prometheus = "io.prometheus" % "simpleclient" % "0.16.0" // https://github.com/prometheus/client_java/releases
  // testing
  val jfairy = "com.devskiller" % "jfairy" % "0.6.5" // https://github.com/Devskiller/jfairy/releases
  val guice  = "com.google.inject" % "guice" % "7.0.0" // required by jfairy on JDK 15+
  val guiceAssisted =
    "com.google.inject.extensions" % "guice-assistedinject" % "7.0.0" // required by jfairy on JDK 15+
  val spec2Core       = "org.specs2" %% "specs2-core" % specs2Version
  val spec2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
}
