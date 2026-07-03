// git
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
// linters
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.6.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.3.1")
// cross-compile (JVM-only after Scala 3 migration; projectmatrix kept for module matrix)
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")
// publishing
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.11")
// disabling projects in IDE
addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "1.1.0")
// running
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
