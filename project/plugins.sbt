// Scala.js and cross-compilation
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.1")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.1")
// publishing
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.11")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
// linting
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.1.7")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.10")
// running
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
