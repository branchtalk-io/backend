// Scala.js and cross-compilation
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.1.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.8.0")
// publishing
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.11")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.0")
// linting
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("com.beautiful-scala" % "sbt-scalastyle" % "1.5.1")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.0.6")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")
// running
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
