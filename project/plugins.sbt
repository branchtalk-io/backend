// git
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
// linters
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.1.7")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.1.0")
// cross-compile
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.10.0")
addSbtPlugin("com.indoorvivants" % "sbt-commandmatrix" % "0.0.5")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.16.0")
// publishing
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.11")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
// disabling projects in IDE
addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "1.1.0")
// running
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
