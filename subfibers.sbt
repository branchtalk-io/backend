import sbt._
import Settings._

lazy val root =
  project.root
    .setName("subfibers")
    .setDescription("subfibers build")
    .configureRoot
    .aggregate(derivation, common, infrastructure, domains, persistence)

val derivation = project
  .from("derivation")
  .setName("derivation")
  .setDescription("Derivation helpers")
  .configureModule
  .settings(libraryDependencies += "io.scalaland" %% "catnip" % "1.0.0")

val common = project
  .from("common")
  .setName("common")
  .setDescription("Common utilities")
  .configureModule
  .settings(
    Compile / resourceGenerators += task[Seq[File]] {
      val file = (Compile / resourceManaged).value / "subfibers-version.conf"
      IO.write(file, s"version=${version.value}")
      Seq(file)
    }
  )
  .dependsOn(derivation)

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
      Dependencies.fs2Kafka
    )
  )
  .dependsOn(common)

val domains = project
  .from("domains")
  .setName("domains")
  .setDescription("Domains definitions")
  .configureModule
  .configureTests()
  .dependsOn(common)

val persistence = project
  .from("persistence")
  .setName("persistence")
  .setDescription("Writes projections and Reads queries")
  .configureModule
  .configureTests()
  .dependsOn(domains, infrastructure)

addCommandAlias("fullTest", ";test")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate")
