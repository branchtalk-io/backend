import sbt._
import Settings._

lazy val root = project.root
  .setName("subfibers")
  .setDescription("subfibers build")
  .configureRoot
  .aggregate(common)

val common = project.from("domains")
  .setName("domains")
  .setDescription("Domains definitions")
  .setInitialImport()
  .configureModule
  .configureTests()
  .settings(Compile / resourceGenerators += task[Seq[File]] {
    val file = (Compile / resourceManaged).value / "subfibers-version.conf"
    IO.write(file, s"version=${version.value}")
    Seq(file)
  })

addCommandAlias("fullTest", ";test;fun:test;it:test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;fun:test;it:test;coverageReport;coverageAggregate;scalastyle")
